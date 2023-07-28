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

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhaseStatistics {
    private List<ParentStatisticsInfo> parents;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentStatisticsInfo {
        private PhaseStatisticItem self;
        private List<PhaseStatisticItem> phases;
        private List<PhaseStatisticItem> causes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhaseStatisticItem{
        private String name;
        private int count;
        private double intervalAvg;
        private double intervalMin;
        private double durationAvg;
        private double durationMax;
        private double durationTotal;
    }
}
