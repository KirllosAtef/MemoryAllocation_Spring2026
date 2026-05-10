package model.algorithm;

import java.util.List;
import model.MemoryPartition;

public class BestFitStrategy implements AllocationStrategy {
    @Override
    public int findHole(List<MemoryPartition> holes, int size) {
        int best = -1;
        int bestSz = Integer.MAX_VALUE;
        for (int i = 0; i < holes.size(); i++) {
            int hs = holes.get(i).getSize();
            if (hs >= size && hs < bestSz) {
                best = i;
                bestSz = hs;
            }
        }
        return best;
    }

    @Override
    public String getName() {
        return "Best-Fit";
    }
}
