<!--
    Copyright (c) 2022 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->

<template>
  <el-card>
    <div slot="header">
      <span>{{ $t('jifa.gclog.phaseStats.phaseStats') }}</span>
      <span style="margin-left: 30px">
        <el-select
            v-model="displayMode"
            @change="prepareDisplayData"
        >
          <el-option
              v-for="option of displayModeOptions"
              :key="option"
              :label="$t(`jifa.gclog.phaseStats.${option}`)"
              :value="option"
          ></el-option>
        </el-select>
      </span>
    </div>

    <el-table :data="displayData"
              :loading="loading"
              row-key="key"
              default-expand-all
              size="medium"
              max-height="500"
              :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
              :show-header="true"
    >
      <el-table-column
          v-for="column in columns"
          :key="column"
          :width="columnWidth(column)"
          :label="$t(`jifa.gclog.phaseStats.${column}`)">
        <template slot-scope="scope">
          <el-tooltip
              v-if="scope.row[column].pause"
              effect="dark"
              :content="$t('jifa.gclog.stwTooltip')"
              placement="bottom">
            <i class="el-icon-video-pause"></i>
          </el-tooltip>
          <span :class="scope.row[column].bad ? 'bad-metric' : ''">{{ scope.row[column].value }}</span>
          <Hint :info="scope.row[column].hint"/>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script>
import {gclogService, formatTimePeriod} from '@/util'
import axios from "axios";
import Hint from "@/components/gclog/Hint";
import * as gcutil from "@/components/gclog/GCLogUtil"

export default {
  props: ["file", "metadata", "analysisConfig"],
  data() {
    return {
      loading: true,
      data: null,
      metrics: null,
      displayModeOptions: ["importantMode", "pauseMode", "structuredMode", "causeMode"],
      columns: ['name', 'count', 'intervalAvg', 'intervalMin', 'durationAvg', 'durationMax', 'durationTotal'],
      displayMode: "importantMode",
      originalData: [],
      displayData: [],
    }
  },
  components: {
    Hint
  },
  methods: {
    prepareDisplayData() {
      let data = [];
      this.originalData.forEach(originalParent => {
        if (this.displayMode === "causeMode" && !this.parentHasGCCause(originalParent.self.name)) {
          return
        }
        let parentData = null;
        const parentNeeded = this.displayMode === "structuredMode" || this.displayMode === "causeMode"
            || (this.displayMode === "importantMode" && this.metadata.importantEventTypes.indexOf(originalParent.self.name) >= 0)
            || (this.displayMode === "pauseMode" && this.metadata.mainPauseEventTypes.indexOf(originalParent.self.name) >= 0)
        if (parentNeeded) {
          parentData = this.formatDataItem(true, originalParent.self, null);
          data.push(parentData)
        }
        if (this.displayMode === "structuredMode" || this.displayMode === "causeMode") {
          parentData.children = []
        }
        if (this.displayMode === "causeMode") {
          if (originalParent.causes) {
            originalParent.causes.forEach(originalCause => {
              parentData.children.push(this.formatDataItem(false, originalCause, originalParent.self))
            })
          }
        } else if (originalParent.phases) {
          originalParent.phases.forEach(originalPhase => {
            if (this.displayMode === "structuredMode") {
              parentData.children.push(this.formatDataItem(true, originalPhase, originalParent.self))
            } else if ((this.displayMode === "importantMode" && this.metadata.importantEventTypes.indexOf(originalPhase.name) >= 0) ||
                (this.displayMode === "pauseMode" && this.metadata.mainPauseEventTypes.indexOf(originalPhase.name) >= 0)) {
              data.push(this.formatDataItem(true, originalPhase, originalParent.self))
            }
          })
        }
        if (parentNeeded && parentData.children) {
          parentData.children.sort((d1, d2) => (d2.count.value - d1.count.value))
        }
      })
      data.sort((d1, d2) => (d2.count.value - d1.count.value))
      this.displayData = data;
    },
    formatDataItem(isPhase, originalData, originalParent) {
      return {
        key: (originalParent === null ? '' : originalParent.name) + originalData.name,
        name: {
          value: originalData.name,
          hint: isPhase ? gcutil.getPhaseHint(originalData.name) : gcutil.getCauseHint(originalData.name),
          bad: isPhase ? gcutil.badPhase(originalData.name) : gcutil.badCause(originalParent.name, originalData.name),
          pause: isPhase && gcutil.isPause(originalData.name, this.metadata)
        },
        count: {
          value: originalData.count,
          bad: this.badCount(isPhase, originalData, originalParent)
        },
        intervalAvg: {
          value: formatTimePeriod(originalData.intervalAvg),
          bad: isPhase && originalData.intervalAvg >= 0 && originalData.intervalAvg <= gcutil.badIntervalThreshold(originalData.name, this.analysisConfig)
        },
        intervalMin: {
          value: formatTimePeriod(originalData.intervalMin),
          bad: isPhase && originalData.intervalMin >= 0 && originalData.intervalMin <= gcutil.badIntervalThreshold(originalData.name, this.analysisConfig) / 2
        },
        durationAvg: {
          value: formatTimePeriod(originalData.durationAvg),
          bad: originalData.durationAvg >= gcutil.badDurationThreshold(isPhase ? originalData.name : originalParent.name, this.analysisConfig, this.metadata)
        },
        durationMax: {
          value: formatTimePeriod(originalData.durationMax),
          bad: originalData.durationMax >= gcutil.badDurationThreshold(isPhase ? originalData.name : originalParent.name, this.analysisConfig, this.metadata) * 2
        },
        durationTotal: {
          value: formatTimePeriod(originalData.durationTotal),
        },
      }
    },
    badCount(isPhase, originalData, originalParent) {
      if (!isPhase) {
        const cause = originalData.name;
        if ((originalParent.count >= 10 && originalData.count / originalParent.count >= 0.3) &&
            (cause === 'G1 Humongous Allocation')) {
          return true
        }
      }
      return false
    },
    loadData() {
      this.loading = true
      const requestConfig = {params: {...this.analysisConfig.timeRange}}
      axios.get(gclogService(this.file, 'phaseStatistics'), requestConfig).then(resp => {
        this.originalData = resp.data.parents
        this.prepareDisplayData()
        this.loading = false;
      })
    },
    parentHasGCCause(phase) {
      return phase !== "CMS" && phase !== "Concurrent Cycle"
    },
    columnWidth(column) {
      return column === "name" ? undefined : "100"
    }
  },
  watch: {
    analysisConfig() {
      this.loadData();
    }
  },
  mounted() {
    this.loadData();
  },
  name: "GCMemoryStats"
}
</script>