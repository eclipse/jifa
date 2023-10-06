/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
import { defineStore } from 'pinia';

export const useGCLogData = defineStore('GCLogData', {
  state: () => ({
    metadata: null as any,
    analysisConfig: null as any,
    analysisConfigVisible: false,
    showDetails: false,
    capacityInfo: null as any,
    selectedTimeRange: null as any
  }),
  getters: {
    useUptime(state) {
      return state.metadata.timestamp < 0;
    }
  },
  actions: {
    init(metadata: any, timeRange?: any) {
      this.metadata = metadata;
      this.analysisConfig = { ...metadata.analysisConfig };
      if (timeRange) {
        this.analysisConfig.timeRange = timeRange;
      }
      this.showDetails = false;
    },

    setAnalysisConfig(analysisConfig: any) {
      this.analysisConfig = analysisConfig;
    },

    setCapacityData(capacityInfo: any) {
      this.capacityInfo = capacityInfo;
    },

    reset() {
      this.metadata = null;
      this.analysisConfig = null;
    },

    toggleDetails() {
      this.showDetails = !this.showDetails;
    },

    toggleAnalysisConfigVisible(timeRange?: any) {
      if (!this.analysisConfigVisible && timeRange) {
        this.selectedTimeRange = timeRange;
      } else {
        this.selectedTimeRange = null;
      }
      this.analysisConfigVisible = !this.analysisConfigVisible;
    }
  }
});
