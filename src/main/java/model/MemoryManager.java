package model;

import javafx.collections.*;
import java.util.*;

public class MemoryManager {

    public enum Algorithm { FIRST_FIT, BEST_FIT }

    private final int totalMemory;
    private final ObservableList<MemoryPartition> freePartitions  = FXCollections.observableArrayList();
    private final ObservableList<MemoryPartition> allocPartitions = FXCollections.observableArrayList();
    private final Map<String, Process>            processes       = new LinkedHashMap<>();
    private Algorithm algorithm = Algorithm.FIRST_FIT;

    public MemoryManager(int totalMemory) {
        this.totalMemory = totalMemory;
    }

    // ── Initial holes ──────────────────────────────────────────────────────
    public void addInitialHole(int start, int size) {
        if (start < 0 || start + size > totalMemory)
            throw new IllegalArgumentException(
                "Hole [" + start + ", " + (start+size-1) + "] exceeds memory bounds (0-" + (totalMemory-1) + ")");
        freePartitions.add(new MemoryPartition(start, size));
        FXCollections.sort(freePartitions);
    }

    // ── Config ─────────────────────────────────────────────────────────────
    public void setAlgorithm(Algorithm a) { algorithm = a; }
    public Algorithm getAlgorithm()       { return algorithm; }

    // ── Processes ──────────────────────────────────────────────────────────
    public void addProcess(Process p)        { processes.put(p.getName(), p); }
    public boolean processExists(String n)   { return processes.containsKey(n); }
    public Map<String, Process> getProcesses(){ return Collections.unmodifiableMap(processes); }

    // ── Allocate ───────────────────────────────────────────────────────────
    public List<String> allocate(String procName) {
        Process proc = processes.get(procName);
        List<String> log = new ArrayList<>();
        if (proc == null) { log.add("ERROR: process not found"); return log; }
        if (proc.isAllocated()) { log.add(procName + " is already allocated."); return log; }

        log.add("Allocating " + procName + " [" + algorithm + "]");

        // Tentative pass — deep-copy free list
        List<MemoryPartition> tentative = deepCopyFree();
        List<int[]> placements = new ArrayList<>();

        for (Segment seg : proc.getSegments()) {
            int idx = findHole(tentative, seg.getSize());
            if (idx == -1) {
                log.add("  ✗ Segment '" + seg.getName() + "' (size=" + seg.getSize() + ") — no hole fits.");
                log.add("  ✗ " + procName + " NOT allocated (all-or-nothing policy).");
                return log;
            }
            MemoryPartition h = tentative.get(idx);
            placements.add(new int[]{h.getStartAddress()});
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
            log.add(String.format("  ✓ %s [%d – %d]", seg.getName(), base, base + seg.getSize() - 1));
        }

        freePartitions.setAll(tentative);
        FXCollections.sort(freePartitions);
        FXCollections.sort(allocPartitions);
        proc.setAllocated(true);
        log.add("  ✓ " + procName + " fully allocated.");
        return log;
    }

    // ── Deallocate ─────────────────────────────────────────────────────────
    public List<String> deallocate(String procName) {
        Process proc = processes.get(procName);
        List<String> log = new ArrayList<>();
        if (proc == null || !proc.isAllocated()) {
            log.add(procName + " is not currently allocated.");
            return log;
        }
        log.add("Deallocating " + procName);

        allocPartitions.removeIf(ap -> {
            if (ap.getProcessName().equals(procName)) {
                log.add(String.format("  freed [%d – %d] (%s)", ap.getStartAddress(), ap.getEndAddress(), ap.getSegmentName()));
                freePartitions.add(new MemoryPartition(ap.getStartAddress(), ap.getSize()));
                return true;
            }
            return false;
        });

        proc.deallocateAll();
        mergeHoles();
        log.add("  Holes merged → " + freePartitions.size() + " hole(s) remain.");
        return log;
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private int findHole(List<MemoryPartition> holes, int size) {
        if (algorithm == Algorithm.FIRST_FIT) {
            for (int i = 0; i < holes.size(); i++)
                if (holes.get(i).getSize() >= size) return i;
        } else {
            int best = -1, bestSz = Integer.MAX_VALUE;
            for (int i = 0; i < holes.size(); i++) {
                int hs = holes.get(i).getSize();
                if (hs >= size && hs < bestSz) { best = i; bestSz = hs; }
            }
            return best;
        }
        return -1;
    }

    private void mergeHoles() {
        FXCollections.sort(freePartitions);
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

    private List<MemoryPartition> deepCopyFree() {
        List<MemoryPartition> copy = new ArrayList<>();
        for (MemoryPartition p : freePartitions)
            copy.add(new MemoryPartition(p.getStartAddress(), p.getSize()));
        Collections.sort(copy);
        return copy;
    }

    // ── Accessors ──────────────────────────────────────────────────────────
    public int getTotalMemory()                                { return totalMemory; }
    public ObservableList<MemoryPartition> getFreePartitions() { return freePartitions; }
    public ObservableList<MemoryPartition> getAllocPartitions(){ return allocPartitions; }
    public int getTotalFree()      { return freePartitions.stream().mapToInt(MemoryPartition::getSize).sum(); }
    public int getTotalAllocated() { return allocPartitions.stream().mapToInt(MemoryPartition::getSize).sum(); }

    public List<MemoryPartition> getAllSorted() {
        List<MemoryPartition> all = new ArrayList<>();
        all.addAll(freePartitions);
        all.addAll(allocPartitions);
        Collections.sort(all);
        return all;
    }
}
