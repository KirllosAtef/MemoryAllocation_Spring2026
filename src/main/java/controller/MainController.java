package controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.*;
import model.*;
import model.MemoryManager.Algorithm;

import java.net.URL;
import java.util.*;

public class MainController {

    // ── FXML fields ────────────────────────────────────────────────────────
    @FXML private Label    statusLabel;
    @FXML private Pane     diagramPane;
    @FXML private Label    lblFree, lblUsed, lblTotal;

    // Free-holes table
    @FXML private TableView<MemoryPartition>          freeTable;
    @FXML private TableColumn<MemoryPartition, Number> freeStart, freeEnd, freeSize;

    // Allocated table
    @FXML private TableView<MemoryPartition>           allocTable;
    @FXML private TableColumn<MemoryPartition, String> allocProc, allocSeg;
    @FXML private TableColumn<MemoryPartition, Number> allocStart, allocEnd, allocSize;

    // Segment table
    @FXML private ComboBox<String>                    cbSegProcess;
    @FXML private TableView<Segment>                  segTable;
    @FXML private TableColumn<Segment, Number>        segNum, segBase, segSz, segLim;
    @FXML private TableColumn<Segment, String>        segName;

    @FXML private RadioButton rbFirstFit, rbBestFit;
    @FXML private ListView<String> processList;
    @FXML private TextArea logArea;
    @FXML private TabPane  tabPane;
    @FXML private ComboBox<String> cbUnit;

    public static String CURRENT_UNIT = "B";

    // ── State ──────────────────────────────────────────────────────────────
    private MemoryManager mm;
    private MemoryDiagramBuilder diagram;

    private static final Color[] PALETTE = {
        Color.web("#FF6B6B"), Color.web("#4ECDC4"), Color.web("#45B7D1"),
        Color.web("#96CEB4"), Color.web("#FECA57"), Color.web("#FF9FF3"),
        Color.web("#54A0FF"), Color.web("#5F27CD"), Color.web("#00D2D3"),
        Color.web("#FF9F43"), Color.web("#EE5A24"), Color.web("#009432"),
    };
    private int paletteIdx = 0;

    // ── Initialize ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        diagram = new MemoryDiagramBuilder(diagramPane);
        setupFreeTable();
        setupAllocTable();
        setupSegTable();

        cbUnit.setItems(FXCollections.observableArrayList("B", "KB", "MB", "GB"));
        cbUnit.setValue(CURRENT_UNIT);
        updateTableHeaders();

