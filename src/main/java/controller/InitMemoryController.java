package controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.service.UnitConverter;
import model.service.UnitConverter.Unit;
import view.util.EditCell;
import java.util.*;

public class InitMemoryController {
    @FXML
    private Label lblTotalSize;
    @FXML
    private TextField tfTotalSize;
    @FXML
    private ComboBox<Unit> cbInputUnit;
    @FXML
    private TableView<String[]> holesTable;
    @FXML
    private TableColumn<String[], String> holeStart, holeSize;

    private final ObservableList<String[]> rows = FXCollections.observableArrayList();
    private boolean confirmed = false;
    private long totalSizeBytes = 0;
    private final List<long[]> holes = new ArrayList<>();

    private static long lastTotalBytes = 1024L;
    private static Unit lastInputUnit = Unit.B;
    private static final List<String[]> lastRows = new ArrayList<>();
    private static boolean hasInit = false;

    private UnitConverter units;

    public void setUnitConverter(UnitConverter units) {
        this.units = units;
        applyUnit();
    }

    @FXML
    public void initialize() {
        cbInputUnit.setItems(FXCollections.observableArrayList(Unit.values()));
        cbInputUnit.setValue(lastInputUnit);
        cbInputUnit.setOnAction(e -> applyUnit());

        holesTable.setItems(rows);
        holesTable.setEditable(true);
        holeStart.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[0]));
        holeSize.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[1]));
        holeStart.setCellFactory(EditCell.forTableColumn());
        holeSize.setCellFactory(EditCell.forTableColumn());
        holeStart.setOnEditCommit(e -> e.getRowValue()[0] = e.getNewValue());
        holeSize.setOnEditCommit(e -> e.getRowValue()[1] = e.getNewValue());

        if (hasInit) {
            tfTotalSize.setText(formatInUnit(lastTotalBytes, lastInputUnit));
            for (String[] r : lastRows)
                rows.add(new String[] { r[0], r[1] });
        } else {
            tfTotalSize.setText("1024");
            rows.add(new String[] { "0", "1024" });
        }
        applyUnit();
    }

    private void applyUnit() {
        Unit u = cbInputUnit.getValue();
        if (u == null)
            return;
        String lbl = u.getLabel();
        if (lblTotalSize != null)
            lblTotalSize.setText("Total Memory Size (" + lbl + "):");
        if (holeStart != null)
            holeStart.setText("Start (" + lbl + ")");
        if (holeSize != null)
            holeSize.setText("Size (" + lbl + ")");
    }

    @FXML
    private void onAddHoleRow() {
        rows.add(new String[] { "0", "0" });
    }

    @FXML
    private void onRemoveHoleRow() {
        int sel = holesTable.getSelectionModel().getSelectedIndex();
        if (sel >= 0)
            rows.remove(sel);
    }

    @FXML
    private void onOk() {
        Unit u = cbInputUnit.getValue();
        units.setInputUnit(u);
        try {
            long totalB = units.parse(tfTotalSize.getText());
            holes.clear();
            for (String[] row : rows) {
                long startB = (long) (Double.parseDouble(row[0].trim()) * u.getFactor());
                long sizeB = units.parse(row[1]);
                if (startB + sizeB > totalB)
                    throw new Exception("Hole exceeds memory.");
                holes.add(new long[] { startB, sizeB });
            }
            totalSizeBytes = totalB;
            lastTotalBytes = totalB;
            lastInputUnit = u;
            lastRows.clear();
            lastRows.addAll(rows);
            hasInit = true;
            confirmed = true;
            close();
        } catch (Exception ex) {
            alert(ex.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        ((Stage) tfTotalSize.getScene().getWindow()).close();
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private static String formatInUnit(long bytes, Unit u) {
        return (bytes % u.getFactor() == 0) ? String.valueOf(bytes / u.getFactor())
                : String.valueOf((double) bytes / u.getFactor());
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public List<long[]> getHoleBytes() {
        return holes;
    }
}