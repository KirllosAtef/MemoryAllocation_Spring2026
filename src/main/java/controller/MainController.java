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
 * Controller for main.fxml.
 *
 * Owns the single UnitConverter instance.
 * Bridges UI events → model calls.
 * Converts model byte values → display strings via UnitConverter.displayUnit.
 * Passes raw bytes (from dialog controllers) straight to MemoryManager.
 */
public class MainController {

    // ── FXML ──────────────────────────────────────────────────────────────
    @FXML private Label    statusLabel;
    @FXML private Pane     diagramPane;

    @FXML private TableView<MemoryPartition>           freeTable;
    @FXML private TableColumn<MemoryPartition, Number> freeStart, freeEnd, freeSize;

    @FXML private TableView<MemoryPartition>           allocTable;
    @FXML private TableColumn<MemoryPartition, String> allocProc, allocSeg;
    @FXML private TableColumn<MemoryPartition, Number> allocStart, allocEnd, allocSize;

    @FXML private ComboBox<String>             cbSegProcess;
    @FXML private TableView<Segment>           segTable;
    @FXML private TableColumn<Segment, Number> segNum, segBase, segSz, segLim;
    @FXML private TableColumn<Segment, String> segName;

    @FXML private RadioButton      rbFirstFit, rbBestFit;
    @FXML private ListView<String> processList;
    @FXML private TextArea         logArea;
    @FXML private ComboBox<Unit>   cbUnit;   // display unit — independent of input unit

    // ── Services ──────────────────────────────────────────────────────────
    private final UnitConverter  units  = UnitConverter.getInstance();
    private       MemoryManager  mm;
    private       MemoryDiagramBuilder diagram;
    private final SystemLogger   logger = this::appendLog;

    private static final Color[] PALETTE = {
        Color.web("#FF6B6B"), Color.web("#4ECDC4"), Color.web("#45B7D1"),
        Color.web("#96CEB4"), Color.web("#FECA57"), Color.web("#FF9FF3"),
        Color.web("#54A0FF"), Color.web("#5F27CD"), Color.web("#00D2D3"),
        Color.web("#FF9F43"), Color.web("#EE5A24"), Color.web("#009432"),
    };
    private int paletteIdx = 0;
    private int processCounter = 1;

    // ── Initialize ────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        diagram = new MemoryDiagramBuilder(diagramPane, units);

        setupFreeTable();
        setupAllocTable();
        setupSegTable();

        cbUnit.setItems(FXCollections.observableArrayList(Unit.values()));
        cbUnit.setValue(units.getDisplayUnit());

