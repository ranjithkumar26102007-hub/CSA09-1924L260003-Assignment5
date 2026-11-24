import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Task B4: Enterprise Pass Migration Engine.
 * Migrates bus pass records from legacy text files into an enterprise relational table,
 * applying two-tier validation (syntactic + business rules), batched transactional loading,
 * rejected record accounting reconciliation, and DatabaseMetaData inspection.
 *
 * Course: CSA09 – Programming in Java
 * Student: R. Ranjith Kumar | Register Number: 1924L260003
 */
public class PassMigration {

    public record Rejection(int lineNumber, String reason, String rawLine) {}

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  CSA09 Assignment 5 - Task B4: Enterprise Migration Engine (12 Campuses)");
        System.out.println("  Student: R. Ranjith Kumar | Reg No: 1924L260003");
        System.out.println("================================================================================");

        Path sourceFile = Paths.get("Task04", "input", "migration_source.txt");
        if (sourceFile.getParent() != null && !Files.exists(sourceFile.getParent())) {
            try { Files.createDirectories(sourceFile.getParent()); } catch (IOException ignored) {}
        }

        try (Connection conn = DbConfig.getConnection()) {
            System.out.println("Connected to MySQL: " + DbConfig.getDbUrl());

            // 1. Inspect DatabaseMetaData
            printDatabaseMetaData(conn);

            // 2. Ensure test migration source file exists
            if (!Files.exists(sourceFile) || Files.size(sourceFile) < 1_000_000) {
                generateMigrationSource(sourceFile, 50_000);
            }

            // 3. Clean destination table
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("TRUNCATE TABLE enterprise_bus_pass");
            }

            // 4. Execute Migration Pipeline
            System.out.println("\n--- Executing Migration Pipeline (Batched Transaction) ---");
            long startTime = System.currentTimeMillis();

            List<Rejection> rejections = new ArrayList<>();
            Set<String> seenPassIds = new HashSet<>(60_000);
            int linesRead = 0;
            int migratedCount = 0;
            int batchSize = 1000;

