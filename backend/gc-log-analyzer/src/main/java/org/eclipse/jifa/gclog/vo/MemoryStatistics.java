package org.eclipse.jifa.gclog.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryStatistics {
    private MemoryStatisticsItem young;
    private MemoryStatisticsItem old;
    private MemoryStatisticsItem humongous;
    private MemoryStatisticsItem heap;
    private MemoryStatisticsItem metaspace;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryStatisticsItem {
        private long capacityAvg;
        private long usedMax;
        private long usedAvgAfterFullGC;
        private long usedAvgAfterOldGC;
    }
}
