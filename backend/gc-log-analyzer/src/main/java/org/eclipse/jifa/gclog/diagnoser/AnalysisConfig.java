package org.eclipse.jifa.gclog.diagnoser;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.jifa.gclog.vo.TimeRange;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisConfig {
    /*
     * Notice: This class should be kept in sync with initializePage in GCLog.vue.
     */
    private TimeRange timRange;
    private int longPauseThreshold;
    private int longConcurrentThreshold;
    private int youngGCFrequentIntervalThreshold;
    private int oldGCFrequentIntervalThreshold;
    private int fullGCFrequentIntervalThreshold;
    private int highOldUsageThreshold;
    private int highHumongousUsageThreshold;
    private int highHeapUsageThreshold;
    private int highMetaspaceUsageThreshold;
    private int smallGenerationThreshold;
    private int highPromotionThreshold;
    private int badThroughputThreshold;
}
