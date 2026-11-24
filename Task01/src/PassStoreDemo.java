import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Verification test harness matching the official assignment specification:
 * - Expected output: P1003 record, followed by null for the damaged P1002 line.
 * - Damaged lines reported: [line 2: expected 7 fields, found 5]
 *
 * Course: CSA09 – Programming in Java
 * Student: R. Ranjith Kumar | Register Number: 1924L260003
 */
public class PassStoreDemo {
    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  CSA09 Assignment 5 - Task B1: Bus-Pass File Store Verification Demo");
        System.out.println("  Student: R. Ranjith Kumar | Reg No: 1924L260003");
        System.out.println("================================================================================");

        Path demoFile = Paths.get("Task01", "input", "b1_demo.txt");
        if (!Files.exists(demoFile)) {
            demoFile = Paths.get("input", "b1_demo.txt");
        }
        if (!Files.exists(demoFile)) {
            demoFile = Paths.get("C:", "Users", "User", "Downloads", "CSA09-1924L260003-Assignment5", "Task01", "input", "b1_demo.txt");
        }

        System.out.println("Target File: " + demoFile.toAbsolutePath());
        PassStore store = new PassStore(demoFile);

        try {
            // Test 1: Query P1003 (Valid record located after damaged record)
            System.out.println("\n--> Test 1: Query valid pass 'P1003' (located after damaged record)");
            BusPass p1003 = store.find("P1003");
            System.out.println("store.find(\"P1003\") -> " + p1003);

            // Test 2: Query P1002 (Corrupted line with 5 fields instead of 7)
            System.out.println("\n--> Test 2: Query damaged pass 'P1002'");
            BusPass p1002 = store.find("P1002");
            System.out.println("store.find(\"P1002\") -> " + p1002 + " (damaged line skipped)");

            List<String> damaged = store.getDamaged();
            System.out.println("damaged lines reported: " + damaged);

            // Verification Check
            boolean matchP1003 = (p1003 != null && p1003.passId().equals("P1003"));
            boolean matchP1002 = (p1002 == null);
            boolean matchDamaged = (damaged.size() == 1 && damaged.get(0).contains("expected 7 fields, found 5"));

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("  SPECIFICATION CONFORMANCE VERIFICATION");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("  [P1003 Record Returned]  : " + (matchP1003 ? "PASS" : "FAIL"));
            System.out.println("  [P1002 Returns null]     : " + (matchP1002 ? "PASS" : "FAIL"));
            System.out.println("  [Damage Line 2 Logged]   : " + (matchDamaged ? "PASS" : "FAIL"));
            System.out.println("  Matches the expected output: " + (matchP1003 && matchP1002 && matchDamaged ? "YES" : "NO"));
            System.out.println("================================================================================");

        } catch (Exception e) {
            System.err.println("Unexpected failure: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