        appendLog("Welcome! Click 'Initialise Memory' to begin.");
        appendLog("Then add processes and allocate them.");
    }

    @FXML private void onUnitChanged() {
        CURRENT_UNIT = cbUnit.getValue();
        updateTableHeaders();
        refreshAll();
    }

    private void updateTableHeaders() {
        freeSize.setText("Size (" + CURRENT_UNIT + ")");
        allocSize.setText("Size (" + CURRENT_UNIT + ")");
        segSz.setText("Size (" + CURRENT_UNIT + ")");
        if (mm != null) {
            statusLabel.setText("● Memory: " + mm.getTotalMemory() + " " + CURRENT_UNIT);
        }
    }

    // ── Table setup ────────────────────────────────────────────────────────
    private void setupFreeTable() {
        freeStart.setCellValueFactory(cd -> cd.getValue().startAddressProperty());
        freeEnd  .setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getEndAddress()));
        freeSize .setCellValueFactory(cd -> cd.getValue().sizeProperty());
    }

    private void setupAllocTable() {
        allocProc .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getProcessName()));
        allocSeg  .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getSegmentName()));
        allocStart.setCellValueFactory(cd -> cd.getValue().startAddressProperty());
        allocEnd  .setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getEndAddress()));
        allocSize .setCellValueFactory(cd -> cd.getValue().sizeProperty());
    }

    private void setupSegTable() {
        segNum .setCellValueFactory(cd -> new SimpleIntegerProperty(
                    segTable.getItems().indexOf(cd.getValue())));
        segName.setCellValueFactory(cd -> cd.getValue().nameProperty());
        segBase.setCellValueFactory(cd -> cd.getValue().baseAddressProperty());
        segSz  .setCellValueFactory(cd -> cd.getValue().sizeProperty());
        segLim .setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getEndAddress()));

        // Show "—" for unallocated segments
        segBase.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item.intValue() == -1 ? "—" : item.toString());
            }
        });
        segLim.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item.intValue() == -1 ? "—" : item.toString());
            }
        });
    }

    // ── Action: Initialise ─────────────────────────────────────────────────
    @FXML private void onInitMemory() {
        try {
            URL fxmlUrl = getClass().getResource("/fxml/initMemory.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            InitMemoryController ctrl = loader.getController();

            Stage dlg = new Stage();
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("Initialise Memory");
            dlg.setScene(new Scene(root));
            dlg.setResizable(false);
            dlg.showAndWait();

            if (!ctrl.isConfirmed()) return;

            mm = new MemoryManager(ctrl.getTotalSize());
            paletteIdx = 0;
            for (int[] h : ctrl.getHoles()) {
                try { mm.addInitialHole(h[0], h[1]); }
                catch (IllegalArgumentException ex) { showError(ex.getMessage()); return; }
            }

            refreshAll();
            statusLabel.setText("● Memory: " + mm.getTotalMemory() + " " + CURRENT_UNIT);
            statusLabel.getStyleClass().setAll("status-badge-ok");
            appendLog("──────────────────────────────");
            appendLog("Memory initialised: " + mm.getTotalMemory() + " " + CURRENT_UNIT + ", "
                    + mm.getFreePartitions().size() + " hole(s).");
        } catch (Exception ex) { showError(ex.getMessage()); ex.printStackTrace(); }
    }

    // ── Action: Add Process ────────────────────────────────────────────────
    @FXML private void onAddProcess() {
        if (mm == null) { showError("Initialise memory first."); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/addProcess.fxml"));
            Parent root = loader.load();
            AddProcessController ctrl = loader.getController();

            Stage dlg = new Stage();
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("Add Process");
            dlg.setScene(new Scene(root));
            dlg.setResizable(false);
            dlg.showAndWait();

            if (!ctrl.isConfirmed()) return;
            String name = ctrl.getProcName();
        // Temporarily reset items to force full redraw of columns
        ObservableList<MemoryPartition> fList = freeTable.getItems();
        freeTable.setItems(FXCollections.emptyObservableList());
        freeTable.setItems(fList);

        ObservableList<MemoryPartition> aList = allocTable.getItems();
        allocTable.setItems(FXCollections.emptyObservableList());
        allocTable.setItems(aList);

        ObservableList<Segment> sList = segTable.getItems();
        segTable.setItems(FXCollections.emptyObservableList());
        segTable.setItems(sList);

            model.Process p = new model.Process(name);
            p.setColor(PALETTE[paletteIdx % PALETTE.length]);
            paletteIdx++;
            for (String[] sd : ctrl.getSegments())
                p.addSegment(new Segment(sd[0], Integer.parseInt(sd[1])));
            mm.addProcess(p);

            refreshProcessList();
            refreshSegProcessCombo();
            appendLog("Process '" + name + "' added ("
                    + p.getSegments().size() + " segments, " + p.totalSize() + " " + CURRENT_UNIT + ").");
        } catch (Exception ex) { showError(ex.getMessage()); ex.printStackTrace(); }
    }

    // ── Action: Allocate ───────────────────────────────────────────────────
    @FXML private void onAllocate() {
        String sel = processList.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Select a process first."); return; }
        String name = sel.split(" ")[0];

        mm.setAlgorithm(rbBestFit.isSelected() ? Algorithm.BEST_FIT : Algorithm.FIRST_FIT);
        mm.allocate(name).forEach(this::appendLog);
        refreshAll();
    }

    // ── Action: Deallocate ─────────────────────────────────────────────────
    @FXML private void onDeallocate() {
        String sel = processList.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Select a process first."); return; }
        String name = sel.split(" ")[0];
        mm.deallocate(name).forEach(this::appendLog);
        refreshAll();
    }

    // ── Action: Segment table combo ────────────────────────────────────────
    @FXML private void onSegProcessSelected() { /* handled by show button */ }

    @FXML private void onShowSegTable() {
        if (mm == null) return;
        String sel = cbSegProcess.getValue();
        if (sel == null) return;
        model.Process p = mm.getProcesses().get(sel);
        if (p != null) {
            segTable.setItems(p.getSegments());
            tabPane.getSelectionModel().select(2);
        }
    }

    // ── Action: Clear log ──────────────────────────────────────────────────
    @FXML private void onClearLog() { logArea.clear(); }

    // ── Refresh helpers ────────────────────────────────────────────────────
    private void refreshAll() {
        if (mm == null) return;
        freeTable .setItems(mm.getFreePartitions());
        allocTable.setItems(mm.getAllocPartitions());
        lblFree .setText("Free: "  + mm.getTotalFree()      + " " + CURRENT_UNIT);
        lblUsed .setText("Used: "  + mm.getTotalAllocated() + " " + CURRENT_UNIT);
        lblTotal.setText("Total: " + mm.getTotalMemory()    + " " + CURRENT_UNIT);
        diagram.redraw(mm);
        refreshProcessList();
        refreshSegProcessCombo();
    }

    private void refreshProcessList() {
        if (mm == null) return;
        ObservableList<String> items = FXCollections.observableArrayList();
        for (model.Process p : mm.getProcesses().values())
            items.add(p.getName() + (p.isAllocated() ? " [ALLOCATED]" : " [PENDING]"));
        processList.setItems(items);
    }

    private void refreshSegProcessCombo() {
        if (mm == null) return;
        String prev = cbSegProcess.getValue();
        cbSegProcess.setItems(FXCollections.observableArrayList(mm.getProcesses().keySet()));
        if (prev != null && mm.getProcesses().containsKey(prev))
            cbSegProcess.setValue(prev);
    }

    // ── Utilities ──────────────────────────────────────────────────────────
    private void appendLog(String msg) {
        logArea.appendText(msg + "\n");
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
