import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Benchmark measuring search latency and memory trade-offs:
 * 1. Pass search latency as file grows from 1,000 to 100,000 records (First, Middle, Last, Missing)
 * 2. Scan per search vs. in-memory HashMap index for 1, 10, 100, and 1,000 searches.
 *
 * Course: CSA09 – Programming in Java
 * Student: R. Ranjith Kumar | Register Number: 1924L260003
 */
public class SearchBenchmark {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  CSA09 Assignment 5 - Task B1: Pass Search & HashMap Benchmark");
        System.out.println("  Student: R. Ranjith Kumar | Reg No: 1924L260003");
        System.out.println("================================================================================");

        Path workDir = Paths.get("Task01", "input");
        if (!Files.exists(workDir)) {
            workDir = Paths.get("input");
        }

        int[] recordCounts = {1_000, 10_000, 50_000, 100_000};

        System.out.println("\n--- Experiment 1: Search Latency by Position across File Sizes ---");
        System.out.printf("%-10s | %-12s | %-12s | %-12s | %-12s%n",
                "Records", "First (ms)", "Middle (ms)", "Last (ms)", "Missing (ms)");
        System.out.println("--------------------------------------------------------------------------------");

        for (int count : recordCounts) {
            Path file = workDir.resolve("passes_" + count + ".txt");
            try {
                if (!Files.exists(file) || countLines(file) < count) {
                    generatePassFile(file, count);
                }

                PassStore store = new PassStore(file);

                String firstId = String.format("P%06d", 1);
                String midId = String.format("P%06d", count / 2);
                String lastId = String.format("P%06d", count);
                String missingId = "P999999_NONEXISTENT";

                // Warm up
                store.find(firstId);

                double tFirst = measureFindTime(store, firstId, 10);
                double tMid = measureFindTime(store, midId, 5);
                double tLast = measureFindTime(store, lastId, 5);
                double tMissing = measureFindTime(store, missingId, 5);

                System.out.printf("%-10d | %12.3f | %12.3f | %12.3f | %12.3f%n",
                        count, tFirst, tMid, tLast, tMissing);

            } catch (Exception e) {
                System.err.println("Benchmark failed for count " + count + ": " + e.getMessage());
            }
        }

        // Experiment 2: Scan vs HashMap on 100,000 passes
        System.out.println("\n--- Experiment 2: Scan vs. HashMap Index (100,000 passes) ---");
        System.out.printf("%-10s | %-16s | %-18s | %-15s | %-10s%n",
                "Searches", "Scan Total (ms)", "HashMap Total (ms)", "Map Build (ms)", "Winner");
        System.out.println("--------------------------------------------------------------------------------");

        Path bigFile = workDir.resolve("passes_100000.txt");
        try {
            PassStore store = new PassStore(bigFile);

            // Measure HashMap build time
            long startBuild = System.nanoTime();
            Map<String, BusPass> index = new HashMap<>(150_000);
            try (BufferedReader r = Files.newBufferedReader(bigFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.isBlank()) continue;
                    BusPass p = BusPass.parse(line);
                    index.put(p.passId(), p);
                }
            }
            double mapBuildMs = (System.nanoTime() - startBuild) / 1_000_000.0;

            int[] searchBatches = {1, 10, 100, 1_000};
            for (int q : searchBatches) {
                // Measure scan time for q searches (looking up middle/random records)
                long startScan = System.nanoTime();
                for (int i = 0; i < q; i++) {
                    int targetIdx = (i * 97) % 100_000 + 1;
                    String targetId = String.format("P%06d", targetIdx);
                    store.find(targetId);
                }
                double scanTotalMs = (System.nanoTime() - startScan) / 1_000_000.0;

                // Measure HashMap lookups
                long startMap = System.nanoTime();
                for (int i = 0; i < q; i++) {
                    int targetIdx = (i * 97) % 100_000 + 1;
                    String targetId = String.format("P%06d", targetIdx);
                    index.get(targetId);
                }
                double mapLookupMs = (System.nanoTime() - startMap) / 1_000_000.0;
                double mapTotalMs = mapBuildMs + mapLookupMs;

                String winner = (scanTotalMs < mapTotalMs) ? "scan" : "HashMap";
                System.out.printf("%-10d | %16.2f | %18.2f | %15.2f | %-10s%n",
                        q, scanTotalMs, mapTotalMs, mapBuildMs, winner);
            }

        } catch (Exception e) {
            System.err.println("Experiment 2 failed: " + e.getMessage());
        }

        System.out.println("================================================================================");
    }

    private static double measureFindTime(PassStore store, String id, int iterations) throws IOException {
        long totalNano = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            store.find(id);
            totalNano += (System.nanoTime() - start);
        }
        return (totalNano / (double) iterations) / 1_000_000.0;
    }

    private static void generatePassFile(Path file, int count) throws IOException {
        System.out.print("[Generating synthetic " + count + " passes at " + file.getFileName() + "... ");
        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (int i = 1; i <= count; i++) {
                String passId = String.format("P%06d", i);
                String regNo = String.format("1924L%05d", (260000 + (i % 9999)));
                String name = "Student_" + i;
                String route = "R" + (1 + (i % 25));
                String stop = "Stop_" + (1 + (i % 50));
                String date = "2027-03-31";
                long amount = 450000 + (i % 1000) * 100;

                bw.write(String.format("%s|%s|%s|%s|%s|%s|%d\n",
                        passId, regNo, name, route, stop, date, amount));
            }
        }
        System.out.println("Done]");
    }

    private static long countLines(Path file) throws IOException {
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            long lines = 0;
            while (r.readLine() != null) lines++;
            return lines;
        }
    }
}
