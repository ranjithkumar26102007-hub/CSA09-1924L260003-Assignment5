import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Task B3: Transaction Failure and Rollback Investigation.
 * Tests inserting 1,000 records where Record 500 contains an invalid amount_paise (-1)
 * violating the MySQL CHECK constraint. Compares:
 * 1. Auto-commit mode (batches of 100) -> Leaves 499 orphan rows in table.
 * 2. Single transaction mode (batches of 100) -> Clean rollback leaving 0 rows.
 *
 * Course: CSA09 – Programming in Java
 * Student: R. Ranjith Kumar | Register Number: 1924L260003
 */
public class TransactionRollbackTest {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  CSA09 Assignment 5 - Task B3: Transaction Failure & Rollback Test");
        System.out.println("  Student: R. Ranjith Kumar | Reg No: 1924L260003");
        System.out.println("================================================================================");

        try (Connection conn = DbConfig.getConnection()) {
            System.out.println("Connected to MySQL: " + DbConfig.getDbUrl());

            // Generate 1,000 passes with Record 500 invalid (-1 paise)
            List<PassLoader.Pass> passes = new ArrayList<>(1000);
            for (int i = 1; i <= 1000; i++) {
                long amount = (i == 500) ? -1 : (450000 + i);
                passes.add(new PassLoader.Pass(
                        String.format("TR%05d", i),
                        String.format("1924L%05d", 260000 + i),
                        "Student_" + i,
                        "R12",
                        "Stop_01",
                        LocalDate.of(2027, 3, 31),
                        amount
                ));
            }

            System.out.println("\n--- Experiment: Bad Record at Row 500 of 1,000 (Violates chk_amount) ---");
            System.out.printf("%-30s | %-24s | %-10s | %-26s%n",
                    "Mode", "What Happened", "Rows Left", "Verdict");
            System.out.println("--------------------------------------------------------------------------------------------------");

            // TEST 1: Auto-commit, batches of 100
            truncateTable(conn);
            String autoCommitResult;
            int autoCommitRows = 0;
            try {
                conn.setAutoCommit(true);
                runBatchedInsert(conn, passes, 100);
                autoCommitResult = "Unexpected success";
            } catch (SQLException e) {
                autoCommitResult = e.getClass().getSimpleName();
            }
            autoCommitRows = getTableRowCount(conn);
            String autoVerdict = (autoCommitRows > 0) ? "partial data left in the table" : "clean";
            System.out.printf("%-30s | %-24s | %10d | %-26s%n",
                    "auto-commit, batches of 100", autoCommitResult, autoCommitRows, autoVerdict);

            // TEST 2: Single transaction, batches of 100
            truncateTable(conn);
            String txResult;
            int txRows = 0;
            try {
                conn.setAutoCommit(false);
                runBatchedInsert(conn, passes, 100);
                conn.commit();
                txResult = "Unexpected success";
            } catch (SQLException e) {
                conn.rollback();
                txResult = e.getClass().getSimpleName();
            } finally {
                conn.setAutoCommit(true);
            }
            txRows = getTableRowCount(conn);
            String txVerdict = (txRows == 0) ? "no rows remain; correct" : "FAILED";
            System.out.printf("%-30s | %-24s | %10d | %-26s%n",
                    "one transaction, batches of 100", txResult, txRows, txVerdict);

            System.out.println("--------------------------------------------------------------------------------------------------");
            System.out.println("OBSERVATION: Under auto-commit, " + autoCommitRows + " rows remained permanently committed.");
            System.out.println("             Under single transaction, rollback restored database to exactly 0 rows.");
            System.out.println("================================================================================");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runBatchedInsert(Connection conn, List<PassLoader.Pass> passes, int batchSize) throws SQLException {
        String sql = "INSERT INTO bus_pass (pass_id, register_no, name, route_no, boarding_stop, valid_until, amount_paise) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int count = 0;
            for (PassLoader.Pass p : passes) {
                ps.setString(1, p.passId());
                ps.setString(2, p.registerNo());
                ps.setString(3, p.name());
                ps.setString(4, p.routeNo());
                ps.setString(5, p.boardingStop());
                ps.setDate(6, Date.valueOf(p.validUntil()));
                ps.setLong(7, p.amountPaise());
                ps.addBatch();
                count++;
                if (count % batchSize == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private static void truncateTable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("TRUNCATE TABLE bus_pass");
        }
    }

    private static int getTableRowCount(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM bus_pass")) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }
}
