package model.algorithm;

import java.util.List;
import model.MemoryPartition;

public class FirstFitStrategy implements AllocationStrategy {
    @Override
    public int findHole(List<MemoryPartition> holes, int size) {
        for (int i = 0; i < holes.size(); i++) {
            if (holes.get(i).getSize() >= size) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getName() {
        return "First-Fit";
    }
}
