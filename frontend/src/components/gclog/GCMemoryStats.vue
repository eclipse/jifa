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
      <el-table-column v-for="metric in metrics" :key="metric" props="metric">
        <template slot="header" slot-scope="scope">
          {{ $t(`jifa.gclog.memoryStats.${metric}`) }}
          <Hint :info="getMetricHint(metric)"/>
        </template>
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
import {hasOldGC} from "@/components/gclog/GCLogUtil";

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
          generations.push("young")
        }
        if (hasOldGC(this.metadata)) {
          generations.push("old")
        }
        if (this.metadata.collector === "G1 GC" && this.metadata.logStyle === "unified") {
          generations.push("humongous")
        }
        generations.push("heap", "metaspace")

        let metrics = ["capacityAvg", "usedMax", "usedAvgAfterFullGC"]
        if (hasOldGC(this.metadata)) {
          metrics.push("usedAvgAfterOldGC");
        }
        this.metrics = metrics

        let data = []
        const sharedInfo = {}
        const heapCapacity = resp.data.heap.capacityAvg
        generations.forEach(generation => {
          sharedInfo[generation+"CapacityAvg"] = resp.data[generation].capacityAvg
          let item = {}
          metrics.forEach(metric => {
            item[metric] = {
              value: toSizeString(resp.data[generation][metric]),
              bad: this.badValue(resp.data[generation], metric, generation, heapCapacity)
            }
          })
          item.generation = generation
          data.push(item)
        })
        this.$emit('saveSharedInfo', sharedInfo)
        this.data = data;
        this.loading = false;
      })
    },
    getValueHint(generation, metric) {
      if (generation === 'metaspace' && metric === 'capacityAvg' && !this.metadata.metaspaceCapacityReliable) {
        return 'jifa.gclog.memoryStats.metaspaceCapacity'
      }
      return ''
    },
    getGenerationHint(generation) {
      if (generation === 'humongous') {
        return 'jifa.gclog.generation.humongousHint'
      }
      return ''
    },
    badValue(row, metric, generation, heapCapacity) {
      if (metric === 'usedAvgAfterFullGC' || metric === 'usedAvgAfterOldGC') {
        // check leak
        if (row[metric] < 0 || row.capacityAvg < 0) {
          return false;
        }
        const percentage = row[metric] / row.capacityAvg * 100
        return (generation === "old" && percentage >= this.analysisConfig.highOldUsageThreshold) ||
            (generation === "humongous" && percentage >= this.analysisConfig.highHumongousUsageThreshold) ||
            (generation === "heap" && percentage >= this.analysisConfig.highHeapUsageThreshold) ||
            (generation === "metaspace" && percentage >= this.analysisConfig.highMetaspaceUsageThreshold)
      } else if (metric === 'capacityAvg' && (generation === 'young' || generation === 'old')) {
        // check small generation
        if (row[metric] < 0 || heapCapacity < 0) {
          return false;
        }
        const percentage = row[metric] / heapCapacity * 100
        return percentage < this.analysisConfig.smallGenerationThreshold
      }
    },
    getMetricHint(metric) {
      const hint = [this.$t(`jifa.gclog.memoryStats.${metric}Hint`)]
      if (this.metadata.collector === 'G1 GC' && metric === 'capacityAvg') {
        hint.push(this.$t('jifa.gclog.memoryStats.g1DynamicCapacity'))
      }
      return hint
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