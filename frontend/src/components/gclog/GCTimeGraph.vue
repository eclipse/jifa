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
  <el-card :body-style="{ padding: '0px' }">
    <div slot="header">
      <span>{{ $t('jifa.gclog.timeGraph.timeGraph') }}</span>
    </div>
    <div>
      <div ref="canvas" style="height: 400px"/>
    </div>
  </el-card>
</template>

<script>
import {toSizeString, gclogService, formatTimePeriod} from '@/util'
import axios from "axios";
import Hint from "@/components/gclog/Hint";
import * as echarts from "echarts";
import {getIthColor} from "@/components/gclog/ColorUtil";

export default {
  props: ["file", "metadata", "analysisConfig"],
  data() {
    return {
      loading: true,

      dataConfig: {},
      dataColor: {},
      dataTypesModel: {},
      data: {},
      useUptime: false,
    }
  },
  components: {
    Hint
  },
  methods: {
    refresh() {
      const missingData = this.getMissingData();
      if (missingData.length > 0) {
        this.loadData(missingData);
      } else {
        this.generateOptions();
      }
    },
    getMissingData() {
      return this.allTypes().filter(e => this.data[e] === undefined)
    },
    loadData(missingData) {
      this.loading = true
      const requestConfig = {params: {dataTypes: JSON.stringify(missingData)}}
      axios.get(gclogService(this.file, 'timeGraphData'), requestConfig).then(resp => {
        Object.assign(this.data, resp.data)
        this.generateOptions()
      })
    },
    generateOptions() {
      const canvas = this.$refs.canvas
      let chart = echarts.getInstanceByDom(canvas);
      if (!chart) {
        chart = echarts.init(canvas)
      } else {
        chart.clear();
      }

      const xInterval = this.useUptime ? 300 : 300000 // 5 min
      const expectedWindowWith = 300000 * 5; // 25 min
      const start = Math.max(Math.min(this.analysisConfig.timeRange.start, this.metadata.endTime - expectedWindowWith),
          this.metadata.startTime)
      const zoomStartValue = this.transformTime(start)
      const zoomEndValue = this.transformTime(Math.min(start + expectedWindowWith, this.metadata.endTime))

      let option = {
        legend: {
          orient: 'horizontal',
          left: 'center',
          top: '28px'
        },
        xAxis: {
          type: this.useUptime ? 'value' : 'time',
          min: this.useUptime ? this.metadata.startTime / 1000 : this.metadata.startTime + this.metadata.timestamp,
          max: this.useUptime ? this.metadata.endTime / 1000 : this.metadata.endTime + this.metadata.timestamp,
          interval: xInterval,
          minInterval: xInterval,
          maxInterval: xInterval,
          minorTick: {
            show: true,
            splitNumber: 5
          }
        },
        yAxis: [{
          type: 'value',
          id: 'memory',
          name: '内存',
          position: 'left',
          z: 0,
          min: 0,
          splitNumber: 10,
          axisLabel: {
            formatter: toSizeString,
          },
          splitLine: {
            show: false
          },
        }, {
          type: 'value',
          name: '时间',
          id: 'time',
          z: 1,
          min: 0,
          splitNumber: 10,
          axisLabel: {
            formatter: formatTimePeriod,
          },
          splitLine: {
            show: false
          },
        }],
        dataZoom: [
          {
            type: 'slider',
            startValue: zoomStartValue,
            endValue: zoomEndValue,
            filterMode: 'none',
            zoomLock: true,
            moveOnMouseMove: false,
            zoomOnMouseWheel: false,
            brushSelect: false
          },
          {
            type: "inside",
            zoomOnMouseWheel: false,
            moveOnMouseWheel: true,
            moveOnMouseMove: true
          }
        ],
        series: []
      };

      Object.keys(this.dataTypesModel).filter(type => this.dataTypesModel[type] === true).forEach(type => {
        const category = this.getCategory(type)
        option.series.push({
          symbol: 'circle',
          symbolSize: category === 'memory' ? 1 : 6,
          name: type,
          yAxisIndex: category === 'memory' ? 0 : 1,
          type: category === 'memory' ? 'line' : 'scatter',
          sampling: "lttb",
          data: this.data[type].map(d => [this.transformTime(d[0]), d[1]])
        })
      })

      console.log(option)

      chart.setOption(option);

      this.loading = false;
    },
    getCategory(type) {
      for (let t in this.dataConfig) {
        if (this.dataConfig[t].indexOf(type) >= 0) {
          return t
        }
      }
      return undefined
    },
    generateDataConfig() {
      const memory = [];
      if (this.metadata.generational) {
        memory.push('youngCapacity', 'oldUsed', 'oldCapacity')
      }
      if (this.metadata.collector === "G1 GC" && this.metadata.logStyle === "unified") {
        memory.push('humongousUsed')
      }
      memory.push('heapUsed', 'heapCapacity', 'metaspaceUsed')
      if (this.metadata.metaspaceCapacityReliable) {
        memory.push('metaspaceCapacity')
      }
      memory.push('reclamation')
      if (this.metadata.generational) {
        memory.push('promotion')
      }
      const time = this.metadata.importantEventTypes
      this.dataConfig = {
        memory: memory,
        time: time
      }
      this.allTypes().forEach((type, index) => {
        this.dataConfig[type] = getIthColor(index)
      })
    },
    choseDefaultDataTypes() {
      const model = {};
      this.allTypes().forEach(type => {
        model[type] = false
      });
      ['oldUsed', 'humongousUsed', 'heapUsed', ...this.metadata.mainPauseEventTypes]
          .filter(type => this.getCategory(type) !== undefined)
          .forEach(type => model[type] = true);
      this.dataTypesModel = model;
    },
    allTypes() {
      return [...this.dataConfig.memory, ...this.dataConfig.time];
    },
    transformTime(time) {
      if (this.useUptime) {
        return time / 1000;
      } else {
        return time + this.metadata.timestamp;
      }
    }
  },
  watch: {
    analysisConfig(newVal, oldVal) {
      if (newVal.timeRange.start !== oldVal.timeRange.start ||
          newVal.timeRange.end !== oldVal.timeRange.end) {
        this.refresh()
      }
    }
  },
  mounted() {
    this.useUptime = (this.metadata.timestamp < 0)
    this.generateDataConfig();
    this.choseDefaultDataTypes();
    this.refresh();
  },
  name: "GCTimeGraph"
}
</script>