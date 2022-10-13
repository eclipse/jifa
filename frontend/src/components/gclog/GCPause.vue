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
  <div>
    <el-card :body-style="{padding:'10px'}">
      <div slot="header">
        <span>{{ $t('jifa.gclog.pauseInfo.pauseInfo') }}</span>
      </div>
      <div>
        <el-table :data="tableData"
                  :loading="loadStats"
                  :show-header="true"
        >
          <el-table-column v-for="column in columns" :key="column">
            <template slot="header">
              <span>{{ $t('jifa.gclog.pauseInfo.' + column) }}</span>
              <Hint :info="getColumnHint(column)"></Hint>
            </template>
            <template slot-scope="scope">
              <ValueWithBadHint :value="scope.row[column].value"
                                :bad="scope.row[column].bad"
                                :badHint="scope.row[column].badHint"/>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div :loading="loadingDistribution" style="height: 400px">
        <div ref="canvas" style="height: 400px"/>
      </div>
    </el-card>
  </div>
</template>

<script>
import {gclogService} from '@/util'
import axios from "axios";
import * as echarts from 'echarts';
import Hint from "@/components/gclog/Hint";
import {formatPercentage, formatTimePeriod} from "@/components/gclog/GCLogUtil";
import ValueWithBadHint from "@/components/gclog/ValueWithBadHint";

export default {
  props: ["file", "metadata", "analysisConfig"],
  data() {
    return {
      loadingStats: true,
      loadingDistribution: true,
      columns: ['throughput', 'pauseAvg', 'pauseMedian', 'pauseP99', 'pauseP999', 'pauseMax'],
      tableData: [],
      distribution: null,
      // colors below are from default colors of ECharts
      fixedColor: {
        'Young GC': '#5470c6',
        'Mixed GC': '#fac858',
        'Full GC': '#ee6666',
      },
      autoColor: [
        '#91cc75',
        '#73c0de',
        '#3ba272',
        '#fc8452',
        '#9a60b4',
        '#ea7ccc']
    }
  },
  components: {
    Hint,
    ValueWithBadHint
  },
  methods: {
    loadStats() {
      this.loadingStats = true
      const requestConfig = {params: {...this.analysisConfig.timeRange}}
      axios.get(gclogService(this.file, 'pauseStatistics'), requestConfig).then(resp => {
        this.tableData = [{
          throughput: {
            value: formatPercentage(resp.data.throughput),
            bad: resp.data.throughput >= 0 && resp.data.throughput <= this.analysisConfig.badThroughputThreshold / 100,
            badHint: this.$t('jifa.gclog.badHint.badThroughput')
          },
          pauseAvg: {
            value: formatTimePeriod(resp.data.pauseAvg),
            bad: resp.data.pauseAvg >= this.analysisConfig.longPauseThreshold,
            badHint: this.$t('jifa.gclog.badHint.badPause')
          },
          pauseMedian: {
            value: formatTimePeriod(resp.data.pauseMedian),
            bad: resp.data.pauseMedian >= this.analysisConfig.longPauseThreshold,
            badHint: this.$t('jifa.gclog.badHint.badPause')
          },
          pauseP99: {
            value: formatTimePeriod(resp.data.pauseP99),
            bad: resp.data.pauseP99 >= this.analysisConfig.longPauseThreshold,
            badHint: this.$t('jifa.gclog.badHint.badPause')
          },
          pauseP999: {
            value: formatTimePeriod(resp.data.pauseP999),
            bad: resp.data.pauseP999 >= this.analysisConfig.longPauseThreshold,
            badHint: this.$t('jifa.gclog.badHint.badPause')
          },
          pauseMax: {
            value: formatTimePeriod(resp.data.pauseMax),
            bad: resp.data.pauseMax >= this.analysisConfig.longPauseThreshold,
            badHint: this.$t('jifa.gclog.badHint.badPause')
          },
        }]
        this.loadingStats = false;
      })
    },
    getColumnHint(column) {
      if (column === 'throughput') {
        return 'jifa.gclog.pauseInfo.' + column + 'Hint'
      }
      return ''
    },
    loadDistribution() {
      this.loadingDistribution = true
      let partitions = this.getPartitions();
      const requestConfig = {
        params: {
          ...this.analysisConfig.timeRange,
          partitions: JSON.stringify(this.getPartitions())
        }
      }
      axios.get(gclogService(this.file, 'pauseDistribution'), requestConfig).then(resp => {
        const canvas = this.$refs.canvas
        let chart = echarts.getInstanceByDom(canvas);
        if (!chart) {
          chart = echarts.init(canvas)
        } else {
          chart.clear();
        }

        const subArrayIndex = []
        out:
            for (let i = 0; i < partitions.length; i++) {
              for (let key of Object.keys(resp.data)) {
                if (resp.data[key][i] !== 0) {
                  subArrayIndex.push(i);
                  continue out
                }
              }
            }

        const yAxisData = this.subArray(partitions.map((value, index) =>
            this.formatYAxis(partitions[index], partitions[index + 1])), subArrayIndex)

        const series = this.metadata.mainPauseEventTypes.filter(e => resp.data.hasOwnProperty(e)).map(name => {
          const series = {
            name: name,
            type: 'bar',
            stack: 'total',
            label: {
              show: true
            },
            emphasis: {
              focus: 'series'
            },
            data: this.subArray(resp.data[name], subArrayIndex).map(v => v === 0 ? undefined : v)
          }
          if (this.fixedColor.hasOwnProperty(name)) {
            series.itemStyle = {
              color: this.fixedColor[name]
            }
          }
          return series
        });
        const option = {
          color: this.autoColor,
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
            }
          },
          legend: {
            orient: 'horizontal',
            left: 'center',
            top: '28px'
          },
          xAxis: {
            type: 'value',
            name: this.$t('jifa.gclog.pauseInfo.pauseCount'),
          },
          yAxis: {
            type: 'category',
            name: this.$t('jifa.gclog.pauseInfo.pauseTime'),
            data: yAxisData
          },
          series: series
        };
        chart.setOption(option);
        this.loadingDistribution = false;
      })
    },
    getPartitions() {
      return [0, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000]
    },
    loadData() {
      this.loadStats()
      this.loadDistribution()
    },
    subArray(array, subArrayIndex) {
      const result = []
      subArrayIndex.forEach(i => result.push(array[i]))
      return result;
    },
    formatYAxis(low, high) {
      if (typeof high === 'undefined') {
        return low >= 1000 ? `>${(low / 1000).toFixed(0)} s` : `>${low} ms`
      } else if (low >= 1000) {
        return `${(low / 1000).toFixed(0)} ~ ${(high / 1000).toFixed(0)}s`
      } else {
        return `${low} ~ ${high} ms`
      }
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
  name: "GCPause"
}
</script>