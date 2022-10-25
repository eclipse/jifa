<!--
    Copyright (c) 2022 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<!--suppress HtmlUnknownTag -->
<template>
  <el-container>
    <el-header>
      <view-menu subject="gcLogCompare"
                 :analysisState="analysisState"
                 type="GC_LOG_COMPARE"/>
    </el-header>

    <el-main style="padding-top: 0">

      <div style="padding-top: 20px" v-if="analysisState === 'IN_PROGRESS' || analysisState === 'ERROR'">
        <b-progress height="2rem" show-progress :precision="2"
                    :value="progress"
                    :variant="progressState"
                    striped
                    :animated="progress < 100"></b-progress>
        <b-card class="mt-3" bg-variant="dark" text-variant="white" v-if="message">
          <b-card-text style="white-space: pre-line;">{{ message }}</b-card-text>
          <div class="d-flex justify-content-center mb-3" v-if="progressState === 'info'">
            <b-spinner></b-spinner>
          </div>
        </b-card>
      </div>

      <div v-if="analysisState === 'SUCCESS'" style="padding: 10px 5px 5px; width: 1200px; margin: 0 auto">
        <el-table :data="tableData"
                  :show-header="true"
                  row-key="metric"
                  :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
                  default-expand-all
                  :loading="loading || pendingRequest>=0">
          <el-table-column prop="metric" :label="$t('jifa.gclog.metric')" width="250">
            <template slot-scope="scope">
              <span>{{ scope.row.metric }}</span>
              <Hint :info="scope.row.metricHint"/>
            </template>
          </el-table-column>
          <el-table-column v-for="i in fileCount" :key="i" :prop="'value' + (i-1)">
            <template slot="header">
              <a href="javascript:void(0);" @click="()=> {clickTitle(i - 1)}">
                <span>{{ i === 1 ? $t('jifa.gclog.baselineFile') : $t('jifa.gclog.targetFile') }}</span>
              </a>
            </template>
          </el-table-column>
          <el-table-column prop="compare" width="150">
            <template slot="header">
              <div v-html="$t('jifa.gclog.metricCompare')"></div>
            </template>
            <template slot-scope="scope">
              <span :class="scope.row.compareClass">{{ scope.row.compare }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-main>
  </el-container>
</template>

<script>
import axios from 'axios'
import {gclogService, service} from '../../util'
import ViewMenu from "../menu/ViewMenu";
import {
  formatPercentage, formatSize, formatSizeSpeed, formatTimePeriod,
  formatTimeRange, getUrlParams, hasConcurrentGCThreads,
  hasOldGC,
  hasParallelGCThreads,
  isFullGC,
  isOldGC,
  isYoungGC
} from "@/components/gclog/GCLogUtil";
import Hint from "@/components/gclog/Hint";

export default {
  props: ["file0", 'start0', 'end0', 'file1', 'start1', 'end1'],
  data() {
    return {
      type: 'GC_LOG',
      analysisState: 'NOT_STARTED',
      progressState: 'info',
      message: '',
      progress: 0,
      pollingInternal: 500,

      loading: false,
      pendingRequest: 0,

      fileCount: 2,
      metadata: [null, null],
      files: [null, null],
      analysisConfig: [{}, {}],
      originalData: [{}, {}],
      tableData: [],
      displayConfig: [ // use array to preserve the order of keys
        {
          key: 'basicInfo', // for getting data from original data
          name: 'basicInfo', // for i18n display in table, will add jifa.gclog in the front
          children: [
            {
              key: 'name',
              name: 'gclogFile',
              format: this.formatString,
              compare: "don't compare",
            },
            {
              key: 'logTimeRange', // for getting data from original data
              name: 'logTimeRange', // for i18n display in table, will add jifa.gclog in the front
              // hint: '' // // for the hint of metric, will add jifa.gclog in the front
              format: this.formatString, // how the value will be displayed
              compare: "don't compare", // enum of "don't compare", "just compare", "the more the better", "the less the better"
              // needed: (metadata => true) // is this metric needed?
            },
            {
              key: 'analysisTimeRange',
              name: 'analysisTimeRange',
              format: this.formatString,
              compare: "don't compare",
            },
            {
              key: 'analysisTimeRangeLength',
              name: 'analysisTimeRangeLength',
              format: formatTimePeriod,
              compare: "just compare",
            },
            {
              key: 'collector',
              name: 'collector',
              format: this.formatString,
              compare: "don't compare",
            },
            {
              key: 'parallelGCThreads',
              name: 'parallelGCThreads',
              format: this.formatCount,
              compare: "just compare",
              needed: hasParallelGCThreads
            }, {
              key: 'concurrentGCThreads',
              name: 'concurrentGCThreads',
              format: this.formatCount,
              compare: "just compare",
              needed: hasConcurrentGCThreads
            }],
        },
        {
          key: 'pauseStatistics',
          name: 'pauseInfo.pauseInfo',
          children: [
            {
              key: 'throughput',
              name: 'pauseInfo.throughput',
              format: formatPercentage,
              compare: "the more the better",
            },
            {
              key: 'pauseAvg',
              name: 'pauseInfo.pauseAvg',
              format: formatTimePeriod,
              compare: "the less the better",
            },
            {
              key: 'pauseMedian',
              name: 'pauseInfo.pauseMedian',
              format: formatTimePeriod,
              compare: "the less the better",
            },
            {
              key: 'pauseP99',
              name: 'pauseInfo.pauseP99',
              format: formatTimePeriod,
              compare: "the less the better",
            },
            {
              key: 'pauseP999',
              name: 'pauseInfo.pauseP999',
              format: formatTimePeriod,
              compare: "the less the better",
            },
            {
              key: 'pauseMax',
              name: 'pauseInfo.pauseMax',
              format: formatTimePeriod,
              compare: "the less the better",
            },
          ],
        },
        {
          key: 'memoryStatistics',
          name: 'memoryStats.memoryStats',
          children: [
            {
              key: 'youngCapacityAvg',
              name: 'memoryStats.youngCapacityAvg',
              format: formatSize,
              compare: "just compare",
              needed: this.isGenerational
            },
            {
              key: 'youngUsedMax',
              name: 'memoryStats.youngUsedMax',
              format: formatSize,
              compare: "just compare",
              needed: this.isGenerational
            },
            {
              key: 'oldCapacityAvg',
              name: 'memoryStats.oldCapacityAvg',
              format: formatSize,
              compare: "just compare",
              needed: this.isGenerational
            },
            {
              key: 'oldUsedMax',
              name: 'memoryStats.oldUsedMax',
              format: formatSize,
              compare: "the less the better",
              needed: this.isGenerational
            },
            {
              key: 'oldUsedAvgAfterFullGC',
              name: 'memoryStats.oldUsedAvgAfterFullGC',
              format: formatSize,
              compare: "the less the better",
              needed: this.isGenerational
            },
            {
              key: 'oldUsedAvgAfterOldGC',
              name: 'memoryStats.oldUsedAvgAfterOldGC',
              format: formatSize,
              compare: "the less the better",
              needed: hasOldGC
            },
            {
              key: 'humongousUsedMax',
              name: 'memoryStats.humongousUsedMax',
              format: formatSize,
              compare: "the less the better",
              needed: this.isG1
            },
            {
              key: 'humongousUsedAvgAfterFullGC',
              name: 'memoryStats.humongousUsedAvgAfterFullGC',
              format: formatSize,
              compare: "the less the better",
              needed: this.isG1
            },
            {
              key: 'humongousUsedAvgAfterOldGC',
              name: 'memoryStats.humongousUsedAvgAfterOldGC',
              format: formatSize,
              compare: "the less the better",
              needed: this.isG1
            },
            {
              key: 'heapCapacityAvg',
              name: 'memoryStats.heapCapacityAvg',
              format: formatSize,
              compare: "just compare",
            },
            {
              key: 'heapUsedMax',
              name: 'memoryStats.heapUsedMax',
              format: formatSize,
              compare: "the less the better",
            },
            {
              key: 'heapUsedAvgAfterFullGC',
              name: 'memoryStats.heapUsedAvgAfterFullGC',
              format: formatSize,
              compare: "the less the better",
            },
            {
              key: 'heapUsedAvgAfterOldGC',
              name: 'memoryStats.heapUsedAvgAfterOldGC',
              format: formatSize,
              compare: "the less the better",
              needed: hasOldGC
            },
            {
              key: 'metaspaceCapacityAvg',
              name: 'memoryStats.metaspaceCapacityAvg',
              format: formatSize,
              compare: "just compare",
            },
            {
              key: 'metaspaceUsedMax',
              name: 'memoryStats.metaspaceUsedMax',
              format: formatSize,
              compare: "the less the better",
            },
            {
              key: 'metaspaceUsedAvgAfterFullGC',
              name: 'memoryStats.metaspaceUsedAvgAfterFullGC',
              format: formatSize,
              compare: "the less the better",
            },
            {
              key: 'metaspaceUsedAvgAfterOldGC',
              name: 'memoryStats.metaspaceUsedAvgAfterOldGC',
              format: formatSize,
              compare: "the less the better",
              needed: hasOldGC
            },
          ],
        },
        {
          key: 'phaseStatistics',
          name: 'phaseStats.phaseStats',
          children: [
            {
              key: 'youngGCCount',
              name: 'phaseStats.youngGCCount',
              format: this.formatCount,
              compare: "the less the better",
              needed: this.isGenerational,
            },
            {
              key: 'youngGCIntervalAvg',
              name: 'phaseStats.youngGCIntervalAvg',
              format: formatTimePeriod,
              compare: "the more the better",
              needed: this.isGenerational,
            },
            {
              key: 'youngGCDurationAvg',
              name: 'phaseStats.youngGCDurationAvg',
              format: formatTimePeriod,
              compare: "the less the better",
              needed: this.isGenerational,
            },
            {
              key: 'youngGCDurationMax',
              name: 'phaseStats.youngGCDurationMax',
              format: formatTimePeriod,
              compare: "the less the better",
              needed: this.isGenerational,
            },
            {
              key: 'mixedGCCount',
              name: 'phaseStats.mixedGCCount',
              format: this.formatCount,
              compare: "the less the better",
              needed: this.isG1,
            },
            {
              key: 'mixedGCIntervalAvg',
              name: 'phaseStats.mixedGCIntervalAvg',
              format: formatTimePeriod,
              compare: "the more the better",
              needed: this.isG1,
            },
            {
              key: 'mixedGCDurationAvg',
              name: 'phaseStats.mixedGCDurationAvg',
              format: formatTimePeriod,
              compare: "the less the better",
              needed: this.isG1,
            },
            {
              key: 'mixedGCDurationMax',
              name: 'phaseStats.mixedGCDurationMax',
              format: formatTimePeriod,
              compare: "the less the better",
              needed: this.isG1,
            },
            {
              key: 'oldGCCount',
              name: 'phaseStats.oldGCCount',
              format: this.formatCount,
              compare: "the less the better",
              needed: hasOldGC,
            },
            {
              key: 'oldGCIntervalAvg',
              name: 'phaseStats.oldGCIntervalAvg',
              format: formatTimePeriod,
              compare: "the more the better",
              needed: hasOldGC,
            },
            {
              key: 'oldGCDurationAvg',
              name: 'phaseStats.oldGCDurationAvg',
              format: formatTimePeriod,
              compare: "the less the better",
              needed: hasOldGC,
            },
            {
              key: 'oldGCDurationMax',
              name: 'phaseStats.oldGCDurationMax',
              format: formatTimePeriod,
              compare: "the less the better",
              needed: hasOldGC,
            },
            {
              key: 'fullGCCount',
              name: 'phaseStats.fullGCCount',
              format: this.formatCount,
              compare: "the less the better",
            },
            {
              key: 'fullGCIntervalAvg',
              name: 'phaseStats.fullGCIntervalAvg',
              format: formatTimePeriod,
              compare: "the more the better",
            },
            {
              key: 'fullGCDurationAvg',
              name: 'phaseStats.fullGCDurationAvg',
              format: formatTimePeriod,
              compare: "the less the better",
            },
            {
              key: 'fullGCDurationMax',
              name: 'phaseStats.fullGCDurationMax',
              format: formatTimePeriod,
              compare: "the less the better",
            },
          ],
        },
        {
          key: 'objectStatistics',
          name: 'objectStats',
          children: [
            {
              key: 'objectCreationSpeed',
              name: 'objectCreationSpeed',
              format: formatSizeSpeed,
              compare: "the less the better",
            },
            {
              key: 'objectPromotionSpeed',
              name: 'objectPromotionSpeed',
              format: formatSizeSpeed,
              compare: "the less the better",
              needed: this.isGenerational
            },
            {
              key: 'objectPromotionAvg',
              name: 'objectPromotionAvg',
              format: formatSize,
              compare: "the less the better",
              needed: this.isGenerational
            },
            {
              key: 'objectPromotionMax',
              name: 'objectPromotionMax',
              format: formatSize,
              compare: "the less the better",
              needed: this.isGenerational
            },
          ],
        },
        {
          key: 'vmOptions',
          name: 'vmOptions.vmOptions',
          children: [
            {
              key: 'gcRelated',
              name: 'vmOptions.gcRelatedOptions',
              format: this.formatString,
              compare: "don't compare",
            },
            {
              key: 'other',
              name: 'vmOptions.otherOptions',
              format: this.formatString,
              compare: "don't compare",
            },
          ]
        }
      ]
    }
  },

  components: {
    Hint,
    ViewMenu,
  },
  methods: {
    pollProgressOfAnalysis(i, file) {
      let self = this;
      if (!self || self._isDestroyed) {
        return
      }
      axios.get(gclogService(file, 'progressOfAnalysis')).then(resp => {
        let state = resp.data.state;
        if (state === 'IN_PROGRESS') {
          setTimeout(() => this.pollProgressOfAnalysis(i, file), this.pollingInternal)
        } else if (state === 'SUCCESS') {
          this.progress += 45;
          this.message += `loading ${file} metadata\n`
          // load metadata early because other requests may be based on the metadata
          axios.get(gclogService(file, 'metadata')).then(resp => {
            this.metadata[i] = resp.data;
            this.readMetadata(i)
            this.progress += 5;
            this.progressState = 'success';
            if (this.progress >= 100) {
              this.$notify({
                title: this.$t("jifa.goToOverViewPrompt"),
                position: 'top-right',
                type: "success",
                offset: 200, // changed
                duration: 1000,
                showClose: true,
                onClose: () => {
                  this.loading = true
                  this.updateUrl()
                  this.loadDataIrrelevantToConfig()
                  this.loadDataRelatedToConfig()
                  this.analysisState = "SUCCESS"
                }
              })
            }
          })
        } else {
          if (resp.data.message) {
            this.message += resp.data.message.replace(/\\n/gm, "<b/>")
          }
          this.progressState = 'danger';
          this.analysisState = "ERROR";
          axios.post(gclogService(file, 'release'))
        }
      })
    },
    initialize() {
      for (let i = 0; i < this.fileCount; i++) {
        this.displayConfig.forEach(config => {
          this.originalData[i][config.key] = {}
        })
        this.files[i] = this['file' + i]
      }
    },
    analyzeGCLogs(params) {
      this.analysisState = "IN_PROGRESS";
      for (let i = 0; i < this.fileCount; i++) {
        const file = this.files[i]
        axios.post(gclogService(file, 'analyze'), new URLSearchParams(params)).then(() => {
          this.message += `analyzing ${file}\n`
          this.pollProgressOfAnalysis(i, file);
        })
      }
    },
    loadDataIrrelevantToConfig() {
      for (let i = 0; i < this.fileCount; i++) {
        const file = this.files[i]
        this.loadDisplayName(i, file)
      }
    },
    readMetadata(i) {
      const metadata = this.metadata[i]
      this.analysisConfig[i].timeRange = {
        start: typeof this['start' + i] === "undefined" ? metadata.startTime : Math.max(parseInt(this['start' + i]), metadata.startTime),
        end: typeof this['end' + i] === "undefined" ? metadata.endTime : Math.min(parseInt(this['end' + i]), metadata.endTime)
      }
      this.originalData[i].basicInfo.logTimeRange
          = formatTimeRange(metadata.startTime, metadata.endTime, metadata.timestamp)
      this.originalData[i].basicInfo.analysisTimeRange = formatTimeRange(this.analysisConfig[i].timeRange.start,
          this.analysisConfig[i].timeRange.end, metadata.timestamp)
      this.originalData[i].basicInfo.analysisTimeRangeLength = this['end' + i] - this['start' + i]
      this.originalData[i].basicInfo.collector = metadata.collector
      this.originalData[i].basicInfo.parallelGCThreads = metadata.parallelGCThreads
      this.originalData[i].basicInfo.concurrentGCThreads = metadata.concurrentGCThreads
    },
    loadDisplayName(i, file) {
      this.doBeforeLoadData()
      axios.get(service('/file'), {
        params: {
          name: file,
          type: this.type
        }
      }).then(resp => {
        this.originalData[i].basicInfo.name = resp.data.displayName ? resp.data.displayName : resp.data.originalName
      }).finally(this.doAfterLoadData)
    },
    loadDataRelatedToConfig() {
      for (let i = 0; i < this.fileCount; i++) {
        const file = this['file' + i]
        this.loadObjectStatistics(i, file)
        this.loadVmOptions(i, file)
        this.loadMemory(i, file)
        this.loadPause(i, file)
        this.loadPhase(i, file)
      }
    },
    loadVmOptions(i, file) {
      this.doBeforeLoadData()
      const requestConfig = {params: {...this.analysisConfig[i].timeRange}}
      axios.get(gclogService(file, 'vmOptions'), requestConfig).then(resp => {
        if ((resp.data === undefined)) {
          this.originalData[i].vmOptions = {}
          return
        }
        this.originalData[i].vmOptions = {
          gcRelated: resp.data.gcRelated.map(option => option.text).join(' '),
          other: resp.data.other.map(option => option.text).join(' '),
        }
      }).finally(this.doAfterLoadData)
    },
    loadMemory(i, file) {
      this.doBeforeLoadData()
      const requestConfig = {params: {...this.analysisConfig[i].timeRange}}
      axios.get(gclogService(file, 'memoryStatistics'), requestConfig).then(resp => {
        const memoryStatistics = {}
        Object.keys(resp.data).forEach(generation => {
          const generationData = resp.data[generation]
          Object.keys(generationData).forEach(metric => {
            const key = generation + this.uppercaseFirstLetter(metric)
            memoryStatistics[key] = generationData[metric]
          })
        })
        this.originalData[i].memoryStatistics = memoryStatistics
      }).finally(this.doAfterLoadData)
    },
    loadPause(i, file) {
      this.doBeforeLoadData()
      const requestConfig = {params: {...this.analysisConfig[i].timeRange}}
      axios.get(gclogService(file, 'pauseStatistics'), requestConfig).then(resp => {
        this.originalData[i].pauseStatistics = resp.data
      }).finally(this.doAfterLoadData)
    },
    loadPhase(i, file) {
      this.doBeforeLoadData()
      const requestConfig = {params: {...this.analysisConfig[i].timeRange}}
      axios.get(gclogService(file, 'phaseStatistics'), requestConfig).then(resp => {
        const phaseStatistics = {}
        const metrics = ['count', 'intervalAvg', 'durationAvg', 'durationMax']
        resp.data.parents.map(e => e.self).forEach(event => {
          let prefix = undefined;
          if (event.name === 'Mixed GC') {
            prefix = "mixedGC"
          } else if (isYoungGC(event.name)) {
            prefix = "youngGC"
          } else if (isOldGC(event.name)) {
            prefix = "oldGC"
          } else if (isFullGC(event.name)) {
            prefix = "fullGC"
          }
          metrics.forEach(metric => {
            const key = prefix + this.uppercaseFirstLetter(metric)
            phaseStatistics[key] = event[metric]
          })
        })
        this.originalData[i].phaseStatistics = phaseStatistics
      }).finally(this.doAfterLoadData)
    },
    loadObjectStatistics(i, file) {
      this.doBeforeLoadData()
      const requestConfig = {params: {...this.analysisConfig[i].timeRange}}
      axios.get(gclogService(file, 'objectStatistics'), requestConfig).then(resp => {
        this.originalData[i].objectStatistics = resp.data
      }).finally(this.doAfterLoadData)
    },
    doBeforeLoadData() {
      this.pendingRequest++;
    },
    doAfterLoadData() {
      this.pendingRequest--;
      if (this.pendingRequest <= 0) {
        this.calculateTableData()
        this.pendingRequest = 0
      }
    },
    calculateTableData() {
      this.loading = true;
      const data = []
      this.displayConfig.forEach(metricGroupConfig => {
        const metricGroupObj = {
          metric: this.$t('jifa.gclog.' + metricGroupConfig.name),
          children: [],
        }
        metricGroupConfig.children.forEach(metricConfig => {
          const needed = this.metadata.map(m => typeof metricConfig.needed === "function" ? metricConfig.needed(m) : true)
          if (!needed[0] && !needed[1]) {
            return
          }
          const originalValue = this.originalData.map(d => d[metricGroupConfig.key][metricConfig.key])
          const valueAvailable = needed.map((dummy, i) =>
              needed[i] &&
              originalValue[i] !== undefined &&
              (typeof originalValue[i] !== "number" || originalValue[i] >= 0)) // check number additionally for comparing
          if (!valueAvailable[0] && !valueAvailable[1]) {
            return;
          }
          let compare = '', compareClass = ''
          if (valueAvailable[0] && valueAvailable[1] && metricConfig.compare !== "don't compare") {
            const diff = originalValue[1] - originalValue[0]
            compare = (diff >= 0 ? '+' : '-') + formatPercentage(Math.abs(diff / originalValue[0]))
            if (metricConfig.compare !== "just compare" && diff !== 0) {
              if ((diff > 0 && metricConfig.compare === 'the more the better') ||
                  (diff < 0 && metricConfig.compare === 'the less the better')) {
                compareClass = 'good-metric-compare'
              } else {
                compareClass = 'bad-metric-compare'
              }
            }
          }
          const metricObj = {
            metric: this.$t('jifa.gclog.' + metricConfig.name),
            metricHint: metricConfig.metricHint === undefined ? undefined : this.$t('jifa.gclog.' + metricConfig.metricHint),
            value0: valueAvailable[0] ? metricConfig.format(originalValue[0]) : 'N/A',
            value1: valueAvailable[1] ? metricConfig.format(originalValue[1]) : 'N/A',
            compare: compare,
            compareClass: compareClass
          }
          metricGroupObj.children.push(metricObj)
        })
        if (metricGroupObj.children.length > 0) {
          data.push(metricGroupObj)
        }
      })

      this.tableData = data
      this.loading = false;
    },
    isGenerational(metadata) {
      return metadata.generational
    },
    isG1(metadata) {
      return metadata.collector === 'G1 GC'
    },
    updateUrl() {
      const params = getUrlParams(window.location.href);
      if (this.analysisConfig[0].timeRange.start != params.start0 ||
          this.analysisConfig[0].timeRange.end != params.end0 ||
          this.analysisConfig[1].timeRange.start != params.start1 ||
          this.analysisConfig[1].timeRange.end != params.end1) {
        this.$router.push({
          query: {
            file0: this.files[0],
            start0: this.analysisConfig[0].timeRange.start,
            end0: this.analysisConfig[0].timeRange.end,
            file1: this.files[1],
            start1: this.analysisConfig[1].timeRange.start,
            end1: this.analysisConfig[1].timeRange.end,
          }
        })
      }
    },
    formatString(str) {
      return !str ? "N/A" : str;
    },
    formatCount(n) {
      return n <= 0 ? "N/A" : n + '';
    },
    uppercaseFirstLetter(str) {
      return str.slice(0, 1).toUpperCase() + str.slice(1)
    },
    clickTitle(i) {
      const url = this.$router.resolve({
        name: 'gcLog',
        query: {
          file: this.files[i],
          start: this.analysisConfig[i].timeRange.start,
          end: this.analysisConfig[i].timeRange.end,
        }
      })
      window.open(url.href)
      this.compareConfigVisible = false
    }
  },
  mounted() {
    this.initialize();
    this.analyzeGCLogs();
  }
}
</script>

<style scoped>
.bad-metric-compare {
  color: #E74C3C;
}

.good-metric-compare {
  color: #9ad969;
}
</style>