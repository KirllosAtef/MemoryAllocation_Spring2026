package controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.service.UnitConverter;
import view.util.EditCell;
import java.util.*;

public class AddProcessController {
    @FXML
    private TextField tfName;
    @FXML
    private TableView<String[]> segTable;
    @FXML
    private TableColumn<String[], String> colSegName, colSegSize;

    private final ObservableList<String[]> rows = FXCollections.observableArrayList();
    private boolean confirmed = false;
    private String procName;
    private final List<String[]> segments = new ArrayList<>();
    private UnitConverter units;

    public void setUnitConverter(UnitConverter units) {
        this.units = units;
        updateSizeHeader();
    }

    public void initData(String name, List<String[]> existingSegments) {
        if (tfName != null)
            tfName.setText(name);
            
        if (existingSegments != null) {
            rows.clear();
            for (String[] seg : existingSegments) {
                String displaySize = seg[1];
                if (units != null) {
                    try {
                        long bytes = Long.parseLong(seg[1]);
                        displaySize = String.valueOf((double) bytes / units.getInputUnit().getFactor());
                    } catch (NumberFormatException ignored) {
                    }
                }
                rows.add(new String[] { seg[0], displaySize });
            }
        }
    }

    @FXML
    public void initialize() {
        segTable.setItems(rows);
        segTable.setEditable(true);
        colSegName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[0]));
        colSegSize.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[1]));
        colSegName.setCellFactory(EditCell.forTableColumn());
        colSegSize.setCellFactory(EditCell.forTableColumn());
        colSegName.setOnEditCommit(e -> e.getRowValue()[0] = e.getNewValue());
        colSegSize.setOnEditCommit(e -> e.getRowValue()[1] = e.getNewValue());

        if (rows.isEmpty()) {
            rows.addAll(new String[] { "Code", "50" }, new String[] { "Data", "200" }, new String[] { "Stack", "100" });
        }
    }

    private void updateSizeHeader() {
        if (colSegSize != null && units != null)
            colSegSize.setText("Size (" + units.inputLabel() + ")");
    }

    @FXML
    private void onAddRow() {
        rows.add(new String[] { "Segment", "0" });
    }

    @FXML
    private void onRemoveRow() {
        int sel = segTable.getSelectionModel().getSelectedIndex();
        if (sel >= 0)
            rows.remove(sel);
    }

    @FXML
    private void onAdd() {
        procName = tfName.getText().trim();
        if (procName.isEmpty() || rows.isEmpty()) {
            alert("Check process name and segments.");
            return;
        }
        segments.clear();
        try {
            for (String[] row : rows) {
                long bytes = units.parse(row[1]);
                segments.add(new String[] { row[0], String.valueOf(bytes) });
            }
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
        ((Stage) tfName.getScene().getWindow()).close();
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getProcName() {
        return procName;
    }

    public List<String[]> getSegments() {
        return segments;
    }
}