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

package org.eclipse.jifa.gclog;

import org.eclipse.jifa.gclog.diagnoser.AnalysisConfig;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.vo.TimeRange;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TestUtil {
    public static BufferedReader stringToBufferedReader(String source) {
        InputStream inputStream = new ByteArrayInputStream(source.getBytes());
        return new BufferedReader(new InputStreamReader(inputStream));
    }

    // mirror of analysisConfigModel in GCLog.vue
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
