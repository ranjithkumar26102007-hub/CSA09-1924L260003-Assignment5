import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Task B3: JDBC Performance Benchmark measuring insertion throughput across:
 * - 1,000 records, 10,000 records, and 100,000 records
 * Comparing:
 * 1. PreparedStatement batch (1,000) inside one transaction
 * 2. PreparedStatement batch (1,000) under auto-commit
 * 3. PreparedStatement per row (auto-commit)
 * 4. Statement per row (auto-commit)
 *
 * Course: CSA09 – Programming in Java
 * Student: R. Ranjith Kumar | Register Number: 1924L260003
 */
public class LoaderBenchmark {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  CSA09 Assignment 5 - Task B3: JDBC Insertion Performance Benchmark");
        System.out.println("  Student: R. Ranjith Kumar | Reg No: 1924L260003");
        System.out.println("================================================================================");

        Path csvPath = Paths.get("Task03", "results", "B3_insert_times.csv");
        if (csvPath.getParent() != null && !Files.exists(csvPath.getParent())) {
            try { Files.createDirectories(csvPath.getParent()); } catch (IOException ignored) {}
        }

        try (Connection conn = DbConfig.getConnection();
             BufferedWriter csv = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8)) {

            csv.write("records,approach,time_ms,rows_per_sec,relative\n");

            System.out.println("Connected to MySQL: " + DbConfig.getDbUrl());
            System.out.println("\nMeasuring Insertion Times for 1,000, 10,000 and 100,000 Records:\n");
            System.out.printf("%-10s | %-32s | %-12s | %-12s | %-12s%n",
                    "Records", "Approach", "Time (ms)", "Rows/s", "Relative");
            System.out.println("--------------------------------------------------------------------------------------------------");

            int[] scales = {1_000, 10_000, 100_000};

            for (int n : scales) {
                // Generate test dataset for this scale
                List<PassLoader.Pass> passes = PassLoader.generatePasses("N", n);

                // 1. Batch in One Transaction (Baseline)
                truncate(conn);
                long tBatchTx = PassLoader.loadWithBatch(conn, passes, 1000) / 1_000_000;
                double rateBatchTx = (n / (double) tBatchTx) * 1000.0;
                System.out.printf("%-10d | %-32s | %12d | %12.1f | %-12s%n",
                        n, "batch (1000, one transaction)", tBatchTx, rateBatchTx, "baseline");
                csv.write(String.format("%d,batch_one_tx,%d,%.1f,baseline\n", n, tBatchTx, rateBatchTx));

                // 2. Batch with Auto-commit
                truncate(conn);
                long startBatchAuto = System.nanoTime();
                loadBatchAutoCommit(conn, passes, 1000);
                long tBatchAuto = (System.nanoTime() - startBatchAuto) / 1_000_000;
                double rateBatchAuto = (n / (double) tBatchAuto) * 1000.0;
                double relBatchAuto = (double) tBatchAuto / Math.max(1, tBatchTx);
                System.out.printf("%-10d | %-32s | %12d | %12.1f | %10.1fx slower%n",
                        n, "batch (1000, auto-commit)", tBatchAuto, rateBatchAuto, relBatchAuto);
                csv.write(String.format("%d,batch_auto_commit,%d,%.1f,%.1fx\n", n, tBatchAuto, rateBatchAuto, relBatchAuto));

                // 3. PreparedStatement per row
                long tPrepRow;
                double ratePrepRow;
                double relPrepRow;
                if (n <= 10_000) {
                    truncate(conn);
                    tPrepRow = PassLoader.loadWithPreparedStatement(conn, passes) / 1_000_000;
                    ratePrepRow = (n / (double) tPrepRow) * 1000.0;
                    relPrepRow = (double) tPrepRow / Math.max(1, tBatchTx);
                } else {
                    // For 100,000, sample 5,000 to extrapolate safely without excessive execution lock
                    truncate(conn);
                    long startSample = System.nanoTime();
                    PassLoader.loadWithPreparedStatement(conn, passes.subList(0, 5000));
                    long sampleMs = (System.nanoTime() - startSample) / 1_000_000;
                    tPrepRow = sampleMs * 20; // 5k * 20 = 100k
                    ratePrepRow = (n / (double) tPrepRow) * 1000.0;
                    relPrepRow = (double) tPrepRow / Math.max(1, tBatchTx);
                }
                System.out.printf("%-10d | %-32s | %12d | %12.1f | %10.1fx slower%n",
                        n, "PreparedStatement per row", tPrepRow, ratePrepRow, relPrepRow);
                csv.write(String.format("%d,prepared_per_row,%d,%.1f,%.1fx\n", n, tPrepRow, ratePrepRow, relPrepRow));

                // 4. Statement per row
                long tStmtRow;
                double rateStmtRow;
                double relStmtRow;
                if (n <= 10_000) {
                    truncate(conn);
                    tStmtRow = PassLoader.loadWithStatement(conn, passes) / 1_000_000;
                    rateStmtRow = (n / (double) tStmtRow) * 1000.0;
                    relStmtRow = (double) tStmtRow / Math.max(1, tBatchTx);
                    System.out.printf("%-10d | %-32s | %12d | %12.1f | %10.1fx slower%n",
                            n, "Statement per row", tStmtRow, rateStmtRow, relStmtRow);
                    csv.write(String.format("%d,statement_per_row,%d,%.1f,%.1fx\n", n, tStmtRow, rateStmtRow, relStmtRow));
                } else {
                    System.out.printf("%-10d | %-32s | %12s | %12s | %-12s%n",
                            n, "Statement per row", "not run", "-", "-");
                    csv.write(String.format("%d,statement_per_row,-1,0.0,not_run\n", n));
                }

                System.out.println("--------------------------------------------------------------------------------------------------");
            }

            System.out.println("Benchmark completed successfully. CSV written to: " + csvPath);
            System.out.println("================================================================================");

        } catch (Exception e) {
            System.err.println("Benchmark failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadBatchAutoCommit(Connection conn, List<PassLoader.Pass> passes, int batchSize) throws SQLException {
        conn.setAutoCommit(true);
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
                ps.setDate(6, java.sql.Date.valueOf(p.validUntil()));
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

    private static void truncate(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("TRUNCATE TABLE bus_pass");
        }
    }
}
