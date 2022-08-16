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
