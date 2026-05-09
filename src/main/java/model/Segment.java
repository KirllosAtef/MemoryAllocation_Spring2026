package model;

import javafx.beans.property.*;

public class Segment {
    private final StringProperty  name        = new SimpleStringProperty();
    private final IntegerProperty size        = new SimpleIntegerProperty();
    private final IntegerProperty baseAddress = new SimpleIntegerProperty(-1);
    private final BooleanProperty allocated   = new SimpleBooleanProperty(false);

    public Segment(String name, int size) {
        this.name.set(name);
        this.size.set(size);
    }

    public void allocate(int base) { baseAddress.set(base); allocated.set(true); }
    public void deallocate()       { baseAddress.set(-1);   allocated.set(false); }

    public int getEndAddress() { return isAllocated() ? getBaseAddress() + getSize() - 1 : -1; }

    // Property getters
    public StringProperty  nameProperty()        { return name; }
    public IntegerProperty sizeProperty()        { return size; }
    public IntegerProperty baseAddressProperty() { return baseAddress; }
    public BooleanProperty allocatedProperty()   { return allocated; }

    // Value getters
    public String  getName()        { return name.get(); }
    public int     getSize()        { return size.get(); }
    public int     getBaseAddress() { return baseAddress.get(); }
    public boolean isAllocated()    { return allocated.get(); }

    @Override public String toString() {
        return String.format("Segment{%s, size=%d, base=%d}", getName(), getSize(), getBaseAddress());
    }
}