        processList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) onEditProcess();
        });

        appendLog("Welcome! Click 'Initialise Memory' to begin.");
        appendLog("Then add processes and allocate them.");
    }

    // ── Display unit changed (main ComboBox) ───────────────────────────────
    @FXML private void onUnitChanged() {
        units.setDisplayUnit(cbUnit.getValue());   // only display; input unit untouched
        updateTableHeaders();
        refreshAll();
    }

    private void updateTableHeaders() {
        String lbl = units.label();                // displayUnit label
        freeSize .setText("Size (" + lbl + ")");
        allocSize.setText("Size (" + lbl + ")");
        segSz    .setText("Size (" + lbl + ")");
        if (mm != null)
            statusLabel.setText("● Memory: " + units.format(mm.getTotalMemory()));
    }

    // ── Table cell factories ──────────────────────────────────────────────
    private void setupFreeTable() {
        freeStart.setCellValueFactory(cd -> cd.getValue().startAddressProperty());
        freeEnd  .setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getEndAddress()));
        freeSize .setCellValueFactory(cd -> cd.getValue().sizeProperty());

        freeStart.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : units.formatAddress(item.longValue()));
            }
        });
        freeEnd.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : units.formatAddress(item.longValue()));
            }
        });
        freeSize.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : units.format(item.longValue()));
            }
        });
    }

    private void setupAllocTable() {
        allocProc .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getProcessName()));
        allocSeg  .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getSegmentName()));
        allocStart.setCellValueFactory(cd -> cd.getValue().startAddressProperty());
        allocEnd  .setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getEndAddress()));
        allocSize .setCellValueFactory(cd -> cd.getValue().sizeProperty());

        allocStart.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : units.formatAddress(item.longValue()));
            }
        });
        allocEnd.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : units.formatAddress(item.longValue()));
            }
        });
        allocSize.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : units.format(item.longValue()));
            }
        });
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
                setText(item.intValue() == -1 ? "—" : units.formatAddress(item.longValue()));
            }
        });
        segLim.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item.intValue() == -1 ? "—" : units.formatAddress(item.longValue()));
            }
        });
        segSz.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : units.format(item.longValue()));
            }
        });
    }

    // ── Action: Initialise Memory ──────────────────────────────────────────
    @FXML private void onInitMemory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/initMemory.fxml"));
            Parent root = loader.load();
            InitMemoryController ctrl = loader.getController();
            ctrl.setUnitConverter(units);

            Stage dlg = dialogStage("Initialise Memory", root);
            dlg.showAndWait();
            if (!ctrl.isConfirmed()) return;

            // Dialog returns raw bytes — no further conversion needed
            long totalBytes = ctrl.getTotalSizeBytes();
            if (totalBytes > Integer.MAX_VALUE) {
                showError("Memory size exceeds " + Integer.MAX_VALUE + " bytes. Use a smaller value.");
                return;
            }

            MemoryManager oldMm = mm;
            mm = new MemoryManager((int) totalBytes);
            paletteIdx = 0;

            for (long[] h : ctrl.getHoleBytes()) {
                if (h[0] + h[1] > totalBytes) {
                    showError("Hole starting at " + h[0] + " B with size " + h[1]
                            + " B exceeds total memory.");
                    return;
                }
                mm.addInitialHole((int) h[0], (int) h[1]);
            }

            // Preserve all processes across re-init (reset to PENDING)
            if (oldMm != null) {
                int maxIdx = 0;
                for (model.Process oldP : oldMm.getProcesses().values()) {
                    model.Process newP = new model.Process(oldP.getName());
                    newP.setColor(oldP.getColor());
                    for (Segment s : oldP.getSegments())
                        newP.addSegment(new Segment(s.getName(), s.getSize()));
                    mm.addProcess(newP);
                    paletteIdx++;

                    // Update name counter
                    try {
                        if (oldP.getName().startsWith("P")) {
                            int idx = Integer.parseInt(oldP.getName().substring(1));
                            maxIdx = Math.max(maxIdx, idx);
                        }
                    } catch (Exception ignored) {}
                }
                processCounter = maxIdx + 1;
            }

            refreshAll();
            statusLabel.setText("● Memory: " + units.format(mm.getTotalMemory()));
            statusLabel.getStyleClass().setAll("status-badge-ok");
            appendLog("──────────────────────────────");
            appendLog("Memory initialised: " + units.format(mm.getTotalMemory())
                    + "  [input unit: " + units.getInputUnit().getLabel()
                    + "  display unit: " + units.label() + "]");
            appendLog(mm.getFreePartitions().size() + " initial hole(s).");
        } catch (Exception ex) { showError(ex.getMessage()); ex.printStackTrace(); }
    }

    // ── Action: Add Process ───────────────────────────────────────────────
    @FXML private void onAddProcess() {
        if (mm == null) { showError("Initialise memory first."); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/addProcess.fxml"));
            Parent root = loader.load();
            AddProcessController ctrl = loader.getController();
            ctrl.setUnitConverter(units);

            // Suggest name, but pass null for segments to keep defaults
            ctrl.initData("P" + processCounter, null);

            Stage dlg = dialogStage("Add Process", root);
            dlg.showAndWait();
            if (!ctrl.isConfirmed()) return;

            String name = ctrl.getProcName();
            if (mm.processExists(name)) { showError("Process '" + name + "' already exists."); return; }

            model.Process p = buildProcess(name, ctrl.getSegments());
            mm.addProcess(p);

            // Increment counter only if name was the suggested one or similar? 
            // Better to just increment to stay ahead.
            processCounter++;

            refreshProcessList();
            refreshSegProcessCombo();
            appendLog("Process '" + name + "' added ("
                    + p.getSegments().size() + " segments, "
                    + units.format(p.totalSize()) + ").");
        } catch (Exception ex) { showError(ex.getMessage()); ex.printStackTrace(); }
    }

    // ── Action: Edit Process ──────────────────────────────────────────────
    @FXML private void onEditProcess() {
        if (mm == null) return;
        String sel = processList.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String name = sel.split(" ")[0];
        model.Process p = mm.getProcesses().get(name);
        if (p == null) return;
        if (p.isAllocated()) { showError("Deallocate '" + name + "' before editing."); return; }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/addProcess.fxml"));
            Parent root = loader.load();
            AddProcessController ctrl = loader.getController();
            ctrl.setUnitConverter(units);

            // Pass raw bytes — initData converts to inputUnit for display
            List<String[]> segs = new ArrayList<>();
            for (Segment s : p.getSegments())
                segs.add(new String[]{s.getName(), String.valueOf(s.getSize())});
            ctrl.initData(p.getName(), segs);

            Stage dlg = dialogStage("Edit Process", root);
            dlg.showAndWait();
            if (!ctrl.isConfirmed()) return;

            String newName = ctrl.getProcName();
            if (!newName.equals(p.getName()) && mm.processExists(newName)) {
                showError("A process named '" + newName + "' already exists."); return;
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

    // ── Action: Delete Process ────────────────────────────────────────────
    @FXML private void onDeleteProcess() {
        if (mm == null) return;
        String sel = processList.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String name = sel.split(" ")[0];
        model.Process p = mm.getProcesses().get(name);
        if (p == null) return;
        if (p.isAllocated()) { showError("Deallocate '" + name + "' before deleting."); return; }
        mm.removeProcess(name);
        refreshProcessList();
        refreshSegProcessCombo();
        appendLog("Process '" + name + "' deleted.");
    }

    // ── Action: Allocate ──────────────────────────────────────────────────
    @FXML private void onAllocate() {
        String sel = processList.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Select a process first."); return; }
        String name = sel.split(" ")[0];
        mm.setStrategy(rbBestFit.isSelected() ? new BestFitStrategy() : new FirstFitStrategy());
        mm.allocate(name, logger);
        refreshAll();
    }

    // ── Action: Deallocate ────────────────────────────────────────────────
    @FXML private void onDeallocate() {
        String sel = processList.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Select a process first."); return; }
        String name = sel.split(" ")[0];
        mm.deallocate(name, logger);
        refreshAll();
    }

    // ── Segment table ─────────────────────────────────────────────────────
    @FXML private void onSegProcessSelected() { /* trigger via Show button */ }

    @FXML private void onShowSegTable() {
        if (mm == null) return;
        String sel = cbSegProcess.getValue();
        if (sel == null) return;
        model.Process p = mm.getProcesses().get(sel);
        if (p != null) segTable.setItems(p.getSegments());
    }

    // ── Zoom ──────────────────────────────────────────────────────────────
    @FXML private void onZoomIn()  { diagram.zoomIn();  diagram.redraw(mm); }
    @FXML private void onZoomOut() { diagram.zoomOut(); diagram.redraw(mm); }

    // ── Log ───────────────────────────────────────────────────────────────
    @FXML private void onClearLog() { logArea.clear(); }

    // ── Refresh ───────────────────────────────────────────────────────────
    private void refreshAll() {
        if (mm == null) return;
        freeTable .setItems(mm.getFreePartitions());
        allocTable.setItems(mm.getAllocPartitions());
        
        // Force visual refresh of all table cells to reflect new units
        freeTable.refresh();
        allocTable.refresh();
        segTable.refresh();

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

    // ── Utilities ─────────────────────────────────────────────────────────

    /**
     * Build a Process from the segment list returned by AddProcessController.
     * segsData[i] = [name, rawBytesAsString]  (already validated & converted).
     */
    private model.Process buildProcess(String name, List<String[]> segsData) {
        model.Process p = new model.Process(name);
        p.setColor(PALETTE[paletteIdx % PALETTE.length]);
        paletteIdx++;
        for (String[] sd : segsData) {
            // sd[1] is raw bytes (from AddProcessController.onAdd)
            int bytes = (int) Long.parseLong(sd[1]);
            p.addSegment(new Segment(sd[0], bytes));
        }
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
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error");
        a.showAndWait();
    }
}
