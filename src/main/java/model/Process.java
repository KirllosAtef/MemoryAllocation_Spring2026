package model;

import javafx.beans.property.*;
import javafx.collections.*;
import javafx.scene.paint.Color;
import java.util.List;

public class Process {
    private final StringProperty           name      = new SimpleStringProperty();
    private final BooleanProperty          allocated = new SimpleBooleanProperty(false);
    private final ObservableList<Segment>  segments  = FXCollections.observableArrayList();
    private Color color = Color.CORNFLOWERBLUE;

    public Process(String name) { this.name.set(name); }

    public void addSegment(Segment s) { segments.add(s); }

    public void deallocateAll() {
        segments.forEach(Segment::deallocate);
        allocated.set(false);
    }

    public int totalSize() { return segments.stream().mapToInt(Segment::getSize).sum(); }

    // Properties
    public StringProperty  nameProperty()      { return name; }
    public BooleanProperty allocatedProperty() { return allocated; }

    // Values
    public String                   getName()      { return name.get(); }
    public boolean                  isAllocated()  { return allocated.get(); }
    public void                     setAllocated(boolean b) { allocated.set(b); }
    public ObservableList<Segment>  getSegments()  { return segments; }
    public Color                    getColor()     { return color; }
    public void                     setColor(Color c) { color = c; }

    /** CSS hex string for use in FXML/CSS */
    public String getColorHex() {
        return String.format("#%02X%02X%02X",
            (int)(color.getRed()*255),
            (int)(color.getGreen()*255),
            (int)(color.getBlue()*255));
    }

    @Override public String toString() { return getName(); }
}
