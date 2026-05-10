package view;

import javafx.scene.canvas.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import model.MemoryManager;
import model.MemoryPartition;
import model.Process;
import model.service.UnitConverter;

import java.util.List;
import java.util.Map;

/**
 * Draws the memory-layout diagram onto a JavaFX Canvas.
 *
 * MVC compliance: View layer only. Knows model types and UnitConverter.
 * Zero imports from controller package.
 */
public class MemoryDiagramBuilder {

    private static final double BAR_X   = 100;
    private static final double BAR_W   = 240;
    private static final double PAD_TOP = 20;
    private static final double PAD_BOT = 24;
    private static final double MIN_BLK = 20;

    private final Pane          pane;
    private final Canvas        canvas;
    private final UnitConverter units;
    private       double        scale = 1.0;
    private       MemoryManager lastMm;

    /** @param units shared UnitConverter instance supplied by MainController */
    public MemoryDiagramBuilder(Pane pane, UnitConverter units) {
        this.pane   = pane;
        this.units  = units;
        this.canvas = new Canvas();
        pane.getChildren().add(canvas);
        canvas.widthProperty().bind(pane.widthProperty());
        
        pane.widthProperty().addListener((obs, oldVal, newVal) -> redraw(this.lastMm));
    }

    public void zoomIn()  { scale = Math.min(scale * 1.25, 5.0); redraw(this.lastMm); }
    public void zoomOut() { scale = Math.max(scale / 1.25, 0.2); redraw(this.lastMm); }

    public void redraw(MemoryManager mm) {
        this.lastMm = mm;
        if (mm == null) return;

        List<MemoryPartition> parts = mm.getAllSorted();
        Map<String, Process>  procs = mm.getProcesses();
        int total = mm.getTotalMemory();

        double minBlkScaled = MIN_BLK * scale;
        
        double viewportH = 500;
        if (pane.getParent() instanceof javafx.scene.control.ScrollPane) {
            javafx.scene.control.ScrollPane sp = (javafx.scene.control.ScrollPane) pane.getParent();
            if (sp.getViewportBounds().getHeight() > 0) {
                viewportH = sp.getViewportBounds().getHeight();
            }
        } else {
            viewportH = pane.getHeight() > 0 ? pane.getHeight() : 500;
        }
        
        // Use more of the viewport and increase minimum height
        double targetH = Math.max(550, viewportH - PAD_TOP - PAD_BOT - 100);
        double baseDrawH = Math.max(targetH * scale, parts.size() * (minBlkScaled + 2));
        double actualDrawH  = measureHeight(parts, total, minBlkScaled, baseDrawH);

        double availH = PAD_TOP + actualDrawH + PAD_BOT + 80;
        canvas.setHeight(availH);
        pane.setMinHeight(availH);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double cw = canvas.getWidth();
        double barX = (cw > 0) ? (cw - BAR_W) / 2 : BAR_X;
        
        gc.clearRect(0, 0, cw, Math.max(availH, canvas.getHeight()));
        gc.setFill(Color.web("#F8FAFC"));
        gc.fillRect(0, 0, cw, Math.max(availH, canvas.getHeight()));

        gc.setEffect(new DropShadow(8, 0, 4, Color.color(0, 0, 0, 0.1)));
        gc.setFill(Color.WHITE);
        gc.fillRect(barX, PAD_TOP, BAR_W, actualDrawH);
        gc.setEffect(null);

        double y = PAD_TOP;
        int currentAddr = 0;

        for (MemoryPartition p : parts) {
            if (p.getStartAddress() > currentAddr) {
                int    gapSz = p.getStartAddress() - currentAddr;
                double gapH  = Math.max(minBlkScaled, (double) gapSz / total * baseDrawH);
                drawBlock(gc, barX, currentAddr, p.getStartAddress() - 1, gapH, y,
                        Color.web("#E2E8F0"), Color.web("#94A3B8"),
                        "RESERVED (" + units.format(gapSz) + ")", Color.web("#475569"));
                y += gapH;
                currentAddr = p.getStartAddress();
            }
            double blockH = Math.max(minBlkScaled, (double) p.getSize() / total * baseDrawH);
            Color fill, stroke, textFill;
            String label;
            if (p.isFree()) {
                fill = Color.web("#D1FAE5"); stroke = Color.web("#10B981");
                textFill = Color.web("#065F46");
                label = "FREE (" + units.format(p.getSize()) + ")";
            } else {
                fill = processColor(p.getProcessName(), procs);
                stroke = darken(fill, 0.2); textFill = darken(fill, 0.7);
                label = p.getProcessName() + " · " + p.getSegmentName()
                      + " (" + units.format(p.getSize()) + ")";
            }
            drawBlock(gc, barX, currentAddr, p.getEndAddress(), blockH, y, fill, stroke, label, textFill);
            y += blockH;
            currentAddr = p.getEndAddress() + 1;
        }

        if (currentAddr < total) {
            int    gapSz = total - currentAddr;
            double gapH  = Math.max(minBlkScaled, (double) gapSz / total * baseDrawH);
            drawBlock(gc, barX, currentAddr, total - 1, gapH, y,
                    Color.web("#E2E8F0"), Color.web("#94A3B8"),
                    "RESERVED (" + units.format(gapSz) + ")", Color.web("#475569"));
            y += gapH;
        }

        gc.setStroke(Color.web("#334155")); gc.setLineWidth(2.0);
        gc.strokeRect(barX, PAD_TOP, BAR_W, y - PAD_TOP);

        drawStatsPill(gc, mm, barX, y + 35);
    }

