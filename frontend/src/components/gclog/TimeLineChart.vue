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
  <div class="box" :loading = "loading">
    <h5>{{this.title}}</h5>
    <div style="text-align: center" v-show="noData">{{$t('jifa.gclog.graphs.noData')}}</div>
    <div ref="canvas"  class="canvas" v-show="!noData"></div>
  </div>
</template>

<script>
/* eslint-disable */
import axios from "axios";
import i18n from "@/i18n/i18n-setup";
import * as echarts from 'echarts';
import {gclogService} from "@/util"

export default {
  components: {},
  props: ['file', 'title', 'referenceTimestamp', 'timeSpan', 'timePoint', 'type'],
  data() {
    return {
      loading: false,
      noData: false,
    }
  },
  methods: {
    initialize() {
      const canvas = this.$refs.canvas;

      function getCanvasHeight() {
        const aspectRatio = 16 / 7
        return parseFloat(canvas.offsetWidth) / aspectRatio
      }

      const chart = echarts.init(canvas, undefined, {height: getCanvasHeight()});
      window.addEventListener('resize', () => chart.resize({height: getCanvasHeight()}));
      return chart;
    },
    fetchDataAndUpdate() {
      this.loading = true;
      const params = {
        params: {
          type: this.type,
          timeSpan: this.timeSpan,
          timePoint: this.timePoint
        }
      }
      axios.get(gclogService(this.file, 'graph'), params).then(resp => {
        const data = resp.data;
        if (data.dataByTimes.length === 0) {
          this.noData = true
          this.loading = false
          return
        }
        let option = {
          toolbox: {
            feature: {
              dataZoom: {
                show: false,
                yAxisIndex: 'none'
              },
            }
          },
          legend: {
            data: [],
            top: 'bottom'
          },
          tooltip: {
            trigger: 'axis'
          },
          xAxis: {
            type: this.referenceTimestamp < 0 ? 'value' : 'time',
            min: this.referenceTimestamp < 0 ? data.startTime / 1000 : data.startTime + this.referenceTimestamp,
            max: this.referenceTimestamp < 0 ? data.endTime / 1000 : data.endTime + this.referenceTimestamp,
          },
          yAxis: {
            type: 'value',
            min: 0,
          },
          series: []
        }
        this.noData = false;
        data.dataByTimes.forEach(seriesData => {
          const label = seriesData.label.startsWith('jifa') ? i18n.t(seriesData.label) : seriesData.label
          option.legend.data.push(label)
          option.series.push({
            symbol: 'circle',
            name: label,
            type: "line",
            sampling: "lttb",
            data: Object.keys(seriesData.data)
                .map(time => {
                  if (this.referenceTimestamp >= 0) {
                    return [parseFloat(time) + this.referenceTimestamp, seriesData.data[time].toFixed(2)]
                  } else {
                    return [parseFloat(time) / 1000, seriesData.data[time].toFixed(2)]
                  }
                })
                .sort((d1, d2) => d1[0] - d2[0])
          })
        })
        let chart = echarts.getInstanceByDom(this.$refs.canvas);
        if (!chart) {
          chart = this.initialize();
        } else {
          chart.clear();
        }
        chart.setOption(option);
        this.loading = false;
      });
    }
  },
  watch: {
    timeSpan: function (newValue, oldValue) {
      if (newValue !== oldValue) {
        this.fetchDataAndUpdate()
      }
    },
    timePoint: function (newValue, oldValue) {
      if (newValue !== oldValue) {
        this.fetchDataAndUpdate()
      }
    },
  },
  created() {
    this.fetchDataAndUpdate()
  }
}
</script>

<style scoped>
.canvas{
  width: 100%;
}
</style>
