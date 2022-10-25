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
      <el-table-column v-for="metric in metrics" :key="metric" props="metric" :width="columnWidth(metric)">
        <template slot="header">
          {{ $t(`jifa.gclog.memoryStats.${metric}`) }}
          <Hint :info="getMetricHint(metric)"/>
        </template>
        <template slot-scope="scope">
          <ValueWithBadHint :value="scope.row[metric].value"
                            :bad="scope.row[metric].bad"
                            :badHint="scope.row[metric].badHint"
                            :hint="scope.row[metric].hint"/>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script>
import {gclogService} from '@/util'
import axios from "axios";
import Hint from "@/components/gclog/Hint";
import {formatSize, hasOldGC} from "@/components/gclog/GCLogUtil";
import ValueWithBadHint from "@/components/gclog/ValueWithBadHint";

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
    ValueWithBadHint,
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
          sharedInfo[generation + "CapacityAvg"] = resp.data[generation].capacityAvg
          let item = {}
          metrics.forEach(metric => {
            item[metric] = {
              value: formatSize(resp.data[generation][metric]),
              hint: this.getValueHint(generation, metric),
              ...this.badValueCheck(resp.data[generation], metric, generation, heapCapacity)
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
    badValueCheck(row, metric, generation, heapCapacity) {
      if (metric === 'usedAvgAfterFullGC' || metric === 'usedAvgAfterOldGC') {
        // check leak
        if (row[metric] < 0 || row.capacityAvg < 0) {
          return {bad: false}
        }
        const percentage = row[metric] / row.capacityAvg * 100
        return {
          bad: (generation === "old" && percentage >= this.analysisConfig.highOldUsageThreshold) ||
              (generation === "humongous" && percentage >= this.analysisConfig.highHumongousUsageThreshold) ||
              (generation === "heap" && percentage >= this.analysisConfig.highHeapUsageThreshold) ||
              (generation === "metaspace" && percentage >= this.analysisConfig.highMetaspaceUsageThreshold),
          badHint: this.$t('jifa.gclog.badHint.badUsageAfterGC')
        }
      } else if (metric === 'capacityAvg' && (generation === 'young' || generation === 'old')) {
        // check small generation
        if (row[metric] < 0 || heapCapacity < 0) {
          return {bad: false}
        }
        const percentage = row[metric] / heapCapacity * 100
        return {
          bad: percentage < this.analysisConfig.smallGenerationThreshold,
          badHint: this.$t(`jifa.gclog.badHint.${generation}TooSmall`)
        }
      }
    },
    getMetricHint(metric) {
      const hint = [this.$t(`jifa.gclog.memoryStats.${metric}Hint`)]
      if (this.metadata.collector === 'G1 GC' && metric === 'capacityAvg') {
        hint.push(this.$t('jifa.gclog.memoryStats.g1DynamicCapacity'))
      }
      return hint
    },
    columnWidth(column) {
      return column.startsWith("usedAvgAfter") ? 200 : 150
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