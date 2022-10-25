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
  <el-card :body-style="{padding: 0}">
    <div slot="header">
      <span>{{ $t('jifa.gclog.diagnose.diagnose') }}</span>
    </div>
    <div>
      <div v-if="noProblem" style="padding: 20px"> {{ $t('jifa.gclog.diagnose.noProblem') }}</div>
      <div v-else>
        <div style="height: 150px" ref="canvas"/>
        <hr style="margin: 0"/>
        <div style="padding: 20px">
          <p style="font-weight: bold">
            <i class="el-icon-warning"/>
            <template v-for="part in problem">
              <a v-if="part.action"
                 href="javascript:void(0);"
                 @click="part.action"
                 style="display: inline"
              >{{ part.text }}</a>
              <span v-else>{{ part.text }}</span>
            </template>
          </p>
          <p>{{ $t('jifa.gclog.diagnose.solution') }}</p>
          <p v-for="suggestion in suggestions" :key="suggestion">{{ suggestion }}</p>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script>
import {formatTime, gclogService} from '@/util'
import axios from "axios";
import Hint from "@/components/gclog/Hint";
import {formatTimeRange} from "@/components/gclog/GCLogUtil";
import * as echarts from "echarts";
import {colors} from "@/components/gclog/ColorUtil";

export default {
  props: ["file", "metadata", "analysisConfig"],
  data() {
    return {
      loading: true,
      noProblem: true,
      sites: [],
      problem: [],
      suggestions: [],
      useUptime: false,
    }
  },
  components: {
    Hint
  },
  methods: {
    loadData() {
      this.loadingStats = true
      const requestConfig = {params: {config: this.analysisConfig}}
      axios.get(gclogService(this.file, 'diagnoseInfo'), requestConfig).then(resp => {
        this.noProblem = Object.keys(resp.data.seriousProblems).length === 0;
        if (this.noProblem) {
          if (this.$refs.canvas) {
            const chart = echarts.getInstanceByDom(this.$refs.canvas)
            if (chart) {
              chart.dispose()
            }
          }
          return;
        }
        this.dealMostSeriousProblem(resp.data.mostSeriousProblem)
        this.dealGraph(resp.data.seriousProblems);
        this.loading = false;
      })
    },
    dealMostSeriousProblem(mostSerious) {
      this.sites = mostSerious.sites
      this.suggestions = mostSerious.suggestions.map(
          (suggestion, index) => (index + 1) + '. ' + this.$t(suggestion.name, suggestion.params)
      )
      /* To support generating clickable substring in a dynamically generated string, we create a simple DSL.
       * Format like $$A|B|C|D$$ will be transformed to text A, and when A is clicked, function this[B] will be called
       * with argument C, D etc. All arguments are in string type.
       */
      const time = mostSerious.sites
          .map(range => formatTimeRange(range.start, range.end, this.metadata.timestamp))
          .map((s, index) => `$$${s}|setTimeRangeByIndex|${index}$$`)
          .join(', ')
      const problem = this.$t(mostSerious.problem.name, mostSerious.problem.params)
      const string = this.$t('jifa.gclog.diagnose.problemTemplate', {time: time, problem: problem})
      this.problem = this.parseStringWithAction(string)
    },
    dealGraph(seriousProblems) {
      this.$nextTick(() => {
        const canvas = this.$refs.canvas
        let chart = echarts.getInstanceByDom(canvas);
        if (!chart) {
          chart = echarts.init(canvas)
          const option = {
            color: colors,
            legend: {
              top: 10,
              orient: 'horizontal',
            },
            xAxis: {},
            yAxis: {
              type: 'value',
              show: false,
              min: 0,
              max: 1,
            },
            series: []
          };
          chart.setOption(option)
        }
        const option = {
          xAxis: {
            type: this.useUptime ? 'value' : 'time',
            min: this.transformTime(this.analysisConfig.timeRange.start),
            max: this.transformTime(this.analysisConfig.timeRange.end),
          },
          series: Object.keys(seriousProblems).map((type, index) => {
            return {
              symbol: 'circle',
              symbolSize: 5,
              name: this.$t('jifa.gclog.diagnose.abnormal.' + type),
              type: 'scatter',
              sampling: "lttb",
              data: seriousProblems[type].map(x => [this.transformTime(x), index * 0.2])
            }
          })
        }
        chart.setOption(option, {replaceMerge: ['xAxis', 'series']})
      })
    },
    parseStringWithAction(s) {
      // Currently, two $$ can not be adjacent and $$ can not be at the beginning. Change the logic below
      // when the assumption is broken.
      return s.split('$$')
          .map((split, index) => {
            if (index % 2 === 0) {
              return {text: split}
            } else {
              const parts = split.split('|')
              return {
                text: parts[0],
                action: () => {
                  this[parts[1]](parts[2], parts[3], parts[4], parts[5], parts[6], parts[7], parts[8])
                }
              }
            }
          })
    },
    transformTime(time) {
      if (this.useUptime) {
        return time / 1000;
      } else {
        return time + this.metadata.timestamp;
      }
    },
    setTimeRangeByIndex(index) {
      const timeRange = this.sites[parseInt(index)]
      const config = {timeRange: timeRange}
      this.$emit('applyTimeToConfig', config)
    }
  },
  watch: {
    analysisConfig() {
      this.loadData();
    }
  },
  mounted() {
    this.useUptime = this.metadata.timestamp < 0
    this.loadData();
  },
  name: "GCMemoryStats"
}
</script>