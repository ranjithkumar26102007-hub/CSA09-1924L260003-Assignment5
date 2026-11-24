import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Domain entity representing an individual bus pass in the University Transport System.
 *
 * Course: CSA09 – Programming in Java
 * Student: R. Ranjith Kumar | Register Number: 1924L260003
 */
public class BusPass {
    private final String passId;
    private final String registerNo;
    private final String name;
    private final String routeNo;
    private final String boardingStop;
    private final LocalDate validUntil;
    private final long amountPaise;

    public BusPass(String passId, String registerNo, String name, String routeNo,
                   String boardingStop, LocalDate validUntil, long amountPaise) {
        this.passId = Objects.requireNonNull(passId, "passId cannot be null").trim();
        this.registerNo = Objects.requireNonNull(registerNo, "registerNo cannot be null").trim();
        this.name = Objects.requireNonNull(name, "name cannot be null").trim();
        this.routeNo = Objects.requireNonNull(routeNo, "routeNo cannot be null").trim();
        this.boardingStop = Objects.requireNonNull(boardingStop, "boardingStop cannot be null").trim();
        this.validUntil = Objects.requireNonNull(validUntil, "validUntil cannot be null");
        this.amountPaise = amountPaise;
    }

    public String passId() { return passId; }
    public String registerNo() { return registerNo; }
    public String name() { return name; }
    public String routeNo() { return routeNo; }
    public String boardingStop() { return boardingStop; }
    public LocalDate validUntil() { return validUntil; }
    public long amountPaise() { return amountPaise; }

    /**
     * Formats this bus pass as a pipe-delimited text line.
     */
    public String toDelimitedLine() {
        return String.join("|",
                passId,
                registerNo,
                name,
                routeNo,
                boardingStop,
                validUntil.toString(),
                String.valueOf(amountPaise)
        );
    }

    /**
     * Parses a pipe-delimited string line into a BusPass record.
     * Throws DamagedRecordException if any column count, format, or date conversion rule is violated.
     */
    public static BusPass parse(String line) throws DamagedRecordException {
        if (line == null || line.isBlank()) {
            throw new DamagedRecordException("Blank line encountered");
        }

        String[] fields = line.split("\\|", -1);
        if (fields.length != 7) {
            throw new DamagedRecordException("expected 7 fields, found " + fields.length);
        }

        String pId = fields[0].trim();
        String rNo = fields[1].trim();
        String sName = fields[2].trim();
        String route = fields[3].trim();
        String stop = fields[4].trim();
        String dateStr = fields[5].trim();
        String amtStr = fields[6].trim();

        if (pId.isEmpty()) throw new DamagedRecordException("empty passId");
        if (rNo.isEmpty()) throw new DamagedRecordException("empty registerNo");
        if (sName.isEmpty()) throw new DamagedRecordException("empty student name");
        if (route.isEmpty()) throw new DamagedRecordException("empty routeNo");
        if (stop.isEmpty()) throw new DamagedRecordException("empty boardingStop");

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new DamagedRecordException("invalid date format: " + dateStr);
        }

        long paise;
        try {
            paise = Long.parseLong(amtStr);
        } catch (NumberFormatException e) {
            throw new DamagedRecordException("non-numeric amount: " + amtStr);
        }

        if (paise < 0) {
            throw new DamagedRecordException("amount cannot be negative: " + paise);
        }

        return new BusPass(pId, rNo, sName, route, stop, date, paise);
    }

    @Override
    public String toString() {
        return "BusPass[passId=" + passId + ", registerNo=" + registerNo +
                ", name=" + name + ", routeNo=" + routeNo +
                ", boardingStop=" + boardingStop + ", validUntil=" + validUntil +
                ", amountPaise=" + amountPaise + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BusPass busPass)) return false;
        return amountPaise == busPass.amountPaise &&
                passId.equals(busPass.passId) &&
                registerNo.equals(busPass.registerNo) &&
                name.equals(busPass.name) &&
                routeNo.equals(busPass.routeNo) &&
                boardingStop.equals(busPass.boardingStop) &&
                validUntil.equals(busPass.validUntil);
    }

    @Override
    public int hashCode() {
        return Objects.hash(passId, registerNo, name, routeNo, boardingStop, validUntil, amountPaise);
    }

    /**
     * Custom checked exception indicating a malformed, torn, or unparseable record.
     */
    public static class DamagedRecordException extends Exception {
        public DamagedRecordException(String message) {
            super(message);
        }
    }
}
