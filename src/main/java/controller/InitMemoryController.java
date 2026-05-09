package controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import util.EditCell;
import javafx.stage.Stage;
import java.util.*;

public class InitMemoryController {

    @FXML private TextField tfTotalSize;
    @FXML private Label lblTotalSize;
    @FXML private TableView<String[]>   holesTable;
    @FXML private TableColumn<String[], String> holeStart, holeSize;

    private final ObservableList<String[]> rows = FXCollections.observableArrayList();
    private boolean confirmed = false;
    private int totalSize;
    private final List<int[]> holes = new ArrayList<>();

    // Keep state across instances
    private static int lastTotalSize = 1024;
    private static final List<String[]> lastRows = new ArrayList<>();
    private static boolean hasInitialized = false;

    @FXML
    public void initialize() {
        lblTotalSize.setText("Total Memory Size (" + MainController.CURRENT_UNIT + "):");
        holeSize.setText("Size (" + MainController.CURRENT_UNIT + ")");
        holesTable.setItems(rows);
        holesTable.setEditable(true);

        holesTable.getSelectionModel().setCellSelectionEnabled(true);

        holeStart.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[0]));
        holeSize .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[1]));

        holeStart.setCellFactory(util.EditCell.forTableColumn());
        holeSize .setCellFactory(util.EditCell.forTableColumn());

        holeStart.setOnEditCommit(e -> e.getRowValue()[0] = e.getNewValue());
        holeSize .setOnEditCommit(e -> e.getRowValue()[1] = e.getNewValue());

        tfTotalSize.setText(String.valueOf(lastTotalSize));
        if (hasInitialized) {
            for (String[] r : lastRows) {
                rows.add(new String[]{r[0], r[1]});
            }
        } else {
            // Default example hole
            rows.add(new String[]{"0", "1024"});
        }
    }


    @FXML private void onAddHoleRow()    { rows.add(new String[]{"0", "0"}); }
    @FXML private void onRemoveHoleRow() {
        int sel = holesTable.getSelectionModel().getSelectedIndex();
        if (sel >= 0) rows.remove(sel);
    }

    @FXML private void onOk() {
        try {
            totalSize = Integer.parseInt(tfTotalSize.getText().trim());
            if (totalSize <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            alert("Invalid total memory size. Enter a positive integer.");
            return;
        }
        holes.clear();
        for (String[] row : rows) {
            try {
                int start = Integer.parseInt(row[0].trim());
                int size  = Integer.parseInt(row[1].trim());
                if (size <= 0) throw new NumberFormatException();
                holes.add(new int[]{start, size});
            } catch (NumberFormatException ex) {
                alert("Invalid hole entry: [" + row[0] + ", " + row[1] + "]. Use positive integers.");
                return;
            }
        }
        if (holes.isEmpty()) holes.add(new int[]{0, totalSize});
        
        lastTotalSize = totalSize;
        lastRows.clear();
        for (String[] row : rows) {
            lastRows.add(new String[]{row[0], row[1]});
        }
        hasInitialized = true;
        
        confirmed = true;
        close();
    }

    @FXML private void onCancel() { close(); }

    private void close()  { ((Stage) tfTotalSize.getScene().getWindow()).close(); }
    private void alert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    public boolean    isConfirmed() { return confirmed; }
    public int        getTotalSize(){ return totalSize; }
    public List<int[]> getHoles()   { return holes; }
}
