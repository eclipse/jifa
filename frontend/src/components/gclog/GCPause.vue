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
    <el-card>
      <div slot="header">
        <span>{{ $t('jifa.gclog.pauseInfo.pauseInfo') }}</span>
      </div>
      <el-table :data="['dummy data']"
                :loading="loadStats"
                :show-header="true"
      >
        <el-table-column v-for="stat in stats" :key="stat.metric">
          <template slot="header">
            <span>{{ stat.metric }}</span>
            <Hint :info="stat.hint"/>
          </template>
          <template>
            <span :class="stat.bad ? 'bad-metric' : ''">{{ stat.value }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div :loading="loadingDistribution" style="height: 400px">
        <div ref="canvas" style="height: 400px"/>
      </div>

    </el-card>
  </div>
</template>

<script>
import {formatTimePeriod, gclogService} from '@/util'
import axios from "axios";
import * as echarts from 'echarts';
import Hint from "@/components/gclog/Hint";

export default {
  props: ["file", "metadata", "timeRange", 'longPauseThreshold'],
  data() {
    return {
      loadingStats: true,
      loadingDistribution: true,
      stats: null,
      distribution: null,
    }
  },
  components: {
    Hint
  },
  methods: {
    loadStats() {
      this.loadingStats = true
      const requestConfig = {params: {...this.timeRange}}
      axios.get(gclogService(this.file, 'pauseStatistics'), requestConfig).then(resp => {
        this.stats = [
          {
            metric: this.$t('jifa.gclog.pauseInfo.throughput'),
            value: resp.data.throughput < 0 ? "N/A" : (resp.data.throughput * 100).toFixed(2) + "%",
            hint: 'jifa.gclog.pauseInfo.throughputHint',
            bad: resp.data.throughput >= 0 && resp.data.throughput <= 90
          },
          {
            metric: this.$t('jifa.gclog.pauseInfo.pauseAvg'),
            value: formatTimePeriod(resp.data.pauseAvg),
            bad: resp.data.pauseMax >= 0 && resp.data.pauseAvg <= this.longPauseThreshold / 2
          },
          {
            metric: this.$t('jifa.gclog.pauseInfo.pauseMax'),
            value: formatTimePeriod(resp.data.pauseMax),
            bad: resp.data.pauseMax >= 0 && resp.data.pauseMax <= this.longPauseThreshold * 2
          },
        ]
        this.loadingStats = false;
      })
    },
    loadDistribution() {
      this.loadingDistribution = true
      let partitions = this.getPartitions();
      const requestConfig = {
        params: {
          ...this.timeRange,
          partitions: JSON.stringify(this.getPartitions())
        }
      }
      axios.get(gclogService(this.file, 'pauseDistribution'), requestConfig).then(resp => {
        const canvas = this.$refs.canvas
        let chart = echarts.getInstanceByDom(canvas);

        let highPartitions;
        out:
            for (highPartitions = partitions.length - 1; highPartitions >= 0; highPartitions--) {
              for (let key of Object.keys(resp.data)) {
                if (resp.data[key][highPartitions] !== 0) {
                  break out
                }
              }
            }
        partitions = partitions.slice(0, highPartitions)

        if (!chart) {
          chart = echarts.init(canvas)
        } else {
          chart.clear();
        }
        const yAxisData = partitions.map((value, index) =>
            index === partitions.length - 1 ? `>${value}ms` : `${value} ~ ${partitions[index + 1]}ms`
        )
        const series = Object.keys(resp.data).map(key => {
          return {
            name: key,
            type: 'bar',
            stack: 'total',
            label: {
              show: true
            },
            emphasis: {
              focus: 'series'
            },
            data: resp.data[key].map(v => v === 0 ? undefined : v)
          }
        });
        const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
            }
          },
          legend: {
            orient: 'vertical',
            left: 'right',
            top: 'middle'
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
  },
  watch: {
    timeRange() {
      this.loadData();
    }
  },
  mounted() {
    this.loadData();
  },
  name: "GCPause"
}
</script>

<style>
.bad-metric {
  color: #E74C3C;
}
</style>