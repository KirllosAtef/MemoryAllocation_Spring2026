package model.service;

/**
 * Converts raw byte values to/from the user-selected units.
 */
public class UnitConverter {
    public enum Unit {
        B("B", 1L, 0),
        KB("KB", 1_024L, 0),   // Round down to whole KB as per "0 to 49KB"
        MB("MB", 1_024L * 1_024, 3), // Round down to 3 decimals as per "0 to 0.048"
        GB("GB", 1_024L * 1_024 * 1_024, 3);

        private final String label;
        private final long factor;
        private final int precision;

        Unit(String label, long factor, int precision) {
            this.label = label;
            this.factor = factor;
            this.precision = precision;
        }

        public String getLabel() { return label; }
        public long getFactor() { return factor; }
        public int getPrecision() { return precision; }

        @Override public String toString() { return label; }
    }

    private static final UnitConverter INSTANCE = new UnitConverter();
    public static UnitConverter getInstance() { return INSTANCE; }

    private Unit inputUnit = Unit.B;
    private Unit displayUnit = Unit.B;

    public void setInputUnit(Unit u) { this.inputUnit = u; }
    public Unit getInputUnit() { return inputUnit; }
    public String inputLabel() { return inputUnit.getLabel(); }

    public long parse(String raw) {
        double d;
        try {
            d = Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + raw + "\" is not a valid number.");
        }
        if (d <= 0) throw new IllegalArgumentException("Size must be positive.");

        long bytes = (long) (d * inputUnit.getFactor());
        if (bytes <= 0) throw new IllegalArgumentException("Value too small for selected unit.");
        return bytes;
    }

    public void setDisplayUnit(Unit u) { this.displayUnit = u; }
    public Unit getDisplayUnit() { return displayUnit; }
    public String label() { return displayUnit.getLabel(); }

    /**
     * Formats a size value (rounded to nearest precision).
     */
    public String format(long bytes) {
        double val = (double) bytes / displayUnit.getFactor();
        return trimZeros(formatWithPrecision(val, displayUnit.getPrecision())) + " " + displayUnit.getLabel();
    }

    /**
     * Formats an address (Start or End) - always rounds DOWN to the nearest precision step.
     */
    public String formatAddress(long bytes) {
        double val = (double) bytes / displayUnit.getFactor();
        int p = displayUnit.getPrecision();
        double factor = Math.pow(10, p);
        double roundedDown = Math.floor(val * factor) / factor;
        return trimZeros(formatWithPrecision(roundedDown, p));
    }

    public String formatValue(long bytes) {
        double val = (double) bytes / displayUnit.getFactor();
        return trimZeros(formatWithPrecision(val, displayUnit.getPrecision()));
    }

    private String formatWithPrecision(double val, int precision) {
        return String.format("%." + precision + "f", val);
    }

    private static String trimZeros(String s) {
        if (!s.contains(".")) return s;
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}