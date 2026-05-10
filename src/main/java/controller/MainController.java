package controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.*;
import model.*;
import model.algorithm.BestFitStrategy;
import model.algorithm.FirstFitStrategy;
import model.service.SystemLogger;
import model.service.UnitConverter;
import model.service.UnitConverter.Unit;
import view.MemoryDiagramBuilder;

import java.util.*;

/**
 * Controller for main.fxml — the application root.
 *
 * MVC responsibilities:
 *   - Owns the single UnitConverter instance and propagates it to dialogs/view.
 *   - Translates user actions (button clicks) into model calls.
 *   - Reads model state and refreshes view components.
 *   - Zero business/algorithm logic lives here.
 *
 * SOLID:
 *   - SRP: UI wiring only; algorithms stay in model.algorithm.*
 *   - OCP: new strategies plug in without touching this class.
 *   - DIP: depends on AllocationStrategy interface, not concrete classes directly.
 */
public class MainController {

    // ── FXML fields ────────────────────────────────────────────────────────
    @FXML private Label    statusLabel;
    @FXML private Pane     diagramPane;

    // Free-holes table
    @FXML private TableView<MemoryPartition>           freeTable;
    @FXML private TableColumn<MemoryPartition, Number> freeStart, freeEnd, freeSize;

    // Allocated table
    @FXML private TableView<MemoryPartition>           allocTable;
    @FXML private TableColumn<MemoryPartition, String> allocProc, allocSeg;
    @FXML private TableColumn<MemoryPartition, Number> allocStart, allocEnd, allocSize;

    // Segment table
    @FXML private ComboBox<String>             cbSegProcess;
    @FXML private TableView<Segment>           segTable;
    @FXML private TableColumn<Segment, Number> segNum, segBase, segSz, segLim;
    @FXML private TableColumn<Segment, String> segName;

    @FXML private RadioButton      rbFirstFit, rbBestFit;
    @FXML private ListView<String> processList;
    @FXML private TextArea         logArea;
    @FXML private ComboBox<Unit>   cbUnit;   // typed ComboBox<Unit> — no raw strings

    // ── Services & state ───────────────────────────────────────────────────
    /** Single shared converter instance. Injected into every dialog and the diagram. */
    private final UnitConverter units = UnitConverter.getInstance();

    private MemoryManager        mm;
    private MemoryDiagramBuilder diagram;

    /** Logger lambda — passed to model; keeps log writing in the controller. */
    private final SystemLogger logger = this::appendLog;

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
        // Diagram gets the shared UnitConverter — no static field needed
        diagram = new MemoryDiagramBuilder(diagramPane, units);

        setupFreeTable();
        setupAllocTable();
        setupSegTable();

        // Populate unit ComboBox with typed enum values
        cbUnit.setItems(FXCollections.observableArrayList(Unit.values()));
        cbUnit.setValue(units.getUnit());

