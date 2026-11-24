import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * File storage engine for bus pass records using delimited text format.
 * Includes error containment, damaged-line skipping, and diagnostic logging.
 *
 * Course: CSA09 – Programming in Java
 * Student: R. Ranjith Kumar | Register Number: 1924L260003
 */
public class PassStore {
    private final Path file;
    private final List<String> damaged = new ArrayList<>();

    public PassStore(Path file) {
        this.file = file;
    }

    /**
     * Appends a new pass record to the file, creating parent directories and the file if needed.
     */
    public synchronized void add(BusPass pass) throws IOException {
        if (file.getParent() != null && !Files.exists(file.getParent())) {
            Files.createDirectories(file.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(pass.toDelimitedLine());
            writer.newLine();
            writer.flush();
        }
    }

    /**
     * Finds a pass record by pass ID.
     *
     * TODO 1 in PassStore.find(): read line by line, skip damaged lines and return the matching record.
     */
    public BusPass find(String passId) throws IOException {
        damaged.clear();
        if (!Files.exists(file)) {
            throw new NoSuchFileException(file.toString());
        }

        int lineNo = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue; // Skip blank lines without penalty
                }

                try {
                    BusPass p = BusPass.parse(line);
                    if (p.passId().equals(passId)) {
                        return p; // First match wins
                    }
                } catch (BusPass.DamagedRecordException e) {
                    // Record damage diagnostics while preserving stream traversal
                    damaged.add("line " + lineNo + ": " + e.getMessage());
                }
            }
        }
        return null; // Not found
    }

    /**
     * Lists all valid pass records registered for a specific route.
     */
    public List<BusPass> listByRoute(String routeNo) throws IOException {
        damaged.clear();
        if (!Files.exists(file)) {
            throw new NoSuchFileException(file.toString());
        }

        List<BusPass> matches = new ArrayList<>();
        int lineNo = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;

                try {
                    BusPass p = BusPass.parse(line);
                    if (p.routeNo().equalsIgnoreCase(routeNo)) {
                        matches.add(p);
                    }
                } catch (BusPass.DamagedRecordException e) {
                    damaged.add("line " + lineNo + ": " + e.getMessage());
                }
            }
        }
        return matches;
    }

    /**
     * Returns an unmodifiable view of damaged line diagnostic records from the most recent scan.
     */
    public List<String> getDamaged() {
        return Collections.unmodifiableList(damaged);
    }
}
