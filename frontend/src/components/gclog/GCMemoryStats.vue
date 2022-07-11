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
      <span>{{ $t('jifa.gclog.memoryStats.memoryStats') }}</span>
    </div>
    <el-table :data="data"
              :show-header="true"
              v-loading="loading">
      <el-table-column props="generation" :label="$t('jifa.gclog.memoryStats.memoryArea')">
        <template slot-scope="scope">
          <span>{{ $t(`jifa.gclog.generation.${scope.row.generation}`) }}</span>
          <Hint :info="getGenerationHint(scope.row.generation)"/>
        </template>
      </el-table-column>
      <el-table-column v-for="metric in metrics" :key="metric" props="metric"
                       :label="$t(`jifa.gclog.memoryStats.${metric}`)">
        <template slot-scope="scope">
          <span :class="scope.row[metric].bad ? 'bad-metric' : ''">{{ scope.row[metric].value }}</span>
          <Hint :info="getValueHint(scope.row.generation, metric)"/>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script>
import {toSizeString, gclogService} from '@/util'
import axios from "axios";
import Hint from "@/components/gclog/Hint";

export default {
  props: ["file", "metadata", "analysisConfig"],
  data() {
    return {
      loading: true,
      data: null,
      metrics: null,
    }
  },
  components: {
    Hint
  },
  methods: {
    loadData() {
      this.loading = true
      const requestConfig = {params: {...this.analysisConfig.timeRange}}
      axios.get(gclogService(this.file, 'memoryStatistics'), requestConfig).then(resp => {
        let generations = []
        if (this.metadata.generational) {
          generations.push("young", "old")
        }
        if (this.metadata.collector === "G1 GC" && this.metadata.logStyle === "unified") {
          generations.push("humongous")
        }
        generations.push("heap", "metaspace")

        let metrics = ["capacityAvg", "usedMax", "usedAvgAfterFullGC"]
        if (this.metadata.collector === "G1 GC" || this.metadata.collector === "CMS GC") {
          metrics.push("usedAvgAfterOldGC");
        }
        this.metrics = metrics

        let data = []
        generations.forEach(generation => {
          let item = {}
          metrics.forEach(metric => {
            item[metric] = {
              value: toSizeString(resp.data[generation][metric]),
              bad: this.badValue(resp.data[generation], metric, generation)
            }
          })
          item.generation = generation
          data.push(item)
        })
        this.data = data;
        this.loading = false;
      })
    },
    getValueHint(generation, metric) {
      if (generation === 'metaspace' && metric === 'capacityAvg') {
        return 'jifa.gclog.memoryStats.metaspaceCapacity'
      } else if ((generation === 'old' || generation === 'metaspace')
          && (metric === 'usedAvgAfterFullGC' || metric === 'usedAvgAfterOldGC')) {
        return 'jifa.gclog.memoryStats.leakIfHigh'
      } else if (generation === 'heap' && metric === 'usedMax') {
        return 'jifa.gclog.memoryStats.heapRss'
      }
      return ''
    },
    getGenerationHint(generation) {
      if (generation === 'humongous') {
        return 'jifa.gclog.generation.humongousHint'
      }
      return ''
    },
    badValue(row, metric, generation) {
      if (metric !== 'usedAvgAfterFullGC' && metric !== 'usedAvgAfterOldGC') {
        return false;
      }
      if (row[metric] < 0 || generation < 0) {
        return false;
      }
      const percentage = row[metric] / row.capacityAvg * 100
      return (generation === "old" && percentage >= this.analysisConfig.highOldUsageThreshold) ||
          (generation === "heap" && percentage >= this.analysisConfig.highHeapUsageThreshold) ||
          (generation === "metaspace" && percentage >= this.analysisConfig.highMetaspaceUsageThreshold)
    }
  },
  watch: {
    timeRange() {
      this.loadData();
    }
  },
  mounted() {
    this.loadData();
  },
  name: "GCMemoryStats"
}
</script>