    private double measureHeight(List<MemoryPartition> parts, int total,
                                 double minBlk, double baseH) {
        double h = 0; int cur = 0;
        for (MemoryPartition p : parts) {
            if (p.getStartAddress() > cur) {
                h += Math.max(minBlk, (double)(p.getStartAddress() - cur) / total * baseH);
                cur = p.getStartAddress();
            }
            h += Math.max(minBlk, (double) p.getSize() / total * baseH);
            cur = p.getEndAddress() + 1;
        }
        if (cur < total) h += Math.max(minBlk, (double)(total - cur) / total * baseH);
        return h;
    }

    private void drawBlock(GraphicsContext gc, double barX, int startAddr, int endAddr,
                           double h, double y, Color fill, Color stroke,
                           String label, Color textFill) {
        gc.setFill(fill); gc.fillRect(barX, y, BAR_W, h);
        gc.setStroke(stroke); gc.setLineWidth(1.0);
        gc.strokeRect(barX, y, BAR_W, h);
        if (h >= 16) {
            gc.setFill(textFill);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 11));
            gc.save();
            gc.rect(barX + 2, y + 1, BAR_W - 4, h - 2);
            gc.clip();
            gc.fillText(label, barX + 8, y + h / 2 + 4);
            gc.restore();
        }
        gc.setFill(Color.web("#334155"));
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        String s = units.formatAddress(startAddr);
        gc.fillText(s, barX - 8 - approxW(s, 11), y + 10);
        String e = units.formatAddress(endAddr);
        gc.setFill(Color.web("#475569"));
        gc.fillText(e, barX + BAR_W + 6, Math.max(y + 10, y + h - 2));
        gc.setStroke(Color.web("#CBD5E1")); gc.setLineWidth(1);
        gc.strokeLine(barX - 5, y, barX, y);
    }

    private void drawStatsPill(GraphicsContext gc, MemoryManager mm, double barX, double y) {
        int reserved = mm.getTotalMemory() - mm.getTotalFree() - mm.getTotalAllocated();
        double pw = BAR_W + 160, px = barX - 80;
        gc.setFill(Color.WHITE); gc.fillRoundRect(px, y, pw, 34, 16, 16);
        gc.setStroke(Color.web("#CBD5E1")); gc.setLineWidth(1);
        gc.strokeRoundRect(px, y, pw, 34, 16, 16);
        gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
        gc.setFill(Color.web("#059669"));
        gc.fillText("Free: " + units.format(mm.getTotalFree()), px + 20, y + 22);
        gc.setFill(Color.web("#DC2626"));
        gc.fillText("Used: " + units.format(mm.getTotalAllocated()), px + 140, y + 22);
        if (reserved > 0) {
            gc.setFill(Color.web("#475569"));
            gc.fillText("Rsvd: " + units.format(reserved), px + 260, y + 22);
        }
    }

    private Color processColor(String name, Map<String, Process> procs) {
        if (procs == null) return Color.web("#60A5FA");
        Process p = procs.get(name);
        return (p != null && p.getColor() != null) ? p.getColor() : Color.web("#60A5FA");
    }

    private Color darken(Color c, double f) {
        return new Color(Math.max(0, c.getRed()*(1-f)), Math.max(0, c.getGreen()*(1-f)),
                         Math.max(0, c.getBlue()*(1-f)), 1.0);
    }

    private double approxW(String t, int sz) { return t.length() * sz * 0.6; }
}
