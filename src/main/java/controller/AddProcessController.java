package controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import view.util.EditCell;
import javafx.stage.Stage;
import model.service.UnitConverter;

import java.util.*;

/**
 * Controller for the "Add / Edit Process" dialog (addProcess.fxml).
 *
 * SRP: only responsible for collecting process name + segment data.
 * Unit label is provided by the injected UnitConverter — no static coupling.
 */
public class AddProcessController {

    @FXML private TextField                     tfName;
    @FXML private TableView<String[]>           segTable;
    @FXML private TableColumn<String[], String> colSegName, colSegSize;

    private final ObservableList<String[]> rows = FXCollections.observableArrayList();
    private boolean      confirmed = false;
    private String       procName;
    private final List<String[]> segments = new ArrayList<>();

    // ── Injected by MainController ─────────────────────────────────────────
    private UnitConverter units;

    public void setUnitConverter(UnitConverter units) {
        this.units = units;
        updateLabels();
    }

    /** Pre-populate the dialog for editing an existing process. */
    public void initData(String name, List<String[]> existingSegments) {
        if (tfName != null) tfName.setText(name);
        rows.clear();
        for (String[] seg : existingSegments) rows.add(new String[]{seg[0], seg[1]});
    }

    @FXML
    public void initialize() {
        updateLabels();

        segTable.setItems(rows);
        segTable.setEditable(true);
        segTable.getSelectionModel().setCellSelectionEnabled(true);

        colSegName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[0]));
        colSegSize.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[1]));
        colSegName.setCellFactory(EditCell.forTableColumn());
        colSegSize.setCellFactory(EditCell.forTableColumn());
        colSegName.setOnEditCommit(e -> e.getRowValue()[0] = e.getNewValue());
        colSegSize.setOnEditCommit(e -> e.getRowValue()[1] = e.getNewValue());

        // Default rows only when table is empty (i.e., not pre-populated by initData)
        if (rows.isEmpty()) {
            rows.addAll(
                new String[]{"Code",  "50"},
                new String[]{"Data",  "200"},
                new String[]{"Stack", "100"}
            );
        }
    }

    private void updateLabels() {
        String lbl = units != null ? units.label() : "B";
        if (colSegSize != null) colSegSize.setText("Size (" + lbl + ")");
    }

    @FXML private void onAddRow()    { rows.add(new String[]{"Segment", "0"}); }
    @FXML private void onRemoveRow() {
        int sel = segTable.getSelectionModel().getSelectedIndex();
        if (sel >= 0) rows.remove(sel);
    }

    @FXML private void onAdd() {
        procName = tfName.getText().trim();
        if (procName.isEmpty()) { alert("Process name cannot be empty."); return; }
        if (rows.isEmpty())     { alert("Add at least one segment.");     return; }

        segments.clear();
        for (String[] row : rows) {
            String sn = row[0].trim(), ss = row[1].trim();
            if (sn.isEmpty() || ss.isEmpty()) { alert("Segment name/size cannot be empty."); return; }
            try {
                if (Integer.parseInt(ss) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                alert("Size of '" + sn + "' must be a positive integer."); return;
            }
            segments.add(new String[]{sn, ss});
        }
        confirmed = true;
        close();
    }

    @FXML private void onCancel() { close(); }

    private void close()  { ((Stage) tfName.getScene().getWindow()).close(); }
    private void alert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    public boolean       isConfirmed()  { return confirmed; }
    public String        getProcName()  { return procName; }
    public List<String[]> getSegments() { return segments; }
}
