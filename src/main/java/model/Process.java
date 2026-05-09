package model;

import javafx.beans.property.*;
import javafx.collections.*;
import javafx.scene.paint.Color;

public class Process {
    private final StringProperty name = new SimpleStringProperty();
    public enum State { PENDING, ALLOCATED, DEALLOCATED, FAILED }
    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.PENDING);
    private final ObservableList<Segment> segments = FXCollections.observableArrayList();
    private Color color = Color.CORNFLOWERBLUE;

    public Process(String name) {
        this.name.set(name);
    }

    public void addSegment(Segment s) {
        segments.add(s);
    }

    public void deallocateAll() {
        segments.forEach(Segment::deallocate);
        state.set(State.DEALLOCATED);
    }

    public int totalSize() {
        return segments.stream().mapToInt(Segment::getSize).sum();
    }

    // Properties
    public StringProperty nameProperty() {
        return name;
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    // Values
    public String getName() {
        return name.get();
    }

    public boolean isAllocated() {
        return state.get() == State.ALLOCATED;
    }

    public State getState() {
        return state.get();
    }

    public void setAllocated(boolean b) {
        state.set(b ? State.ALLOCATED : State.DEALLOCATED);
    }

    public void setState(State s) {
        state.set(s);
    }

    public ObservableList<Segment> getSegments() {
        return segments;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color c) {
        color = c;
    }

    /** CSS hex string for use in FXML/CSS */
    public String getColorHex() {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    @Override
    public String toString() {
        return getName();
    }
}
