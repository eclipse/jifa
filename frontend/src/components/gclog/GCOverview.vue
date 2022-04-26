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
    <el-col :span="18" :offset="3">
      <el-collapse v-model="activeNames" style="width: 100%;">
        <el-collapse-item v-loading="loadingProblemSolution" :title="$t('jifa.gclog.gcOverview.diagnosis')" name="3">
          <el-table
              :data="problemAndSuggestions"
              :show-header="true"
              :highlight-current-row="false"
              stripe
          >
            <el-table-column prop="problem" width="600px" :label="$t('jifa.gclog.gcOverview.problems')">
              <template slot-scope="scope">
                {{ (1 + scope.$index) + '. ' + scope.row.problem }}
              </template>
            </el-table-column>
            <el-table-column prop="suggestions" :label="$t('jifa.gclog.gcOverview.suggestions')">
              <template slot-scope="scope">
                <p v-for="(line,index) of scope.row.suggestions" :key="index"
                   v-html="(1 + index) + '. ' + line"
                ></p>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>

        <el-collapse-item v-loading="loadingBasic" :title="$t('jifa.gclog.gcOverview.basicInfo')" name="1">
          <el-table
              :data="basic"
              :show-header="false"
              :highlight-current-row="false"
              stripe
          >
            <el-table-column prop="key" width="600px">
            </el-table-column>
            <el-table-column prop="value">
            </el-table-column>
          </el-table>
        </el-collapse-item>

        <el-collapse-item v-loading="loadingKPI" :title="$t('jifa.gclog.gcOverview.kpi')" name="2">
          <el-table
              :data="kpi"
              :show-header="false"
              :highlight-current-row="false"
              stripe
          >
            <el-table-column width="600px">
              <template slot-scope="scope">
                <span>{{$t(scope.row.key)}}</span>
                <el-tooltip v-if="scope.row.hint"
                            effect="dark"
                            :content="$t(scope.row.hint)"
                            placement="top-start">
                  <i class="el-icon-question"></i>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column>
              <template slot-scope="scope">
                <span :class="scope.row.bad ?'bad-kpi' : ''">{{scope.row.value}}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>

      </el-collapse>
    </el-col>
  </div>
</template>

