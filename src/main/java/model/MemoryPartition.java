package model;

import javafx.beans.property.*;

public class MemoryPartition implements Comparable<MemoryPartition> {

    public enum Type { FREE, ALLOCATED }

    private final IntegerProperty startAddress = new SimpleIntegerProperty();
    private final IntegerProperty size         = new SimpleIntegerProperty();
    private Type   type;
    private String processName;
    private String segmentName;

    /** Free hole constructor */
    public MemoryPartition(int start, int size) {
        this.startAddress.set(start);
        this.size.set(size);
        this.type = Type.FREE;
    }

    /** Allocated block constructor */
    public MemoryPartition(int start, int size, String proc, String seg) {
        this.startAddress.set(start);
        this.size.set(size);
        this.type        = Type.ALLOCATED;
        this.processName = proc;
        this.segmentName = seg;
    }

    // ── Mutators ──────────────────────────────────────────────────────────
    public void setStartAddress(int v) { startAddress.set(v); }
    public void setSize(int v)         { size.set(v); }

    // ── Properties ────────────────────────────────────────────────────────
    public IntegerProperty startAddressProperty() { return startAddress; }
    public IntegerProperty sizeProperty()         { return size; }

    // ── Values ────────────────────────────────────────────────────────────
    public int    getStartAddress() { return startAddress.get(); }
    public int    getSize()         { return size.get(); }
    public int    getEndAddress()   { return getStartAddress() + getSize() - 1; }
    public Type   getType()         { return type; }
    public boolean isFree()         { return type == Type.FREE; }
    public String getProcessName()  { return processName; }
    public String getSegmentName()  { return segmentName; }

    @Override public int compareTo(MemoryPartition o) {
        return Integer.compare(getStartAddress(), o.getStartAddress());
    }

    @Override public String toString() {
        return String.format("[%d-%d sz=%d %s]",
            getStartAddress(), getEndAddress(), getSize(),
            isFree() ? "FREE" : processName + ":" + segmentName);
    }
}
