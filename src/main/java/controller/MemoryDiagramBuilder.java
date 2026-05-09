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
    private       double scale = 1.0;

    public MemoryDiagramBuilder(Pane pane) {
        this.pane   = pane;
        this.canvas = new Canvas();
        pane.getChildren().add(canvas);
        canvas.widthProperty().bind(pane.widthProperty());
    }

    public void zoomIn() {
        scale *= 1.25;
        if (scale > 5.0) scale = 5.0;
    }

    public void zoomOut() {
        scale /= 1.25;
        if (scale < 0.2) scale = 0.2;
    }

    public void redraw(MemoryManager mm) {
        if (mm == null) return;

        List<MemoryPartition> parts = mm.getAllSorted();
        Map<String, Process>  procs = mm.getProcesses();
        int total = mm.getTotalMemory();

        double min_blk_scaled = MIN_BLK * scale;
        double baseDrawH = Math.max(450 * scale, parts.size() * (min_blk_scaled + 2));
        
        // Pass 1: compute actual height
        double actualDrawH = 0;
        int currentAddrTemp = 0;
        for (MemoryPartition p : parts) {
            if (p.getStartAddress() > currentAddrTemp) {
                double gapRatio = (double) (p.getStartAddress() - currentAddrTemp) / total;
                actualDrawH += Math.max(min_blk_scaled, gapRatio * baseDrawH);
                currentAddrTemp = p.getStartAddress();
            }
            double ratio = (double) p.getSize() / total;
            actualDrawH += Math.max(min_blk_scaled, ratio * baseDrawH);
            currentAddrTemp = p.getEndAddress() + 1;
        }
        if (currentAddrTemp < total) {
            double gapRatio = (double) (total - currentAddrTemp) / total;
            actualDrawH += Math.max(min_blk_scaled, gapRatio * baseDrawH);
        }

        // Compute needed height. We add 80 to PAD_BOT for the bottom stats pill.
        double availH = PAD_TOP + actualDrawH + PAD_BOT + 80;
        canvas.setHeight(availH);
        pane.setMinHeight(availH);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double cw = canvas.getWidth();
        gc.clearRect(0, 0, cw, availH);

        // Background
        gc.setFill(Color.web("#F8FAFC"));
        gc.fillRect(0, 0, cw, availH);

        // Draw drop shadow behind the memory bar
        gc.setEffect(new javafx.scene.effect.DropShadow(8, 0, 4, Color.color(0, 0, 0, 0.1)));
        gc.setFill(Color.WHITE);
        gc.fillRect(BAR_X, PAD_TOP, BAR_W, actualDrawH);
        gc.setEffect(null); // turn off drop shadow

        double y = PAD_TOP;
        int currentAddr = 0;
        double drawH = baseDrawH; // use baseDrawH for scaling calculations

        for (MemoryPartition p : parts) {
            // Draw Reserved space if there's a gap before this partition
            if (p.getStartAddress() > currentAddr) {
                int gapSize = p.getStartAddress() - currentAddr;
                double gapRatio = (double) gapSize / total;
                double gapH = Math.max(min_blk_scaled, gapRatio * drawH);

                drawBlock(gc, currentAddr, p.getStartAddress() - 1, gapH, y, 
                          Color.web("#E2E8F0"), Color.web("#94A3B8"), 
                          "RESERVED (" + gapSize + " " + MainController.CURRENT_UNIT + ")", Color.web("#475569"));
                y += gapH;
                currentAddr = p.getStartAddress();
            }

            double ratio  = (double) p.getSize() / total;
            double blockH = Math.max(min_blk_scaled, ratio * drawH);

            Color fill;
            Color stroke;
            Color textFill;
            String label;

            if (p.isFree()) {
                fill = Color.web("#D1FAE5");
                stroke = Color.web("#10B981");
                textFill = Color.web("#065F46");
                label = "FREE (" + p.getSize() + " " + MainController.CURRENT_UNIT + ")";
            } else {
                fill = getProcessColor(p.getProcessName(), procs);
                stroke = darken(fill, 0.2);
                textFill = darken(fill, 0.7);
                label = p.getProcessName() + " · " + p.getSegmentName() + " (" + p.getSize() + " " + MainController.CURRENT_UNIT + ")";
            }

            drawBlock(gc, currentAddr, p.getEndAddress(), blockH, y, fill, stroke, label, textFill);
            
            y += blockH;
            currentAddr = p.getEndAddress() + 1;
        }

        // Draw trailing Reserved space if any
        if (currentAddr < total) {
            int gapSize = total - currentAddr;
            double gapRatio = (double) gapSize / total;
            double gapH = Math.max(min_blk_scaled, gapRatio * drawH);

            drawBlock(gc, currentAddr, total - 1, gapH, y, 
                      Color.web("#E2E8F0"), Color.web("#94A3B8"), 
                      "RESERVED (" + gapSize + " " + MainController.CURRENT_UNIT + ")", Color.web("#475569"));
            y += gapH;
        }

        // Outer border
        gc.setStroke(Color.web("#334155"));
        gc.setLineWidth(2.0);
        gc.strokeRect(BAR_X, PAD_TOP, BAR_W, y - PAD_TOP);

        // Bottom address label
        gc.setFill(Color.web("#334155"));
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        String lastAddrStr = String.valueOf(total - 1);
        gc.fillText(lastAddrStr, BAR_X - 8 - textWidth(lastAddrStr, 11), y + 10);
        gc.setStroke(Color.web("#CBD5E1"));
        gc.setLineWidth(1);
        gc.strokeLine(BAR_X - 5, y, BAR_X, y);

        // Stats at bottom (Under the diagram)
        y += 35;
        int reserved = total - mm.getTotalFree() - mm.getTotalAllocated();
        
        // Rounded pill background for stats
        double statsWidth = BAR_W + 70;
        double statsX = BAR_X - 35;
        
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillRoundRect(statsX, y, statsWidth, 34, 16, 16);
        gc.setStroke(Color.web("#CBD5E1"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(statsX, y, statsWidth, 34, 16, 16);
        
        gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
        
        // Free
        gc.setFill(Color.web("#059669"));
        gc.fillText("Free: " + mm.getTotalFree() + " " + MainController.CURRENT_UNIT, statsX + 15, y + 22);
        
        // Used
        gc.setFill(Color.web("#DC2626"));
        gc.fillText("Used: " + mm.getTotalAllocated() + " " + MainController.CURRENT_UNIT, statsX + 90, y + 22);
        
        // Reserved
        if (reserved > 0) {
            gc.setFill(Color.web("#475569"));
            gc.fillText("Rsvd: " + reserved + " " + MainController.CURRENT_UNIT, statsX + 170, y + 22);
        }
    }

    private void drawBlock(GraphicsContext gc, int startAddr, int endAddr, double h, double y, 
                           Color fill, Color stroke, String label, Color textFill) {
        
        // Fill
        gc.setFill(fill);
        gc.fillRect(BAR_X, y, BAR_W, h);

        // Inner Border
        gc.setStroke(stroke);
        gc.setLineWidth(1.0);
        gc.strokeRect(BAR_X, y, BAR_W, h);

        // Label inside block
        if (h >= 16) {
            gc.setFill(textFill);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 11));
            double tx = BAR_X + 8;
            double ty = y + h / 2 + 4;
            
            gc.save();
            gc.rect(BAR_X + 2, y + 1, BAR_W - 4, h - 2);
            gc.clip();
            gc.fillText(label, tx, ty);
            gc.restore();
        }

        // Left Address (Start)
        gc.setFill(Color.web("#334155"));
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        String startStr = String.valueOf(startAddr);
        gc.fillText(startStr, BAR_X - 8 - textWidth(startStr, 11), y + 10);
        
        // Right Address (End)
        String endStr = String.valueOf(endAddr);
        gc.setFill(Color.web("#475569"));
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        gc.fillText(endStr, BAR_X + BAR_W + 6, Math.max(y + 10, y + h - 2));
        
        // Tick marks
        gc.setStroke(Color.web("#CBD5E1"));
        gc.setLineWidth(1);
        gc.strokeLine(BAR_X - 5, y, BAR_X, y);
    }

    private Color getProcessColor(String name, Map<String, Process> procs) {
        if (procs == null) return Color.web("#60A5FA");
        Process p = procs.get(name);
        return (p != null && p.getColor() != null) ? p.getColor() : Color.web("#60A5FA");
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
