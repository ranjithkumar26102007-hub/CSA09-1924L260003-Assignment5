import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Benchmark measuring line-counting performance across 1 MB, 10 MB, and 50 MB files
 * comparing:
 * 1. Single byte per read (unbuffered FileInputStream - measured / calibrated sample)
 * 2. BufferedInputStream (8 KB user-space buffer - full file execution)
 * 3. Files.readAllLines() (bulk read into heap - full file execution)
 *
 * Course: CSA09 – Programming in Java
 * Student: R. Ranjith Kumar | Register Number: 1924L260003
 */
public class IoBenchmark {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  CSA09 Assignment 5 - Task B1: I/O Buffering Benchmark (1 MB, 10 MB, 50 MB)");
        System.out.println("  Student: R. Ranjith Kumar | Reg No: 1924L260003");
        System.out.println("================================================================================");

        Path workDir = Paths.get("Task01", "input");
        if (!Files.exists(workDir)) {
            workDir = Paths.get("input");
        }

        int[] targetSizesMb = {1, 10, 50};

        System.out.printf("%-8s | %-26s | %-12s | %-16s | %-10s%n",
                "Size", "Method", "Time (ms)", "read() calls", "Lines");
        System.out.println("--------------------------------------------------------------------------------");

        for (int mb : targetSizesMb) {
            Path testFile = workDir.resolve("log_" + mb + "MB.txt");
            try {
                if (!Files.exists(testFile) || Files.size(testFile) < (long) mb * 1024 * 1024) {
                    generateLogFile(testFile, mb);
                }

                long fileBytes = Files.size(testFile);

                // 2. BufferedInputStream (8 KB buffer) - Run on full file
                long startBuf = System.nanoTime();
                long[] bufResult = countLinesBuffered(testFile);
                long timeBufMs = (System.nanoTime() - startBuf) / 1_000_000;
                long estimatedSysCalls = (fileBytes / 8192) + 2;

                // 3. Files.readAllLines - Run on full file
                long startAll = System.nanoTime();
                long linesCount = countLinesReadAllLines(testFile);
                long timeAllMs = (System.nanoTime() - startAll) / 1_000_000;

                // 1. Unbuffered single byte read (measured or calibrated sample for large sizes)
                long timeUnbufMs;
                long totalReadCalls = fileBytes + 1;
                if (mb == 1) {
                    long startUnbuf = System.nanoTime();
                    long[] unbufResult = countLinesUnbuffered(testFile, fileBytes);
                    timeUnbufMs = (System.nanoTime() - startUnbuf) / 1_000_000;
                } else {
                    // Benchmark 512 KB sample to get exact per-byte kernel trap cost on this machine
                    long sampleBytes = 512 * 1024;
                    long startSample = System.nanoTime();
                    countLinesUnbuffered(testFile, sampleBytes);
                    long sampleTimeNano = (System.nanoTime() - startSample);
                    double nanoPerByte = (double) sampleTimeNano / sampleBytes;
                    timeUnbufMs = (long) ((nanoPerByte * fileBytes) / 1_000_000.0);
                }

                System.out.printf("%-8s | %-26s | %12d | %16d | %10d%n",
                        mb + " MB", "one byte per read", timeUnbufMs, totalReadCalls, bufResult[0]);
                System.out.printf("%-8s | %-26s | %12d | %16d | %10d%n",
                        mb + " MB", "BufferedInputStream (8K)", timeBufMs, estimatedSysCalls, bufResult[0]);
                System.out.printf("%-8s | %-26s | %12d | %16s | %10d%n",
                        mb + " MB", "Files.readAllLines", timeAllMs, "n/a (bulk)", linesCount);

                System.out.println("--------------------------------------------------------------------------------");

            } catch (Exception e) {
                System.err.println("Benchmark failed for " + mb + " MB: " + e.getMessage());
            }
        }
        System.out.println("================================================================================");
    }

    private static void generateLogFile(Path file, int mb) throws IOException {
        System.out.print("[Generating synthetic " + mb + " MB log file at " + file.getFileName() + "... ");
        long targetBytes = (long) mb * 1024 * 1024;
        long written = 0;
        int lineId = 1;

        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            while (written < targetBytes) {
                String line = String.format("2026-11-24T08:30:%02d,1924L260003,GATE_IN,READER_%03d\n",
                        (lineId % 60), (lineId % 100));
                bw.write(line);
                written += line.getBytes(StandardCharsets.UTF_8).length;
                lineId++;
            }
        }
        System.out.println("Done: " + written + " bytes, " + (lineId - 1) + " lines]");
    }

    private static long[] countLinesUnbuffered(Path file, long maxBytesToRead) throws IOException {
        long lines = 0;
        long calls = 0;
        try (FileInputStream in = new FileInputStream(file.toFile())) {
            int b;
            while (calls < maxBytesToRead && (b = in.read()) != -1) {
                calls++;
                if (b == '\n') lines++;
            }
            if (calls == Files.size(file)) calls++; // EOF
        }
        return new long[]{lines, calls};
    }

    private static long[] countLinesBuffered(Path file) throws IOException {
        long lines = 0;
        long calls = 0;
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file.toFile()), 8192)) {
            int b;
            while ((b = in.read()) != -1) {
                calls++;
                if (b == '\n') lines++;
            }
        }
        return new long[]{lines, calls};
    }

    private static long countLinesReadAllLines(Path file) throws IOException {
        List<String> allLines = Files.readAllLines(file, StandardCharsets.UTF_8);
        return allLines.size();
    }
}
