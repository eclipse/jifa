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
    <div class="time-graph">
      <el-dialog
          :fullscreen="true"
          :visible.sync="fullScreen"
          :close-on-click-modal=false
          :show-close="false"
          @open="enterFullScreen"
          @close="exitFullScreen">
        <div ref="fullCanvas" class="full-canvas"/>
      </el-dialog>
      <div ref="canvas" style="height: 600px"/>
    </div>
  </el-card>
</template>

<script>
import {gclogService, formatTime} from '@/util'
import axios from "axios";
import Hint from "@/components/gclog/Hint";
import * as echarts from "echarts";
import {colors, getIthColor} from "@/components/gclog/ColorUtil";
import {formatTimePeriod} from "@/components/gclog/GCLogUtil";

export default {
  props: ["file", "metadata", "analysisConfig"],
  data() {
    return {
      dataConfig: {},
      fullScreen: false,
      dataLoaded: {},
      name2type: {},
      useUptime: false,
      memoryUnit: 128 * 1024 * 1024, // 128MB
      fixedColor: {
        'Young GC': '#5470c6',
        'Mixed GC': '#fac858',
        'Full GC': '#ee6666',
        'Garbage Collection': '#ee6666',
        'Concurrent Mark Cycle': '#91cc75',
        'Concurrent Undo Cycle': '#ea7ccc',
        'CMS': '#91cc75',
      },
      color: {}
    }
  },
  components: {
    Hint
  },
  methods: {
    getMissingTypes(expectedTypes) {
      return this.allTypes()
          .filter(type => expectedTypes[type] === true)
          .filter(type => this.dataLoaded[type] !== true)
    },
    loadData(expectedTypes) {
      const missingTypes = this.getMissingTypes(expectedTypes)
      if (missingTypes.length === 0) {
        return
      }
      this.getChart().showLoading();
      const requestConfig = {params: {dataTypes: JSON.stringify(missingTypes)}}
      axios.get(gclogService(this.file, 'timeGraphData'), requestConfig).then(resp => {
        const option = {
          series: missingTypes.map(type => {
            const yUnit = this.getCategory(type) === 'memory' ? this.memoryUnit : 1
            return {
              id: type,
              data: resp.data[type].map(d => [this.transformTime(d[0]), d[1] / yUnit])
            }
          })
        }
        const chart = this.getChart()
        chart.setOption(option)
        chart.hideLoading()
        missingTypes.forEach(type => this.dataLoaded[type] = true)
      })
    },
    getChart(canvas) {
      if (canvas === undefined) {
        if (this.fullScreen) {
          canvas = this.$refs.fullCanvas
        } else {
          canvas = this.$refs.canvas
        }
      }
      let chart = echarts.getInstanceByDom(canvas)
      if (!chart) {
        chart = this.initializeChart(canvas)
      }
      return chart;
    },
    initializeChart(canvas) {
      const chart = echarts.init(canvas)
      chart.on('legendselectchanged', (params) => {
        const expectedTypes = {}
        Object.keys(params.selected).forEach(name => expectedTypes[this.name2type[name]] = true)
        this.loadData(expectedTypes)
      })
      window.addEventListener('resize', () => chart.resize());
      return chart
    },
    applyTimeToConfig() {
      const option = this.getChart().getOption()
      const zoom = option.dataZoom.find(e => e.type === 'slider')
      this.$emit('applyTimeToConfig', {
        timeRange: {
          start: this.revertTransformTime(zoom.startValue),
          end: this.revertTransformTime(zoom.endValue)
        }
      })
    },
    generateOptions() {
      let chart = this.getChart()

      const defaultTypes = this.chooseDefaultDataTypes()
      const selected = {}
      Object.keys(defaultTypes).forEach(type => selected[this.typeI18n(type)] = defaultTypes[type])

      let option = {
        color: colors,
        title: {
          text: this.$t('jifa.gclog.timeGraph.timeGraph'),
          left: 'left',
          top: 'top'
        },
        toolbox: {
          feature: {
            myApplyTime: {
              show: true,
              title: this.$t('jifa.gclog.applyTimeToConfig'),
              icon: "path://M512.001535 1023.967254c-281.904485 0-510.385223-229.231845-510.385223-511.967254C1.617335 229.222635 230.096027 0.032746 512.001535 0.032746c281.899368 0 510.38113 229.189889 510.38113 511.967254C1022.382665 794.735409 793.900903 1023.967254 512.001535 1023.967254zM512.001535 96.000448c-229.024114 0-414.667208 186.225355-414.667208 415.999552 0 229.690286 185.643094 415.95555 414.667208 415.95555 229.02002 0 414.665161-186.265264 414.665161-415.95555C926.666696 282.226826 741.021555 96.000448 512.001535 96.000448zM719.355093 559.962362 623.634008 559.962362l-63.754023 0L512.001535 559.962362l0 0c-26.420743 0-47.839565-21.460778-47.839565-47.962362L464.16197 223.972051c0-26.503631 21.418822-48.00534 47.839565-48.00534 26.459628 0 47.87845 21.50171 47.87845 48.00534l0 240.022609 63.754023 0 95.721085 0c26.415626 0 47.834448 21.502733 47.834448 48.00534S745.771743 559.962362 719.355093 559.962362z",
              onclick: this.applyTimeToConfig,
            },
            myFullScreen: {
              show: true,
              title: this.$t('jifa.gclog.timeGraph.fullScreen'),
              icon: "path://M240.8 196l178.4 178.4-45.6 45.6-177.6-179.2-68 68V128h180.8l-68 68z m133.6 408.8L196 783.2 128 715.2V896h180.8l-68-68 178.4-178.4-44.8-44.8zM715.2 128l68 68-178.4 178.4 45.6 45.6 178.4-178.4 68 68V128H715.2z m-65.6 476.8l-45.6 45.6 178.4 178.4-68 68H896V715.2l-68 68-178.4-178.4z",
              onclick: () => {
                this.fullScreen = !this.fullScreen
              },
            },
          }
        },
        legend: {
          orient: 'horizontal',
          left: 'center',
          top: '50px',
          selected: selected
        },
        grid: {
          top: 180,
        },
        xAxis: {
          type: this.useUptime ? 'value' : 'time',
          min: this.transformTime(this.metadata.startTime),
          max: this.transformTime(this.metadata.endTime),
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
            formatter: size => Math.round(size * this.memoryUnit / 1024 / 1024) + 'MB',
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
            id: 'zoom',
            type: 'slider',
            startValue: this.transformTime(this.analysisConfig.timeRange.start),
            endValue: this.transformTime(this.analysisConfig.timeRange.end),
            filterMode: 'none',
            zoomLock: false,
            moveOnMouseMove: false,
            zoomOnMouseWheel: false,
            brushSelect: true,
            labelFormatter: this.formatXAxis,
            dataBackground: {
              lineStyle: {
                opacity: 0
              },
              areaStyle: {
                opacity: 0
              }
            }
          },
          {
            type: "inside",
            zoomOnMouseWheel: true,
            moveOnMouseMove: true
          }
        ],
        series: []
      };

      this.allTypes().forEach(type => {
        const category = this.getCategory(type)
        if (category === 'memory') {
          option.series.push({
            symbol: 'circle',
            symbolSize: 1,
            id: type,
            name: this.typeI18n(type),
            yAxisIndex: 0,
            type: 'line',
            sampling: "lttb",
            color: this.color[type],
            data: []
          })
        } else if (category === 'time') {
          option.series.push({
            symbol: 'circle',
            symbolSize: 6,
            id: type,
            name: this.typeI18n(type),
            yAxisIndex: 1,
            type: 'scatter',
            sampling: "lttb",
            color: this.color[type],
            data: []
          })
        }
      })

      chart.setOption(option);
    },
    typeI18n(type) {
      if (this.getCategory(type) === 'memory') {
        return this.$t('jifa.gclog.timeGraph.' + type)
      } else {
        return this.$t('jifa.gclog.timeGraph.durationOf', {type: type})
      }
    },
    getCategory(type) {
      for (let t in this.dataConfig) {
        if (this.dataConfig[t].indexOf(type) >= 0) {
          return t
        }
      }
      return undefined
    },
    initialize() {
      // decide what series should appear in graph
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
      this.allTypes().forEach(type => this.name2type[this.typeI18n(type)] = type)

      // decide color for each series
      this.color = {};
      const allTypes = ['youngCapacity', 'oldUsed', 'oldCapacity', 'humongousUsed', 'heapUsed', 'heapCapacity',
        'metaspaceUsed', 'metaspaceCapacity', 'reclamation', 'promotion', ...this.metadata.importantEventTypes]
      let i = 0;
      allTypes.forEach(type => {
        if (this.fixedColor.hasOwnProperty(type)) {
          this.color[type] = this.fixedColor[type]
        } else {
          this.color[type] = getIthColor(i)
          i++
        }
      })
    },
    chooseDefaultDataTypes() {
      const result = {};
      this.allTypes().forEach(type => {
        result[type] = false
      });
      ['oldUsed', 'humongousUsed', 'heapUsed', ...this.metadata.mainPauseEventTypes]
          .filter(type => this.getCategory(type) !== undefined)
          .forEach(type => result[type] = true);
      return result
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
    },
    revertTransformTime(time) {
      if (this.useUptime) {
        return time * 1000
      } else {
        return time - this.metadata.timestamp
      }
    },
    enterFullScreen() {
      this.$nextTick(() => {
        const option = this.getChart(this.$refs.canvas).getOption()
        this.getChart(this.$refs.fullCanvas).setOption(option)
      })
    },
    exitFullScreen() {
      const fullChart = this.getChart(this.$refs.fullCanvas);
      const option = fullChart.getOption()
      this.getChart(this.$refs.canvas).setOption(option)
      fullChart.dispose()
    },
    formatXAxis(time) {
      if (this.useUptime) {
        return Math.round(time)
      } else {
        return formatTime(time, 'M-D h:m:s')
      }
    }
  },
  watch: {
    analysisConfig(newValue, oldValue) {
      if (newValue.timeRange.start !== oldValue.timeRange.start ||
          newValue.timeRange.end !== oldValue.timeRange.end) {
        this.getChart().setOption({
          dataZoom: {
            id: 'zoom',
            startValue: this.transformTime(newValue.timeRange.start),
            endValue: this.transformTime(newValue.timeRange.end),
          }
        })
      }
    }
  },
  mounted() {
    this.useUptime = (this.metadata.timestamp < 0)
    this.initialize();
    this.generateOptions()
    this.loadData(this.chooseDefaultDataTypes());
  },
  name: "GCTimeGraph"
}
</script>

<style>
.time-graph .el-dialog {
  position: relative;
}

.time-graph .el-dialog__header {
  padding: 0;
}

.time-graph .el-dialog__body {
  position: absolute;
  height: 100%;
  width: 100%;
  padding: 20px;
}

.time-graph .full-canvas {
  position: absolute;
  height: calc(100% - 40px);
  width: calc(100% - 40px);
}
</style>