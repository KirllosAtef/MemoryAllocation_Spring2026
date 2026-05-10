package model.service;

/**
 * Converts raw byte values to/from the user-selected display unit.
 *
 * SRP: only responsible for unit math and formatting.
 * All classes that need to display sizes receive this service via constructor
 * or setter — nothing reads a static field anymore.
 */
public class UnitConverter {

    public enum Unit {
        B  ("B",   1L),
        KB ("KB",  1_024L),
        MB ("MB",  1_024L * 1_024),
        GB ("GB",  1_024L * 1_024 * 1_024);

        private final String label;
        private final long   factor;

        Unit(String label, long factor) {
            this.label  = label;
            this.factor = factor;
        }

        public String getLabel() { return label; }
        public long   getFactor(){ return factor; }

        @Override public String toString() { return label; }
    }

    private Unit current = Unit.B;

    // ── Singleton-style shared instance ────────────────────────────────────
    // Controllers pass this one instance around so unit changes propagate
    // everywhere without static coupling.
    private static final UnitConverter INSTANCE = new UnitConverter();
    public static UnitConverter getInstance() { return INSTANCE; }

    /** Change the active unit. All subsequent format/convert calls use it. */
    public void setUnit(Unit u) { this.current = u; }

    public Unit getUnit() { return current; }

    // ── Conversion ─────────────────────────────────────────────────────────

    /** Format a raw byte count for display in the current unit. */
    public String format(long bytes) {
        if (current == Unit.B) return bytes + " B";
        double val = (double) bytes / current.getFactor();
        if (val == Math.floor(val)) return (long) val + " " + current.getLabel();
        return trimZeros(String.format("%.3f", val)) + " " + current.getLabel();
    }

    /** Same as format() but returns just the number string without the unit label. */
    public String formatValue(long bytes) {
        if (current == Unit.B) return String.valueOf(bytes);
        double val = (double) bytes / current.getFactor();
        if (val == Math.floor(val)) return String.valueOf((long) val);
        return trimZeros(String.format("%.4f", val));
    }

    /** Parse a user-entered value in the current unit → bytes. */
    public long toBytes(double userValue) {
        return Math.round(userValue * current.getFactor());
    }

    /** Convert bytes → current unit as a double (for pre-populating input fields). */
    public double fromBytes(long bytes) {
        return (double) bytes / current.getFactor();
    }

    /** Current unit label string, e.g. "KB". */
    public String label() { return current.getLabel(); }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static String trimZeros(String s) {
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
