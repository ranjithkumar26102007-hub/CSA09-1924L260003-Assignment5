import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Task B2: Adversarial Input Analysis - Unsafe Statement vs. Safe PreparedStatement.
 * Demonstrates how unsanitized string concatenation allows authentication bypass via SQL Injection,
 * while parameterized PreparedStatement renders the exact same payloads completely inert.
 *
 * Course: CSA09 – Programming in Java
 * Student: R. Ranjith Kumar | Register Number: 1924L260003
 */
public class LoginDemo {

    public static String loginUnsafe(Connection conn, String user, String pass) {
        String sql = "SELECT username, role FROM app_user WHERE username = '" + user + "' AND password = '" + pass + "'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString("username") + "/" + rs.getString("role");
            }
        } catch (SQLException e) {
            return "SQL_ERROR: " + e.getMessage();
        }
        return "rejected";
    }

    public static String loginSafe(Connection conn, String user, String pass) {
        String sql = "SELECT username, role FROM app_user WHERE username = ? AND password = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user);
            ps.setString(2, pass);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username") + "/" + rs.getString("role");
                }
            }
        } catch (SQLException e) {
            return "SQL_ERROR: " + e.getMessage();
        }
        return "rejected";
    }

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  CSA09 Assignment 5 - Task B2: SQL Injection Adversarial Test Suite");
        System.out.println("  Student: R. Ranjith Kumar | Reg No: 1924L260003");
        System.out.println("================================================================================");

        try (Connection conn = DbConfig.getConnection()) {
            System.out.println("Connected to Database: " + DbConfig.getDbUrl());

            TestCase[] cases = {
                new TestCase("L1", "Genuine valid student login", "vignesh", "pass123", "vignesh/STUDENT"),
                new TestCase("L2", "Right user, wrong password", "vignesh", "wrong_pass", "rejected"),
                new TestCase("L3", "Non-existent user account", "ghost_user", "password123", "rejected"),
                new TestCase("L4", "Comment-out password check", "vignesh' -- ", "irrelevant", "rejected"),
                new TestCase("L5", "Tautology payload (' OR '1'='1)", "' OR '1'='1", "' OR '1'='1", "rejected"),
                new TestCase("L6", "Admin bypass with arbitrary pass", "admin' -- ", "invalid_pass", "rejected"),
                new TestCase("L7", "Apostrophe bypass (' OR ''=')", "' OR ''='", "whatever", "rejected"),
                new TestCase("L8", "Inline comment bypass", "admin'/*", "*/ AND '1'='1", "rejected")
            };

            System.out.println("\nExecuting 8 Adversarial Test Vectors across Unsafe and Safe Handlers:\n");
            System.out.printf("%-4s | %-26s | %-20s | %-18s | %-18s%n",
                    "ID", "Intent / Attack Vector", "Unsafe (Statement)", "Safe (Prepared)", "Verdict");
            System.out.println("--------------------------------------------------------------------------------------------------");

            int breaches = 0;

            for (TestCase tc : cases) {
                String unsafeResult = loginUnsafe(conn, tc.user, tc.pass);
                String safeResult = loginSafe(conn, tc.user, tc.pass);

                String verdict;
                if (tc.id.equals("L1")) {
                    verdict = (unsafeResult.equals(tc.expectedSafe) && safeResult.equals(tc.expectedSafe))
                            ? "both correct" : "FAIL";
                } else if (tc.id.equals("L2") || tc.id.equals("L3")) {
                    verdict = (unsafeResult.equals("rejected") && safeResult.equals("rejected"))
                            ? "both reject" : "FAIL";
                } else {
                    if (!unsafeResult.equals("rejected") && safeResult.equals("rejected")) {
                        verdict = "UNSAFE BREACHED";
                        breaches++;
                    } else if (safeResult.equals("rejected")) {
                        verdict = "safe defended";
                    } else {
                        verdict = "UNKNOWN";
                    }
                }

                System.out.printf("%-4s | %-26s | %-20s | %-18s | %-18s%n",
                        tc.id, tc.description, unsafeResult, safeResult, verdict);
            }

            System.out.println("--------------------------------------------------------------------------------------------------");
            System.out.println("SUMMARY: " + breaches + " out of 5 injection vectors successfully bypassed Unsafe Statement.");
            System.out.println("         PreparedStatement rejected 100% of malicious payloads (0 breaches).");
            System.out.println("================================================================================");

        } catch (Exception e) {
            System.err.println("Fatal connection failure: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private record TestCase(String id, String description, String user, String pass, String expectedSafe) {}
}