            String insertSql = "INSERT INTO enterprise_bus_pass (pass_id, campus_id, register_no, name, route_no, boarding_stop, valid_until, amount_paise) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            boolean origAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try (BufferedReader reader = Files.newBufferedReader(sourceFile, StandardCharsets.UTF_8);
                 PreparedStatement ps = conn.prepareStatement(insertSql)) {

                String line;
                int pendingInBatch = 0;

                while ((line = reader.readLine()) != null) {
                    linesRead++;

                    if (line.isBlank()) {
                        rejections.add(new Rejection(linesRead, "blank line", ""));
                        continue;
                    }

                    String[] fields = line.split("\\|", -1);
                    if (fields.length != 7) {
                        rejections.add(new Rejection(linesRead, "expected 7 fields, found " + fields.length, line));
                        continue;
                    }

                    String pId = fields[0].trim();
                    String regNo = fields[1].trim();
                    String name = fields[2].trim();
                    String route = fields[3].trim();
                    String stop = fields[4].trim();
                    String dateStr = fields[5].trim();
                    String amtStr = fields[6].trim();

                    // Business Rule 1: Register number must be exactly 9 to 11 alphanumeric characters
                    if (!regNo.matches("^[0-9A-Za-z]{9,11}$")) {
                        rejections.add(new Rejection(linesRead, "register number is not 9 to 11 characters: " + regNo, line));
                        continue;
                    }

                    // Business Rule 2: Non-numeric amount or non-positive
                    long amountPaise;
                    try {
                        amountPaise = Long.parseLong(amtStr);
                        if (amountPaise <= 0) {
                            rejections.add(new Rejection(linesRead, "amount is not positive: " + amountPaise, line));
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        rejections.add(new Rejection(linesRead, "non-numeric amount: " + amtStr, line));
                        continue;
                    }

                    // Business Rule 3: Amount ceiling of Rs 50,000 (5,000,000 paise)
                    if (amountPaise > 5_000_000) {
                        rejections.add(new Rejection(linesRead, "amount above the Rs 50,000 ceiling: " + amountPaise, line));
                        continue;
                    }

                    // Business Rule 4: Valid date format and validity date not before 2020
                    LocalDate validDate;
                    try {
                        validDate = LocalDate.parse(dateStr);
                        if (validDate.getYear() < 2020) {
                            rejections.add(new Rejection(linesRead, "validity date before 2020: " + dateStr, line));
                            continue;
                        }
                    } catch (Exception e) {
                        rejections.add(new Rejection(linesRead, "bad date: " + dateStr, line));
                        continue;
                    }

                    // Business Rule 5: Duplicate pass ID in file
                    if (seenPassIds.contains(pId)) {
                        rejections.add(new Rejection(linesRead, "duplicate pass ID in file: " + pId, line));
                        continue;
                    }
                    seenPassIds.add(pId);

                    // Assign campus dynamically (simulate 12 campuses)
                    int campusNum = 1 + (migratedCount % 12);
                    String campusId = String.format("CAMPUS_%02d", campusNum);

                    // Add to batch
                    ps.setString(1, pId);
                    ps.setString(2, campusId);
                    ps.setString(3, regNo);
                    ps.setString(4, name);
                    ps.setString(5, route);
                    ps.setString(6, stop);
                    ps.setDate(7, Date.valueOf(validDate));
                    ps.setLong(8, amountPaise);
                    ps.addBatch();

                    migratedCount++;
                    pendingInBatch++;

                    if (pendingInBatch % batchSize == 0) {
                        ps.executeBatch();
                        pendingInBatch = 0;
                    }
                }

                if (pendingInBatch > 0) {
                    ps.executeBatch();
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(origAutoCommit);
            }

            long elapsedMs = System.currentTimeMillis() - startTime;
            int tableRows = getRowCount(conn, "enterprise_bus_pass");

            // 5. Reconciliation Report
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("  MIGRATION RECONCILIATION SUMMARY");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.printf("%-30s : %s (%d bytes)%n", "Source File", sourceFile.getFileName(), Files.size(sourceFile));
            System.out.printf("%-30s : %d%n", "Total Lines Read", linesRead);
            System.out.printf("%-30s : %d%n", "Valid Records Migrated", migratedCount);
            System.out.printf("%-30s : %d%n", "Corrupt Records Rejected", rejections.size());
            System.out.printf("%-30s : %d ms%n", "Total Migration Time", elapsedMs);
            System.out.printf("%-30s : %d%n", "Rows in enterprise_bus_pass", tableRows);

            boolean reconciled = (linesRead == (migratedCount + rejections.size()));
            System.out.printf("%-30s : %s%n", "Accounting Identity Reconciled?", (reconciled ? "YES, every line accounted for" : "NO"));
            System.out.println("--------------------------------------------------------------------------------");

            // 6. Rejected Records Audit Log
            System.out.println("\n--- Rejected Record Audit Log (" + rejections.size() + " Faults Injected & Caught) ---");
            System.out.printf("%-8s | %-42s | %-24s%n", "Line", "Specific Rejection Reason", "Status");
            System.out.println("--------------------------------------------------------------------------------");
            for (Rejection r : rejections) {
                System.out.printf("%-8d | %-42s | %-24s%n", r.lineNumber(), r.reason(), "REJECTED & LOGGED");
            }
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("================================================================================");

        } catch (Exception e) {
            System.err.println("Migration engine failure: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printDatabaseMetaData(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("  DatabaseMetaData Engine Inspection");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-28s : %s%n", "Database Product Name", meta.getDatabaseProductName());
        System.out.printf("%-28s : %s%n", "Database Product Version", meta.getDatabaseProductVersion());
        System.out.printf("%-28s : %s%n", "JDBC Driver Name", meta.getDriverName());
        System.out.printf("%-28s : %s%n", "JDBC Driver Version", meta.getDriverVersion());
        System.out.printf("%-28s : %d.%d%n", "JDBC Specification Version", meta.getJDBCMajorVersion(), meta.getJDBCMinorVersion());
        System.out.printf("%-28s : %b%n", "Supports Transactions?", meta.supportsTransactions());
        System.out.printf("%-28s : %b%n", "Supports Batch Updates?", meta.supportsBatchUpdates());

        String isolation = switch (meta.getDefaultTransactionIsolation()) {
            case Connection.TRANSACTION_READ_COMMITTED -> "READ_COMMITTED";
            case Connection.TRANSACTION_READ_UNCOMMITTED -> "READ_UNCOMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ -> "REPEATABLE_READ";
            case Connection.TRANSACTION_SERIALIZABLE -> "SERIALIZABLE";
            default -> "UNKNOWN";
        };
        System.out.printf("%-28s : %s%n", "Default Transaction Isolation", isolation);

        System.out.print("Discovered Catalog Tables    : ");
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = meta.getTables("csa09_assignment5", null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        System.out.println(String.join(", ", tables));
        System.out.println("--------------------------------------------------------------------------------");
    }

    private static void generateMigrationSource(Path file, int validCount) throws IOException {
        System.out.print("[Generating synthetic 50,000-line migration source file with 8 injected faults... ");
        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            int lineNo = 0;
            for (int i = 1; i <= validCount; i++) {
                lineNo++;

                // Inject 8 precise faults matching assignment specification
                if (lineNo == 1001) {
                    bw.write("P990001|INVALID_REG|Student_1001|R01|Stop_01|2027-03-31|450000\n"); // invalid reg number
                    continue;
                }
                if (lineNo == 2002) {
                    bw.write("P990002|1924L260002|Student_2002|R01|Stop_01|2027-03-31|-500\n"); // negative amount
                    continue;
                }
                if (lineNo == 3003) {
                    bw.write("P990003|1924L260003|Student_3003|R01|Stop_01|2027-03-31|6500000\n"); // > Rs 50k ceiling
                    continue;
                }
                if (lineNo == 4004) {
                    bw.write("P990004|1924L260004|Student_4004|R01|Stop_01\n"); // 5 fields instead of 7
                    continue;
                }
                if (lineNo == 5005) {
                    bw.write("   \n"); // blank line
                    continue;
                }
                if (lineNo == 6006) {
                    bw.write("P990006|1924L260006|Student_6006|R01|Stop_01|2018-03-31|450000\n"); // date before 2020
                    continue;
                }
                if (lineNo == 7007) {
                    bw.write("P000001|1924L260007|Student_7007|R01|Stop_01|2027-03-31|450000\n"); // duplicate ID P000001
                    continue;
                }
                if (lineNo == 8008) {
                    bw.write("P990008|1924L260008|Student_8008|R01|Stop_01|31-03-2027|450000\n"); // bad date format
                    continue;
                }

                String pId = String.format("P%06d", i);
                String regNo = String.format("1924L%05d", (260000 + (i % 9999)));
                String name = "Student_" + i;
                String route = "R" + (1 + (i % 25));
                String stop = "Stop_" + (1 + (i % 40));
                String date = "2027-03-31";
                long amount = 450000 + (i % 500) * 10;

                bw.write(String.format("%s|%s|%s|%s|%s|%s|%d\n",
                        pId, regNo, name, route, stop, date, amount));
            }
        }
        System.out.println("Done]");
    }

    private static int getRowCount(Connection conn, String table) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM " + table)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }
}
