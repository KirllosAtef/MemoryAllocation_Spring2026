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
 * Controller for the "Initialise Memory" dialog (initMemory.fxml).
 *
 * SRP: only responsible for collecting total-size + hole data from the user.
 * Unit display is delegated to the injected UnitConverter — no reference to
 * MainController or any static field.
 */
public class InitMemoryController {

    @FXML private TextField                     tfTotalSize;
    @FXML private Label                         lblTotalSize;
    @FXML private ComboBox<UnitConverter.Unit>  cbUnit;
    @FXML private TableView<String[]>           holesTable;
    @FXML private TableColumn<String[], String> holeStart, holeSize;

    private final ObservableList<String[]> rows = FXCollections.observableArrayList();
    private boolean confirmed = false;
    private int     totalSize;
    private final   List<int[]> holes = new ArrayList<>();

    // Persist values across re-opens (session-scoped, not application-global)
    private static double               lastTotalSize = 1024;
    private static UnitConverter.Unit   lastUnit      = UnitConverter.Unit.B;
    private static final List<String[]> lastRows      = new ArrayList<>();
    private static boolean              hasInitialized = false;

    // ── Injected by MainController before the dialog is shown ─────────────
    private UnitConverter units;

    /** Called by MainController to inject the shared converter before show(). */
    public void setUnitConverter(UnitConverter units) {
        this.units = units;
        // Defensive: if units was set after initialize(), update labels again
        updateLabels();
    }

    @FXML
    public void initialize() {
        cbUnit.setItems(FXCollections.observableArrayList(UnitConverter.Unit.values()));
        cbUnit.setValue(lastUnit);

        updateLabels();

        holesTable.setItems(rows);
        holesTable.setEditable(true);
        holesTable.getSelectionModel().setCellSelectionEnabled(true);

        holeStart.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[0]));
        holeSize .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[1]));
        holeStart.setCellFactory(EditCell.forTableColumn());
        holeSize .setCellFactory(EditCell.forTableColumn());
        holeStart.setOnEditCommit(e -> e.getRowValue()[0] = e.getNewValue());
        holeSize .setOnEditCommit(e -> e.getRowValue()[1] = e.getNewValue());

        // Handle possible trailing .0 for cleaner display
        String totalText = String.valueOf(lastTotalSize);
        if (totalText.endsWith(".0")) totalText = totalText.substring(0, totalText.length() - 2);
        tfTotalSize.setText(totalText);

        if (hasInitialized) {
            for (String[] r : lastRows) rows.add(new String[]{r[0], r[1]});
        } else {
            rows.add(new String[]{"0", "1024"});
        }
    }

    @FXML private void onUnitChanged() {
        updateLabels();
    }

    private void updateLabels() {
        String lbl = cbUnit != null && cbUnit.getValue() != null ? cbUnit.getValue().getLabel() : "B";
        if (lblTotalSize != null) lblTotalSize.setText("Total Memory Size (" + lbl + "):");
        if (holeSize      != null) holeSize.setText("Size (" + lbl + ")");
        if (holeStart     != null) holeStart.setText("Start Address (" + lbl + ")");
    }

    @FXML private void onAddHoleRow()    { rows.add(new String[]{"0", "0"}); }
    @FXML private void onRemoveHoleRow() {
        int sel = holesTable.getSelectionModel().getSelectedIndex();
        if (sel >= 0) rows.remove(sel);
    }

    @FXML private void onOk() {
        UnitConverter.Unit u = cbUnit.getValue();
        long factor = u.getFactor();

        try {
            double val = Double.parseDouble(tfTotalSize.getText().trim());
            if (val <= 0) throw new NumberFormatException();
            totalSize = (int) Math.round(val * factor);
            lastTotalSize = val;
        } catch (NumberFormatException ex) {
            alert("Invalid total memory size. Enter a positive number."); return;
        }

        holes.clear();
        for (String[] row : rows) {
            try {
                double startVal = Double.parseDouble(row[0].trim());
                double sizeVal  = Double.parseDouble(row[1].trim());
                if (sizeVal <= 0 || startVal < 0) throw new NumberFormatException();
                
                int start = (int) Math.round(startVal * factor);
                int size  = (int) Math.round(sizeVal * factor);
                holes.add(new int[]{start, size});
            } catch (NumberFormatException ex) {
                alert("Invalid hole entry: [" + row[0] + ", " + row[1] + "]. Use positive numbers.");
                return;
            }
        }
        if (holes.isEmpty()) holes.add(new int[]{0, totalSize});

        // Save unit used and rows exactly as entered
        lastUnit = u;
        lastRows.clear();
        for (String[] row : rows) lastRows.add(new String[]{row[0], row[1]});
        hasInitialized = true;

        confirmed = true;
        close();
    }

    @FXML private void onCancel() { close(); }

    private void close()  { ((Stage) tfTotalSize.getScene().getWindow()).close(); }
    private void alert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    public boolean     isConfirmed() { return confirmed; }
    public int         getTotalSize(){ return totalSize; }
    public List<int[]> getHoles()    { return holes; }
}
