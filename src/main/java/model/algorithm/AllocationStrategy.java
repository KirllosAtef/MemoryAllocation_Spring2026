package model.algorithm;

import java.util.List;
import model.MemoryPartition;

public interface AllocationStrategy {
    int findHole(List<MemoryPartition> holes, int size);
    String getName();
}
