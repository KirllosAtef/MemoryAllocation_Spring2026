package model;

import javafx.collections.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import model.algorithm.AllocationStrategy;
import model.algorithm.FirstFitStrategy;
import model.service.SystemLogger;

public class MemoryManager {

    private final int totalMemory;
    private final ObservableList<MemoryPartition> freePartitions = FXCollections.observableArrayList();
    private final ObservableList<MemoryPartition> allocPartitions = FXCollections.observableArrayList();
    private final Map<String, Process> processes = new LinkedHashMap<>();
    private AllocationStrategy strategy = new FirstFitStrategy();

    public MemoryManager(int totalMemory) {
        this.totalMemory = totalMemory;
    }

    // ── Initial holes ──────────────────────────────────────────────────────
    public void addInitialHole(int start, int size) {
        if (start < 0 || start + size > totalMemory)
            throw new IllegalArgumentException(
                    "Hole [" + start + ", " + (start + size - 1) + "] exceeds memory bounds (0-" + (totalMemory - 1)
                            + ")");
        freePartitions.add(new MemoryPartition(start, size));
        Collections.sort(freePartitions);
    }

    // ── Config ─────────────────────────────────────────────────────────────
    public void setStrategy(AllocationStrategy s) {
        strategy = s;
    }

    public AllocationStrategy getStrategy() {
        return strategy;
    }

    // ── Processes ──────────────────────────────────────────────────────────
    public void addProcess(Process p) {
        processes.put(p.getName(), p);
    }

    public boolean processExists(String n) {
        return processes.containsKey(n);
    }

    public void removeProcess(String n) {
        processes.remove(n);
    }

    public Map<String, Process> getProcesses() {
        return Collections.unmodifiableMap(processes);
    }

    // ── Allocate ───────────────────────────────────────────────────────────
    public void allocate(String procName, SystemLogger logger) {
        Process proc = processes.get(procName);
        if (proc == null) {
            logger.log("ERROR: process not found");
            return;
        }
        if (proc.isAllocated()) {
            logger.log(procName + " is already allocated.");
            return;
        }

        logger.log("Allocating " + procName + " [" + strategy.getName() + "]");

        // Tentative pass — deep-copy free list
        java.util.List<MemoryPartition> tentative = deepCopyFree();
        java.util.List<int[]> placements = new ArrayList<>();

        for (Segment seg : proc.getSegments()) {
            int idx = strategy.findHole(tentative, seg.getSize());
            if (idx == -1) {
                logger.log("  ✗ Segment '" + seg.getName() + "' (size=" + seg.getSize() + ") — no hole fits.");
                logger.log("  ✗ " + procName + " NOT allocated (all-or-nothing policy).");
                proc.setState(Process.State.FAILED);
                return;
            }
            MemoryPartition h = tentative.get(idx);
            placements.add(new int[] { h.getStartAddress() });
            if (h.getSize() == seg.getSize()) {
                tentative.remove(idx);
            } else {
                h.setStartAddress(h.getStartAddress() + seg.getSize());
                h.setSize(h.getSize() - seg.getSize());
            }
        }

        // Commit
        List<Segment> segs = proc.getSegments();
        for (int i = 0; i < segs.size(); i++) {
            Segment seg = segs.get(i);
            int base = placements.get(i)[0];
            seg.allocate(base);
            allocPartitions.add(new MemoryPartition(base, seg.getSize(), procName, seg.getName()));
            logger.log(String.format("  ✓ %s [%d – %d]", seg.getName(), base, base + seg.getSize() - 1));
        }

        freePartitions.setAll(tentative);
        Collections.sort(freePartitions);
        Collections.sort(allocPartitions);
        proc.setAllocated(true);
        logger.log("  ✓ " + procName + " fully allocated.");
    }

    // ── Deallocate ─────────────────────────────────────────────────────────
    public void deallocate(String procName, SystemLogger logger) {
        Process proc = processes.get(procName);
        if (proc == null || !proc.isAllocated()) {
            logger.log(procName + " is not currently allocated.");
            return;
        }
        logger.log("Deallocating " + procName);

        allocPartitions.removeIf(ap -> {
            if (ap.getProcessName().equals(procName)) {
                logger.log(String.format("  freed [%d – %d] (%s)", ap.getStartAddress(), ap.getEndAddress(),
                        ap.getSegmentName()));
                freePartitions.add(new MemoryPartition(ap.getStartAddress(), ap.getSize()));
                return true;
            }
            return false;
        });

        proc.deallocateAll();
        proc.setAllocated(false);
        mergeHoles();
        logger.log("  Holes merged → " + freePartitions.size() + " hole(s) remain.");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void mergeHoles() {
        Collections.sort(freePartitions);
        boolean merged = true;
        while (merged) {
            merged = false;
            for (int i = 0; i < freePartitions.size() - 1; i++) {
                MemoryPartition a = freePartitions.get(i);
                MemoryPartition b = freePartitions.get(i + 1);
                if (a.getEndAddress() + 1 >= b.getStartAddress()) {
                    int newEnd = Math.max(a.getEndAddress(), b.getEndAddress());
                    a.setSize(newEnd - a.getStartAddress() + 1);
                    freePartitions.remove(i + 1);
                    merged = true;
                    break;
                }
            }
        }
    }

    private java.util.List<MemoryPartition> deepCopyFree() {
        java.util.List<MemoryPartition> copy = new ArrayList<>();
        for (MemoryPartition p : freePartitions)
            copy.add(new MemoryPartition(p.getStartAddress(), p.getSize()));
        Collections.sort(copy);
        return copy;
    }

    // ── Accessors ──────────────────────────────────────────────────────────
    public int getTotalMemory() {
        return totalMemory;
    }

    public ObservableList<MemoryPartition> getFreePartitions() {
        return freePartitions;
    }

    public ObservableList<MemoryPartition> getAllocPartitions() {
        return allocPartitions;
    }

    public int getTotalFree() {
        return freePartitions.stream().mapToInt(MemoryPartition::getSize).sum();
    }

    public int getTotalAllocated() {
        return allocPartitions.stream().mapToInt(MemoryPartition::getSize).sum();
    }

    public java.util.List<MemoryPartition> getAllSorted() {
        java.util.List<MemoryPartition> all = new ArrayList<>();
        all.addAll(freePartitions);
        all.addAll(allocPartitions);
        Collections.sort(all);
        return all;
    }
}
