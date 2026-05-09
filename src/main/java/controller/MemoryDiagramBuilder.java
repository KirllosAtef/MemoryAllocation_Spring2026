package controller;

import javafx.scene.canvas.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import model.MemoryManager;
import model.MemoryPartition;
import model.Process;

import java.util.List;
import java.util.Map;

/**
 * Draws the memory-layout diagram onto a JavaFX Canvas placed inside
 * the FXML <Pane fx:id="diagramPane">.
 */
public class MemoryDiagramBuilder {

    private static final double BAR_X     = 100;
    private static final double BAR_W     = 160;
    private static final double PAD_TOP   = 20;
    private static final double PAD_BOT   = 24;
    private static final double MIN_BLK   = 20;   // minimum block height px

    private final Pane   pane;
    private       Canvas canvas;

    public MemoryDiagramBuilder(Pane pane) {
        this.pane   = pane;
        this.canvas = new Canvas();
        pane.getChildren().add(canvas);
        canvas.widthProperty().bind(pane.widthProperty());
    }

    public void redraw(MemoryManager mm) {
        if (mm == null) return;

        List<MemoryPartition> parts = mm.getAllSorted();
        Map<String, Process>  procs = mm.getProcesses();
        int total = mm.getTotalMemory();

        // Compute needed height
        double availH = Math.max(400, parts.size() * (MIN_BLK + 2) + PAD_TOP + PAD_BOT + 30);
        canvas.setHeight(availH);
        pane.setMinHeight(availH);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double cw = canvas.getWidth();
        gc.clearRect(0, 0, cw, availH);

        // Background
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, cw, availH);

        double drawH = availH - PAD_TOP - PAD_BOT;

        // Outer memory border
        gc.setStroke(Color.web("#CCCCCC"));
        gc.setLineWidth(1.5);
        gc.strokeRect(BAR_X, PAD_TOP, BAR_W, drawH);

        double y = PAD_TOP;
        for (MemoryPartition p : parts) {
            double ratio  = (double) p.getSize() / total;
            double blockH = Math.max(MIN_BLK, ratio * drawH);

            // Fill
            Color fill;
            if (p.isFree()) {
                fill = Color.web("#D1FAE5");
            } else {
                fill = getProcessColor(p.getProcessName(), procs);
            }
            gc.setFill(fill);
            gc.fillRect(BAR_X, y, BAR_W, blockH);

            // Border
            gc.setStroke(p.isFree() ? Color.web("#6EE7B7") : Color.web("#333333"));
            gc.setLineWidth(p.isFree() ? 1 : 1.5);
            gc.strokeRect(BAR_X, y, BAR_W, blockH);

            // Label inside block
            if (blockH >= 16) {
                String label = p.isFree()
                    ? "FREE (" + p.getSize() + " " + MainController.CURRENT_UNIT + ")"
                    : p.getProcessName() + " · " + p.getSegmentName() + " (" + p.getSize() + " " + MainController.CURRENT_UNIT + ")";
                gc.setFill(p.isFree() ? Color.web("#065F46") : darken(fill, 0.5));
                gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 10));
                double tx = BAR_X + 5;
                double ty = y + blockH / 2 + 4;
                // Clip label
                gc.save();
                gc.rect(BAR_X + 2, y + 1, BAR_W - 4, blockH - 2);
                gc.clip();
                gc.fillText(label, tx, ty);
                gc.restore();
            }

            // Address label (left side)
            gc.setFill(Color.web("#555555"));
            gc.setFont(Font.font("Monospaced", 10));
            String addrStr = String.valueOf(p.getStartAddress());
            gc.fillText(addrStr, BAR_X - 6 - textWidth(addrStr, 10), y + 10);
            gc.setStroke(Color.web("#AAAAAA"));
            gc.setLineWidth(1);
            gc.strokeLine(BAR_X - 4, y, BAR_X, y);

            // Address label (right side — end address)
            if (blockH >= 20) {
                String endStr = String.valueOf(p.getEndAddress());
                gc.setFill(Color.web("#888888"));
                gc.setFont(Font.font("Monospaced", 9));
                gc.fillText(endStr, BAR_X + BAR_W + 4, y + blockH - 3);
            }

            y += blockH;
        }

        // Bottom address
        gc.setFill(Color.web("#555555"));
        gc.setFont(Font.font("Monospaced", 10));
        String last = String.valueOf(total - 1);
        gc.fillText(last, BAR_X - 6 - textWidth(last, 10), y + 10);
        gc.setStroke(Color.web("#AAAAAA"));
        gc.strokeLine(BAR_X - 4, y, BAR_X, y);

        // Stats at bottom
        gc.setFill(Color.web("#1565C0"));
        gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 10));
        gc.fillText("Free: " + mm.getTotalFree() + " " + MainController.CURRENT_UNIT, BAR_X, y + 18);
        gc.setFill(Color.web("#C62828"));
        gc.fillText("Used: " + mm.getTotalAllocated() + " " + MainController.CURRENT_UNIT, BAR_X + 80, y + 18);
    }

    private Color getProcessColor(String name, Map<String, Process> procs) {
        if (procs == null) return Color.CORNFLOWERBLUE;
        Process p = procs.get(name);
        return (p != null && p.getColor() != null) ? p.getColor() : Color.CORNFLOWERBLUE;
    }

    private Color darken(Color c, double f) {
        return new Color(
            Math.max(0, c.getRed()   * (1 - f)),
            Math.max(0, c.getGreen() * (1 - f)),
            Math.max(0, c.getBlue()  * (1 - f)),
            1.0
        );
    }

    private double textWidth(String text, int size) {
        return text.length() * size * 0.6; // approximation
    }
}
