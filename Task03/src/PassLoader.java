import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Task B3: PassLoader implementing the three core JDBC insertion paradigms:
 * - TODO 1: Raw Statement with SQL string concatenation
 * - TODO 2: PreparedStatement executed one row at a time
 * - TODO 3: PreparedStatement batch execution inside one transaction
 *
 * Course: CSA09 – Programming in Java
 * Student: R. Ranjith Kumar | Register Number: 1924L260003
 */
public class PassLoader {

    public record Pass(String passId, String registerNo, String name, String routeNo,
                       String boardingStop, LocalDate validUntil, long amountPaise) {}

    /**
     * Inserts passes using raw Statement (TODO 1).
     */
    public static long loadWithStatement(Connection conn, List<Pass> passes) throws SQLException {
        long start = System.nanoTime();
        try (Statement st = conn.createStatement()) {
            for (Pass p : passes) {
                /* TODO 1 Statement, values joined into SQL text */
                String sql = "INSERT INTO bus_pass (pass_id, register_no, name, route_no, boarding_stop, valid_until, amount_paise) VALUES (" +
                        "'" + p.passId() + "', " +
                        "'" + p.registerNo() + "', " +
                        "'" + p.name().replace("'", "''") + "', " +
                        "'" + p.routeNo() + "', " +
                        "'" + p.boardingStop().replace("'", "''") + "', " +
                        "'" + Date.valueOf(p.validUntil()) + "', " +
                        p.amountPaise() + ")";
                st.executeUpdate(sql);
            }
        }
        return (System.nanoTime() - start);
    }

    /**
     * Inserts passes using PreparedStatement row-by-row (TODO 2).
     */
    public static long loadWithPreparedStatement(Connection conn, List<Pass> passes) throws SQLException {
        long start = System.nanoTime();
        String sql = "INSERT INTO bus_pass (pass_id, register_no, name, route_no, boarding_stop, valid_until, amount_paise) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Pass p : passes) {
                /* TODO 2 PreparedStatement, one row at a time */
                bindPass(ps, p);
                ps.executeUpdate();
            }
        }
        return (System.nanoTime() - start);
    }

    /**
     * Inserts passes using PreparedStatement batch inside one transaction (TODO 3).
     */
    public static long loadWithBatch(Connection conn, List<Pass> passes, int batchSize) throws SQLException {
        long start = System.nanoTime();
        boolean autoCommit = conn.getAutoCommit();
        String sql = "INSERT INTO bus_pass (pass_id, register_no, name, route_no, boarding_stop, valid_until, amount_paise) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            /* TODO 3 PreparedStatement batch inside one transaction */
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int count = 0;
                for (Pass p : passes) {
                    bindPass(ps, p);
                    ps.addBatch();
                    count++;
                    if (count % batchSize == 0) {
                        ps.executeBatch();
                    }
                }
                ps.executeBatch(); // flush remaining records
                conn.commit();
            }
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }
        return (System.nanoTime() - start);
    }

    private static void bindPass(PreparedStatement ps, Pass p) throws SQLException {
        ps.setString(1, p.passId());
        ps.setString(2, p.registerNo());
        ps.setString(3, p.name());
        ps.setString(4, p.routeNo());
        ps.setString(5, p.boardingStop());
        ps.setDate(6, Date.valueOf(p.validUntil()));
        ps.setLong(7, p.amountPaise());
    }

    public static void printMetaData(Connection conn) throws SQLException {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("  Column Types and Metadata from ResultSetMetaData (bus_pass)");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-3s | %-16s | %-12s | %-6s | %-9s | %-20s%n",
                "#", "Column Name", "SQL Type", "Size", "Nullable", "Java Class");
        System.out.println("--------------------------------------------------------------------------------");

        String sql = "SELECT * FROM bus_pass LIMIT 1";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                String nullDesc = (meta.isNullable(i) == ResultSetMetaData.columnNoNulls) ? "NO" : "YES";
                System.out.printf("%-3d | %-16s | %-12s | %-6d | %-9s | %-20s%n",
                        i,
                        meta.getColumnName(i),
                        meta.getColumnTypeName(i),
                        meta.getColumnDisplaySize(i),
                        nullDesc,
                        meta.getColumnClassName(i)
                );
            }
        }
        System.out.println("--------------------------------------------------------------------------------");
    }

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  CSA09 Assignment 5 - Task B3: PassLoader Verification Demo (10 Records)");
        System.out.println("  Student: R. Ranjith Kumar | Reg No: 1924L260003");
        System.out.println("================================================================================");

        try (Connection conn = DbConfig.getConnection()) {
            System.out.println("Connected to MySQL: " + DbConfig.getDbUrl());

            // 1. Generate 10 test records for each approach
            List<Pass> passesStmt = generatePasses("S", 10);
            List<Pass> passesPrep = generatePasses("P", 10);
            List<Pass> passesBatch = generatePasses("B", 10);

            // Clean bus_pass
            try (Statement st = conn.createStatement()) { st.executeUpdate("TRUNCATE TABLE bus_pass"); }

            System.out.println("\n--- Inserting 10 Records, Three Ways, Verified by SELECT ---");
            System.out.printf("%-26s | %-12s | %-8s | %-16s%n", "Approach", "Time (ms)", "Rows", "Verified");
            System.out.println("--------------------------------------------------------------------------------");

            // Statement
            long tStmt = loadWithStatement(conn, passesStmt) / 1_000_000;
            int countStmt = countRows(conn, "S%");
            System.out.printf("%-26s | %12d | %8d | %-16s%n",
                    "Statement", tStmt, countStmt, countStmt == 10 ? "10 rows present" : "FAILED");

            // PreparedStatement
            long tPrep = loadWithPreparedStatement(conn, passesPrep) / 1_000_000;
            int countPrep = countRows(conn, "P%");
            System.out.printf("%-26s | %12d | %8d | %-16s%n",
                    "PreparedStatement", tPrep, countPrep, countPrep == 10 ? "10 rows present" : "FAILED");

            // Batch
            long tBatch = loadWithBatch(conn, passesBatch, 10) / 1_000_000;
            int countBatch = countRows(conn, "B%");
            System.out.printf("%-26s | %12d | %8d | %-16s%n",
                    "PreparedStatement batch", tBatch, countBatch, countBatch == 10 ? "10 rows present" : "FAILED");

            System.out.println("--------------------------------------------------------------------------------");

            // 2. Report ResultSetMetaData
            printMetaData(conn);

        } catch (Exception e) {
            System.err.println("PassLoader demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static List<Pass> generatePasses(String prefix, int n) {
        List<Pass> list = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            list.add(new Pass(
                    String.format("%s%05d", prefix, i),
                    String.format("1924L%05d", (260000 + i)),
                    "Student_" + prefix + "_" + i,
                    "R" + (1 + (i % 20)),
                    "Stop_" + (1 + (i % 30)),
                    LocalDate.of(2027, 3, 31),
                    450000 + (i * 100)
            ));
        }
        return list;
    }

    private static int countRows(Connection conn, String pattern) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM bus_pass WHERE pass_id LIKE ?")) {
            ps.setString(1, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }
}
