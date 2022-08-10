/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.jifa.gclog.diagnoser;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.vo.TimeRange;

@Data
@NoArgsConstructor
public class AnalysisConfig {
    /*
     * Notice: This class should be kept in sync with initializePage in GCLog.vue.
     */
    private TimeRange timeRange;
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

    // Basically mirror of analysisConfigModel in GCLog.vue, but this function is for testing and debugging.
    // No need to keep sync with frontend
    public static AnalysisConfig defaultConfig(GCModel model) {
        AnalysisConfig config = new AnalysisConfig();
        config.setTimeRange(new TimeRange(model.getStartTime(), model.getEndTime()));
        config.setLongPauseThreshold(model.isPauseless() ? 30 : 400);
        config.setLongConcurrentThreshold(30000);
        config.setYoungGCFrequentIntervalThreshold(1000);
        config.setOldGCFrequentIntervalThreshold(15000);
        config.setFullGCFrequentIntervalThreshold(model.isGenerational() ? 60000 : 2000);
        config.setHighOldUsageThreshold(80);
        config.setHighHumongousUsageThreshold(50);
        config.setHighHeapUsageThreshold(60);
        config.setHighMetaspaceUsageThreshold(80);
        config.setSmallGenerationThreshold(10);
        config.setHighPromotionThreshold(3);
        config.setBadThroughputThreshold(90);
        return config;
    }
}