        // Wire process list double-click → edit
        processList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) onEditProcess();
        });

        appendLog("Welcome! Click 'Initialise Memory' to begin.");
        appendLog("Then add processes and allocate them.");
    }

    // ── Unit changed ───────────────────────────────────────────────────────
    @FXML private void onUnitChanged() {
        units.setUnit(cbUnit.getValue());
        updateTableHeaders();
        refreshAll();
    }

    private void updateTableHeaders() {
        String lbl = units.label();
        freeSize .setText("Size (" + lbl + ")");
        allocSize.setText("Size (" + lbl + ")");
        segSz    .setText("Size (" + lbl + ")");
        if (mm != null)
            statusLabel.setText("● Memory: " + units.format(mm.getTotalMemory()));
    }

    // ── Table cell factories ───────────────────────────────────────────────
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

    // ── Action: Initialise Memory ──────────────────────────────────────────
    @FXML private void onInitMemory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/initMemory.fxml"));
            Parent root = loader.load();
            InitMemoryController ctrl = loader.getController();
            ctrl.setUnitConverter(units);   // <-- inject shared converter

            Stage dlg = dialogStage("Initialise Memory", root);
            dlg.showAndWait();
            if (!ctrl.isConfirmed()) return;

            MemoryManager oldMm = mm;
            mm = new MemoryManager(ctrl.getTotalSize());
            paletteIdx = 0;
            for (int[] h : ctrl.getHoles()) {
                try { mm.addInitialHole(h[0], h[1]); }
                catch (IllegalArgumentException ex) { showError(ex.getMessage()); return; }
            }

            // Preserve existing (unallocated) processes across re-init
            if (oldMm != null) {
                for (model.Process oldP : oldMm.getProcesses().values()) {
                    model.Process newP = new model.Process(oldP.getName());
                    newP.setColor(oldP.getColor());
                    for (Segment s : oldP.getSegments())
                        newP.addSegment(new Segment(s.getName(), s.getSize()));
                    mm.addProcess(newP);
                    paletteIdx++;
                }
            }

            refreshAll();
            statusLabel.setText("● Memory: " + units.format(mm.getTotalMemory()));
            statusLabel.getStyleClass().setAll("status-badge-ok");
            appendLog("──────────────────────────────");
            appendLog("Memory initialised: " + units.format(mm.getTotalMemory())
                    + ", " + mm.getFreePartitions().size() + " hole(s).");
        } catch (Exception ex) { showError(ex.getMessage()); ex.printStackTrace(); }
    }

    // ── Action: Add Process ────────────────────────────────────────────────
    @FXML private void onAddProcess() {
        if (mm == null) { showError("Initialise memory first."); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/addProcess.fxml"));
            Parent root = loader.load();
            AddProcessController ctrl = loader.getController();
            ctrl.setUnitConverter(units);   // <-- inject

            Stage dlg = dialogStage("Add Process", root);
            dlg.showAndWait();
            if (!ctrl.isConfirmed()) return;

            String name = ctrl.getProcName();
            if (mm.processExists(name)) { showError("Process '" + name + "' already exists."); return; }

            model.Process p = buildProcess(name, ctrl.getSegments());
            mm.addProcess(p);

            refreshProcessList();
            refreshSegProcessCombo();
            appendLog("Process '" + name + "' added ("
                    + p.getSegments().size() + " segments, "
                    + units.format(p.totalSize()) + ").");
        } catch (Exception ex) { showError(ex.getMessage()); ex.printStackTrace(); }
    }

    // ── Action: Edit Process ───────────────────────────────────────────────
    @FXML private void onEditProcess() {
        if (mm == null) return;
        String sel = processList.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String name = sel.split(" ")[0];
        model.Process p = mm.getProcesses().get(name);
        if (p == null) return;
        if (p.isAllocated()) { showError("Cannot edit an allocated process. Deallocate it first."); return; }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/addProcess.fxml"));
            Parent root = loader.load();
            AddProcessController ctrl = loader.getController();
            ctrl.setUnitConverter(units);

            List<String[]> segs = new ArrayList<>();
            for (Segment s : p.getSegments()) segs.add(new String[]{s.getName(), String.valueOf(s.getSize())});
            ctrl.initData(p.getName(), segs);

            Stage dlg = dialogStage("Edit Process", root);
            dlg.showAndWait();
            if (!ctrl.isConfirmed()) return;

            String newName = ctrl.getProcName();
            if (!newName.equals(p.getName()) && mm.processExists(newName)) {
                showError("A process with the new name already exists."); return;
            }

            mm.removeProcess(p.getName());
            model.Process newP = buildProcess(newName, ctrl.getSegments());
            newP.setColor(p.getColor());
            mm.addProcess(newP);

            refreshProcessList();
            refreshSegProcessCombo();
            appendLog("Process '" + p.getName() + "' edited"
                    + (newName.equals(p.getName()) ? "." : " → '" + newName + "'."));
        } catch (Exception ex) { showError(ex.getMessage()); ex.printStackTrace(); }
    }

    // ── Action: Delete Process ─────────────────────────────────────────────
    @FXML private void onDeleteProcess() {
        if (mm == null) return;
        String sel = processList.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String name = sel.split(" ")[0];
        model.Process p = mm.getProcesses().get(name);
        if (p == null) return;
        if (p.isAllocated()) { showError("Cannot delete an allocated process. Deallocate it first."); return; }
        mm.removeProcess(name);
        refreshProcessList();
        refreshSegProcessCombo();
        appendLog("Process '" + name + "' deleted.");
    }

    // ── Action: Allocate ───────────────────────────────────────────────────
    @FXML private void onAllocate() {
        String sel = processList.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Select a process first."); return; }
        String name = sel.split(" ")[0];
        mm.setStrategy(rbBestFit.isSelected() ? new BestFitStrategy() : new FirstFitStrategy());
        mm.allocate(name, logger);
        refreshAll();
    }

    // ── Action: Deallocate ─────────────────────────────────────────────────
    @FXML private void onDeallocate() {
        String sel = processList.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Select a process first."); return; }
        String name = sel.split(" ")[0];
        mm.deallocate(name, logger);
        refreshAll();
    }

    // ── Action: Show Segment Table ─────────────────────────────────────────
    @FXML private void onSegProcessSelected() { /* handled by show button */ }

    @FXML private void onShowSegTable() {
        if (mm == null) return;
        String sel = cbSegProcess.getValue();
        if (sel == null) return;
        model.Process p = mm.getProcesses().get(sel);
        if (p != null) segTable.setItems(p.getSegments());
    }

    // ── Action: Zoom ───────────────────────────────────────────────────────
    @FXML private void onZoomIn()  { diagram.zoomIn();  diagram.redraw(mm); }
    @FXML private void onZoomOut() { diagram.zoomOut(); diagram.redraw(mm); }

    // ── Action: Clear log ──────────────────────────────────────────────────
    @FXML private void onClearLog() { logArea.clear(); }

    // ── Refresh helpers ────────────────────────────────────────────────────
    private void refreshAll() {
        if (mm == null) return;
        freeTable .setItems(mm.getFreePartitions());
        allocTable.setItems(mm.getAllocPartitions());
        updateTableHeaders();
        diagram.redraw(mm);
        refreshProcessList();
        refreshSegProcessCombo();
    }

    private void refreshProcessList() {
        if (mm == null) return;
        ObservableList<String> items = FXCollections.observableArrayList();
        for (model.Process p : mm.getProcesses().values())
            items.add(p.getName() + " [" + p.getState().name() + "]");
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
    private model.Process buildProcess(String name, List<String[]> segsData) {
        model.Process p = new model.Process(name);
        p.setColor(PALETTE[paletteIdx % PALETTE.length]);
        paletteIdx++;
        for (String[] sd : segsData)
            p.addSegment(new Segment(sd[0], Integer.parseInt(sd[1])));
        return p;
    }

    private Stage dialogStage(String title, Parent root) {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle(title);
        dlg.setScene(new Scene(root));
        dlg.setResizable(false);
        return dlg;
    }

    private void appendLog(String msg) { logArea.appendText(msg + "\n"); }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