<script>
  import axios from "axios";
  import {formatTimePeriod, gclogService, toSizeSpeedString, toSizeString} from "@/util";

  export default {
    props: ['file'],
    components: {},
    data() {
      return {
        activeNames: ['1', '2', '3'],
        loadingBasic: false,
        loadingKPI: false,
        loadingProblemSolution: false,
        problemAndSuggestions: null,
        basic: null,
        kpi: null,
      }
    },
    methods: {
      transformBasic(obj) {
        const keyMap = {
          vmOptions: this.$t('jifa.gclog.gcOverview.vmOptions'),
          collector: this.$t('jifa.gclog.gcOverview.collector'),
          duration: this.$t('jifa.gclog.gcOverview.duration'),
          heapSize: this.$t('jifa.gclog.gcOverview.heapSize'),
          youngGenSize: this.$t('jifa.gclog.gcOverview.youngGenSize'),
          oldGenSize: this.$t('jifa.gclog.gcOverview.oldGenSize'),
          metaspaceSize: this.$t('jifa.gclog.gcOverview.metaspaceSize'),
          heapRegionSize: "Heap Region Size",
        };
        if (this.collector !== "G1 GC") {
          delete keyMap.heapRegionSize
        }
        if (!this.collectorGenerational() || this.collector === "G1 GC") {
          delete keyMap.youngGenSize
          delete keyMap.oldGenSize
        }
        const valueTransformers = {
          duration: formatTimePeriod,
          heapSize: this.formatKB,
          youngGenSize: this.formatKB,
          oldGenSize: this.formatKB,
          metaspaceSize: this.formatKB,
          heapRegionSize: this.formatKB,
        }
        return Object.keys(keyMap).filter(key => obj.hasOwnProperty(key))
            .map(key => {
              const v = obj[key]
              return {
                key: keyMap.hasOwnProperty(key) ? keyMap[key] : key,
                value: valueTransformers.hasOwnProperty(key) ? valueTransformers[key](v) : v
              }
            })
      },
      collectorGenerational() {
        return this.collector !== "ZGC"
      },
      loadBasic() {
        this.loadingBasic = true;
        axios.get(gclogService(this.file, 'overview/basic')).then(resp => {
          this.collector = resp.data.collector
          this.basic = this.transformBasic(resp.data)
          this.loadingBasic = false
          this.loadKPI()
          this.loadProblemSolution()
        })
      },
      formatPercentage(v) {
        return v == null || v < 0 ? "N/A" : (100 * v).toFixed(2) + "%";
      },
      formatKB(v) {
        return v < 0 ? "N/A" : toSizeString(v * 1024);
      },
      formatKBPS(v) {
        return v < 0 ? "N/A" : toSizeSpeedString(v * 1024);
      },

      loadKPI() {
        this.loadingKPI = true;
        axios.get(gclogService(this.file, 'overview/kpi')).then(resp => {
          const kpiConfig = {
            throughput: {
              key: ["throughput"],
              keyToDisplay: 'jifa.gclog.gcOverview.throughput',
              transformer: this.formatPercentage,
              hint: 'jifa.gclog.gcOverview.throughputHint',
            },
            gcDurationPercentage: {
              key: ["gcDurationPercentage"],
              keyToDisplay: 'jifa.gclog.gcOverview.gcDurationPercentage',
              transformer: this.formatPercentage,
            },
            maxPause: {
              key: ["maxPause"],
              keyToDisplay: 'jifa.gclog.gcOverview.maxPause',
              transformer: formatTimePeriod,
            },
            youngGCInterval: {
              key: ["youngGCIntervalAvg", "youngGCIntervalMin"],
              keyToDisplay: 'jifa.gclog.gcOverview.youngGCIntervalAvgMin',
              transformer: formatTimePeriod,
            },
            youngGCPause: {
              key: ["youngGCPauseAvg", "youngGCPauseMax"],
              keyToDisplay: 'jifa.gclog.gcOverview.youngGCPauseAvgMax',
              transformer: formatTimePeriod,
            },
            oldGCInterval: {
              key: ["oldGCIntervalAvg", "oldGCIntervalMin"],
              keyToDisplay: 'jifa.gclog.gcOverview.oldGCIntervalAvgMin',
              transformer: formatTimePeriod,
            },
            fullGCInterval: {
              key: ["fullGCIntervalAvg", "fullGCIntervalMin"],
              keyToDisplay: 'jifa.gclog.gcOverview.fullGCIntervalAvgMin',
              transformer: formatTimePeriod,
            },
            fullGCPause: {
              key: ["fullGCPauseAvg", "fullGCPauseMax"],
              keyToDisplay: 'jifa.gclog.gcOverview.fullGCPauseAvgMax',
              transformer: formatTimePeriod,
            },
            promotion: {
              key: ["promotionAvg", "promotionMax"],
              keyToDisplay: 'jifa.gclog.gcOverview.promotionAvgMax',
              transformer: this.formatKB,
            },
            promotionSpeed: {
              key: ["promotionSpeed"],
              keyToDisplay: 'jifa.gclog.gcOverview.promotionSpeed',
              transformer: this.formatKBPS,
            },
            objectCreationSpeed: {
              key: ["objectCreationSpeed"],
              keyToDisplay: 'jifa.gclog.gcOverview.objectCreationSpeed',
              transformer: this.formatKBPS,
            },
          }

          this.kpi = Object.keys(kpiConfig).filter(key => {
            return resp.data.hasOwnProperty(kpiConfig[key].key[0])
          }).map(key => {
            const config = kpiConfig[key]
            return {
              key: config.keyToDisplay,
              value: config.key.map(k => config.transformer(resp.data[k].value)).join(" / "),
              hint: config.hint,
              bad: config.key.reduce((prev, nextKey) => prev && resp.data[nextKey].bad, true)
            }
          })
          this.loadingKPI = false;
        })
      },
      loadProblemSolution() {
        this.loadingProblemSolution = true;
        axios.get(gclogService(this.file, 'overview/problem')).then(resp => {
          if (resp.data.length === 0) {
            this.problemAndSuggestions = [{
              problem: this.$t('jifa.gclog.diagnosis.problems.noProblem'),
              suggestions: []
            }]
          } else {
            this.problemAndSuggestions = resp.data.map(item => {
              return {
                problem: this.$t(item.problem.name, item.problem.params),
                suggestions: item.suggestions.map(suggestionItem => this.$t(suggestionItem.name, suggestionItem.params))
              }
            })
          }
          this.loadingProblemSolution = false;
        })
      }
    },
    computed: {},
    created() {
      this.loadBasic()
    }

  }
</script>

<style scoped>
  .bad-kpi{
    color: #E74C3C;
  }
</style>
