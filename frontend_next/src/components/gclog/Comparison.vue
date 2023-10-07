<!--
    Copyright (c) 2023 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router';
import { useAnalysisStore } from '@/stores/analysis';
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';
import { GC_LOG } from '@/composables/file-types';
import axios from 'axios';
import {
  formatPercentage,
  formatTimePeriod,
  formatTimeRange,
  hasParallelGCThreads,
  hasConcurrentGCThreads,
  isFullGC,
  isOldGC,
  isYoungGC,
  uppercaseFirstLetter,
  formatSize,
  hasOldGC,
  formatSizeSpeed
} from '@/components/gclog/utils';
import { gct, t } from '@/i18n/i18n';
import { TABLE_HEADER_CELL_STYLE } from '@/components/styles';
import { Close, Rank } from '@element-plus/icons-vue';
import FileSelector from '@/components/FileSelector.vue';
import { useGCLogData } from '@/stores/gc-log-data';

interface BasicInfo {
  logTimeRange: string;
  analysisTimeRange: string;
  analysisTimeRangeLength: number;
  collector: string;
  parallelGCThreads: number;
  concurrentGCThreads: number;
}

interface TimeRange {
  start;
  end;
}

interface Log {
  uniqueName: string;
  metadata?: any;
  fileInfo?: any;
  basicInfo?: BasicInfo;
  range?: TimeRange;

  objectStatistics?: any;
  vmOptions?: any;
  memoryStatistics?: any;
  pauseStatistics?: any;
  phaseStatistics?: any;
}

const { requestWithTarget } = useAnalysisApiRequester();

function request(api: string, target: string, parameters?: object) {
  return requestWithTarget(api, GC_LOG, target, parameters);
}

const route = useRoute();
const router = useRouter();
const analysisStore = useAnalysisStore();
const GCLogData = useGCLogData();

function extractBaseline() {
  let uniqueName = analysisStore.target;
  let range = GCLogData.analysisConfig.timeRange;
  return {
    uniqueName,
    range
  };
}

function extractCompareTargets() {
  let payload = route.query.compareTo as string;
  if (payload) {
    return payload.split('~~').map((str) => {
      let parts = str.split('~');
      return parts.length === 3
        ? {
            uniqueName: parts[0],
            range: {
              start: parseInt(parts[1]),
              end: parseInt(parts[2])
            }
          }
        : {
            uniqueName: parts[0]
          };
    });
  }
  return [];
}

const fileSelectorVisible = ref(false);

const logs: Log[] = [extractBaseline(), extractCompareTargets()].flat();
const allAnalyzed = ref(false);
const analysisCounter = ref(-1);
const pendingRequest = ref(-1);
const loadingData = ref(false);
const tableData = ref();

const timeRangeSelectorVisible = ref(false);
const timeRangeSelectorTargetIndex = ref(-1);
const timeRangeSelectorTarget = ref();
const timeRangeSelectorTargetUseUptime = ref(false);

const timeRangeStart = ref();
const timeRangeEnd = ref();
const timeRange = ref();

function prepareToUpdateTimeRange(index) {
  let log = logs[index];
  let metadata = log.metadata;
  timeRangeSelectorTargetIndex.value = index;
  timeRangeSelectorTarget.value = log;
  let useUptime = metadata.timestamp < 0;
  timeRangeSelectorTargetUseUptime.value = useUptime;

  if (useUptime) {
    timeRangeStart.value = Math.floor(log.range!.start / 1000);
    timeRangeEnd.value = Math.ceil(log.range!.end / 1000);
  } else {
    timeRange.value = [
      new Date(metadata.timestamp + log.range!.start),
      new Date(metadata.timestamp + log.range!.end)
    ];
  }

  timeRangeSelectorVisible.value = true;
}

function applyTimeRange() {
  let log = timeRangeSelectorTarget.value;
  let metadata = log!.metadata;
  let start;
  let end;
  if (metadata.timestamp < 0) {
    start = timeRangeStart.value * 1000;
    end = timeRangeEnd.value * 1000;
  } else {
    const min = metadata.timestamp;
    const max = metadata.timestamp + metadata.endTime;
    let [ts, te] = timeRange.value;
    if (ts.getTime() < min) {
      ts = new Date(min);
    } else if (ts.getTime() > max) {
      ts = new Date(max);
    }
    if (te.getTime() < min) {
      te = new Date(min);
    } else if (te.getTime() > max) {
      te = new Date(max);
    }
    start = ts.getTime() - metadata.timestamp;
    end = te.getTime() - metadata.timestamp;
  }

  let index = timeRangeSelectorTargetIndex.value;
  let compareTo = route.query.compareTo as string;
  if (index === 0) {
    router.push({ query: { start, end, compareTo } });
  } else {
    let newPayLoad = compareTo ? compareTo.split('~~') : [];
    newPayLoad[index - 1] = `${timeRangeSelectorTarget.value!.uniqueName}~${start}~${end}`;
    router.push({ query: { ...route.query, compareTo: newPayLoad.join('~~') } });
  }

  log!.range = { start, end };
  timeRangeSelectorVisible.value = false;
  analyzeLogs();
}

function disabledDate(time: Date) {
  let metadata = timeRangeSelectorTarget.value!.metadata;
  let start = new Date(metadata.timestamp + metadata.startTime);
  start.setHours(0, 0, 0, 0);
  let end = new Date(metadata.timestamp + metadata.endTime);
  end.setHours(0, 0, 0, 0);
  return !(start <= time && time <= end);
}

watchEffect(() => {
  if (analysisCounter.value === 0) {
    allAnalyzed.value = true;
    loadData();
  }
});

watchEffect(() => {
  if (pendingRequest.value === 0) {
    buildTableData();
    loadingData.value = false;
  }
});

function formatString(str) {
  return !str ? 'N/A' : str;
}

function formatCount(n) {
  return n <= 0 ? 'N/A' : n + '';
}

function formatVmOptions(options) {
  return options ? options.join(' ') : '';
}

function isGenerational(metadata) {
  return metadata.generational;
}

function isG1(metadata) {
  return metadata.collector === 'G1 GC';
}

const displayConfig = [
  // use array to preserve the order of keys
  {
    key: 'basicInfo', // for getting data from original data
    name: 'basicInfo', // for i18n display in table, will add jifa.gclog in the front
    children: [
      {
        key: 'name',
        name: 'gclogFile',
        format: formatString,
        compare: "don't compare"
      },
      {
        key: 'logTimeRange', // for getting data from original data
        name: 'logTimeRange', // for i18n display in table, will add jifa.gclog in the front
        // hint: '' // // for the hint of metric, will add jifa.gclog in the front
        format: formatString, // how the value will be displayed
        compare: "don't compare" // enum of "don't compare", "just compare", "the more the better", "the less the better"
        // needed: (metadata => true) // is this metric needed?
      },
      {
        key: 'analysisTimeRange',
        name: 'analysisTimeRange',
        format: formatString,
        compare: "don't compare",
        click: (index) => prepareToUpdateTimeRange(index)
      },
      {
        key: 'analysisTimeRangeLength',
        name: 'analysisTimeRangeLength',
        format: formatTimePeriod,
        compare: 'just compare'
      },
      {
        key: 'collector',
        name: 'collector',
        format: formatString,
        compare: "don't compare"
      },
      {
        key: 'parallelGCThreads',
        name: 'parallelGCThreads',
        format: formatCount,
        compare: 'just compare',
        needed: hasParallelGCThreads
      },
      {
        key: 'concurrentGCThreads',
        name: 'concurrentGCThreads',
        format: formatCount,
        compare: 'just compare',
        needed: hasConcurrentGCThreads
      }
    ]
  },
  {
    key: 'pauseStatistics',
    name: 'pauseInfo.pauseInfo',
    children: [
      {
        key: 'throughput',
        name: 'pauseInfo.throughput',
        format: formatPercentage,
        compare: 'the more the better'
      },
      {
        key: 'pauseAvg',
        name: 'pauseInfo.pauseAvg',
        format: formatTimePeriod,
        compare: 'the less the better'
      },
      {
        key: 'pauseMedian',
        name: 'pauseInfo.pauseMedian',
        format: formatTimePeriod,
        compare: 'the less the better'
      },
      {
        key: 'pauseP99',
        name: 'pauseInfo.pauseP99',
        format: formatTimePeriod,
        compare: 'the less the better'
      },
      {
        key: 'pauseP999',
        name: 'pauseInfo.pauseP999',
        format: formatTimePeriod,
        compare: 'the less the better'
      },
      {
        key: 'pauseMax',
        name: 'pauseInfo.pauseMax',
        format: formatTimePeriod,
        compare: 'the less the better'
      }
    ]
  },
  {
    key: 'memoryStatistics',
    name: 'memoryStats.memoryStats',
    children: [
      {
        key: 'youngCapacityAvg',
        name: 'memoryStats.youngCapacityAvg',
        format: formatSize,
        compare: 'just compare',
        needed: isGenerational
      },
      {
        key: 'youngUsedMax',
        name: 'memoryStats.youngUsedMax',
        format: formatSize,
        compare: 'just compare',
        needed: isGenerational
      },
      {
        key: 'oldCapacityAvg',
        name: 'memoryStats.oldCapacityAvg',
        format: formatSize,
        compare: 'just compare',
        needed: isGenerational
      },
      {
        key: 'oldUsedMax',
        name: 'memoryStats.oldUsedMax',
        format: formatSize,
        compare: 'the less the better',
        needed: isGenerational
      },
      {
        key: 'oldUsedAvgAfterFullGC',
        name: 'memoryStats.oldUsedAvgAfterFullGC',
        format: formatSize,
        compare: 'the less the better',
        needed: isGenerational
      },
      {
        key: 'oldUsedAvgAfterOldGC',
        name: 'memoryStats.oldUsedAvgAfterOldGC',
        format: formatSize,
        compare: 'the less the better',
        needed: hasOldGC
      },
      {
        key: 'humongousUsedMax',
        name: 'memoryStats.humongousUsedMax',
        format: formatSize,
        compare: 'the less the better',
        needed: isG1
      },
      {
        key: 'humongousUsedAvgAfterFullGC',
        name: 'memoryStats.humongousUsedAvgAfterFullGC',
        format: formatSize,
        compare: 'the less the better',
        needed: isG1
      },
      {
        key: 'humongousUsedAvgAfterOldGC',
        name: 'memoryStats.humongousUsedAvgAfterOldGC',
        format: formatSize,
        compare: 'the less the better',
        needed: isG1
      },
      {
        key: 'heapCapacityAvg',
        name: 'memoryStats.heapCapacityAvg',
        format: formatSize,
        compare: 'just compare'
      },
      {
        key: 'heapUsedMax',
        name: 'memoryStats.heapUsedMax',
        format: formatSize,
        compare: 'the less the better'
      },
      {
        key: 'heapUsedAvgAfterFullGC',
        name: 'memoryStats.heapUsedAvgAfterFullGC',
        format: formatSize,
        compare: 'the less the better'
      },
      {
        key: 'heapUsedAvgAfterOldGC',
        name: 'memoryStats.heapUsedAvgAfterOldGC',
        format: formatSize,
        compare: 'the less the better',
        needed: hasOldGC
      },
      {
        key: 'metaspaceCapacityAvg',
        name: 'memoryStats.metaspaceCapacityAvg',
        format: formatSize,
        compare: 'just compare'
      },
      {
        key: 'metaspaceUsedMax',
        name: 'memoryStats.metaspaceUsedMax',
        format: formatSize,
        compare: 'the less the better'
      },
      {
        key: 'metaspaceUsedAvgAfterFullGC',
        name: 'memoryStats.metaspaceUsedAvgAfterFullGC',
        format: formatSize,
        compare: 'the less the better'
      },
      {
        key: 'metaspaceUsedAvgAfterOldGC',
        name: 'memoryStats.metaspaceUsedAvgAfterOldGC',
        format: formatSize,
        compare: 'the less the better',
        needed: hasOldGC
      }
    ]
  },
  {
    key: 'phaseStatistics',
    name: 'phaseStats.phaseStats',
    children: [
      {
        key: 'youngGCCount',
        name: 'phaseStats.youngGCCount',
        format: formatCount,
        compare: 'the less the better',
        needed: isGenerational
      },
      {
        key: 'youngGCIntervalAvg',
        name: 'phaseStats.youngGCIntervalAvg',
        format: formatTimePeriod,
        compare: 'the more the better',
        needed: isGenerational
      },
      {
        key: 'youngGCDurationAvg',
        name: 'phaseStats.youngGCDurationAvg',
        format: formatTimePeriod,
        compare: 'the less the better',
        needed: isGenerational
      },
      {
        key: 'youngGCDurationMax',
        name: 'phaseStats.youngGCDurationMax',
        format: formatTimePeriod,
        compare: 'the less the better',
        needed: isGenerational
      },
      {
        key: 'mixedGCCount',
        name: 'phaseStats.mixedGCCount',
        format: formatCount,
        compare: 'the less the better',
        needed: isG1
      },
      {
        key: 'mixedGCIntervalAvg',
        name: 'phaseStats.mixedGCIntervalAvg',
        format: formatTimePeriod,
        compare: 'the more the better',
        needed: isG1
      },
      {
        key: 'mixedGCDurationAvg',
        name: 'phaseStats.mixedGCDurationAvg',
        format: formatTimePeriod,
        compare: 'the less the better',
        needed: isG1
      },
      {
        key: 'mixedGCDurationMax',
        name: 'phaseStats.mixedGCDurationMax',
        format: formatTimePeriod,
        compare: 'the less the better',
        needed: isG1
      },
      {
        key: 'oldGCCount',
        name: 'phaseStats.oldGCCount',
        format: formatCount,
        compare: 'the less the better',
        needed: hasOldGC
      },
      {
        key: 'oldGCIntervalAvg',
        name: 'phaseStats.oldGCIntervalAvg',
        format: formatTimePeriod,
        compare: 'the more the better',
        needed: hasOldGC
      },
      {
        key: 'oldGCDurationAvg',
        name: 'phaseStats.oldGCDurationAvg',
        format: formatTimePeriod,
        compare: 'the less the better',
        needed: hasOldGC
      },
      {
        key: 'oldGCDurationMax',
        name: 'phaseStats.oldGCDurationMax',
        format: formatTimePeriod,
        compare: 'the less the better',
        needed: hasOldGC
      },
      {
        key: 'fullGCCount',
        name: 'phaseStats.fullGCCount',
        format: formatCount,
        compare: 'the less the better'
      },
      {
        key: 'fullGCIntervalAvg',
        name: 'phaseStats.fullGCIntervalAvg',
        format: formatTimePeriod,
        compare: 'the more the better'
      },
      {
        key: 'fullGCDurationAvg',
        name: 'phaseStats.fullGCDurationAvg',
        format: formatTimePeriod,
        compare: 'the less the better'
      },
      {
        key: 'fullGCDurationMax',
        name: 'phaseStats.fullGCDurationMax',
        format: formatTimePeriod,
        compare: 'the less the better'
      }
    ]
  },
  {
    key: 'objectStatistics',
    name: 'objectStats',
    children: [
      {
        key: 'objectCreationSpeed',
        name: 'objectCreationSpeed',
        format: formatSizeSpeed,
        compare: 'the less the better'
      },
      {
        key: 'objectPromotionSpeed',
        name: 'objectPromotionSpeed',
        format: formatSizeSpeed,
        compare: 'the less the better',
        needed: isGenerational
      },
      {
        key: 'objectPromotionAvg',
        name: 'objectPromotionAvg',
        format: formatSize,
        compare: 'the less the better',
        needed: isGenerational
      },
      {
        key: 'objectPromotionMax',
        name: 'objectPromotionMax',
        format: formatSize,
        compare: 'the less the better',
        needed: isGenerational
      }
    ]
  },
  {
    key: 'vmOptions',
    name: 'vmOptions.vmOptions',
    children: [
      {
        key: 'gcRelated',
        name: 'vmOptions.gcRelatedOptions',
        format: formatVmOptions,
        compare: "don't compare"
      },
      {
        key: 'other',
        name: 'vmOptions.otherOptions',
        format: formatVmOptions,
        compare: "don't compare"
      }
    ]
  }
];

function buildTableData() {
  const data = [];
  displayConfig.forEach((metricGroupConfig) => {
    const metricGroupObj = {
      metric: gct(metricGroupConfig.name),
      children: []
    };
    metricGroupConfig.children.forEach((metricConfig: any) => {
      const needed = logs.map((log) =>
        typeof metricConfig.needed === 'function' ? metricConfig.needed(log.metadata) : true
      );

      if (!needed.reduce((b1, b2) => b1 || b2)) {
        return;
      }

      const originalValue = logs.map((log) => log[metricGroupConfig.key][metricConfig.key]);
      const valueAvailable = needed.map(
        (_, i) =>
          needed[i] &&
          originalValue[i] !== undefined &&
          (typeof originalValue[i] !== 'number' || originalValue[i] >= 0)
      ); // check number additionally for comparing

      if (!valueAvailable.reduce((v1, v2) => v1 || v2)) {
        return;
      }

      let values = [
        {
          value: valueAvailable[0] ? metricConfig.format(originalValue[0]) : 'N/A'
        }
      ];

      for (let i = 1; i < logs.length; i++) {
        let value = valueAvailable[i] ? metricConfig.format(originalValue[i]) : 'N/A';
        let compare = '';
        let compareClass = '';
        if (valueAvailable[0] && valueAvailable[i] && metricConfig.compare !== "don't compare") {
          const diff = originalValue[i] - originalValue[0];
          if (originalValue[0] !== 0) {
            if (diff == 0) {
              compare = '0%';
            } else {
              compare =
                (diff > 0 ? '+' : '-') + formatPercentage(Math.abs(diff / originalValue[0]));
            }
            if (metricConfig.compare !== 'just compare' && diff !== 0) {
              if (
                (diff > 0 && metricConfig.compare === 'the more the better') ||
                (diff < 0 && metricConfig.compare === 'the less the better')
              ) {
                compareClass = 'better-metric';
              } else {
                compareClass = 'worse-metric';
              }
            }
          }
        }
        values.push({
          value,
          compare,
          compareClass
        });
      }

      const metricObj = {
        metric: gct(metricConfig.name),
        metricHint: metricConfig.metricHint ? gct(metricConfig.metricHint) : undefined,
        values,
        click: metricConfig.click
      };
      metricGroupObj.children.push(metricObj);
    });
    if (metricGroupObj.children.length > 0) {
      data.push(metricGroupObj);
    }
  });

  tableData.value = data;
}

function loadObjectStatistics(log) {
  request('objectStatistics', log.uniqueName, { ...log.range }).then((data) => {
    log.objectStatistics = data;
    pendingRequest.value--;
  });
}

function loadVmOptions(log) {
  request('vmOptions', log.uniqueName, { ...log.range }).then((data) => {
    log.vmOptions = data
      ? {
          gcRelated: data.gcRelated.map((option) => option.text),
          other: data.other.map((option) => option.text)
        }
      : {};
    pendingRequest.value--;
  });
}

function loadMemory(log) {
  request('memoryStatistics', log.uniqueName, { ...log.range }).then((data) => {
    const memoryStatistics = {};
    Object.keys(data).forEach((generation) => {
      const generationData = data[generation];
      Object.keys(generationData).forEach((metric) => {
        const key = generation + uppercaseFirstLetter(metric);
        memoryStatistics[key] = generationData[metric];
      });
    });
    log.memoryStatistics = memoryStatistics;
    pendingRequest.value--;
  });
}

function loadPause(log) {
  request('pauseStatistics', log.uniqueName, { ...log.range }).then((data) => {
    log.pauseStatistics = data;
    pendingRequest.value--;
  });
}

function loadPhase(log) {
  request('phaseStatistics', log.uniqueName, { ...log.range }).then((data) => {
    const phaseStatistics = {};
    const metrics = ['count', 'intervalAvg', 'durationAvg', 'durationMax'];
    data.parents
      .map((e) => e.self)
      .forEach((event) => {
        let prefix = undefined;
        if (event.name === 'Mixed GC') {
          prefix = 'mixedGC';
        } else if (isYoungGC(event.name)) {
          prefix = 'youngGC';
        } else if (isOldGC(event.name)) {
          prefix = 'oldGC';
        } else if (isFullGC(event.name)) {
          prefix = 'fullGC';
        }
        metrics.forEach((metric) => {
          const key = prefix + uppercaseFirstLetter(metric);
          phaseStatistics[key] = event[metric];
        });
      });
    log.phaseStatistics = phaseStatistics;
    pendingRequest.value--;
  });
}

function loadData() {
  loadingData.value = true;
  // Note: 5 is the count of analysis requests per log, it should be updated in sync
  pendingRequest.value = logs.length * 5;

  logs.forEach((log) => {
    loadObjectStatistics(log);
    loadVmOptions(log);
    loadMemory(log);
    loadPause(log);
    loadPhase(log);
  });
}

function pollProgress(log) {
  request('progressOfAnalysis', log.uniqueName).then((progress) => {
    if (progress.state === 'SUCCESS') {
      request('metadata', log.uniqueName).then((metadata) => {
        log.metadata = metadata;

        if (!log.range) {
          log.range = {
            start: metadata.startTime,
            end: metadata.endTime
          };
        }

        log.basicInfo = {
          name: log.fileInfo.originalName,
          logTimeRange: formatTimeRange(metadata.startTime, metadata.endTime, metadata.timestamp),
          analysisTimeRange: formatTimeRange(log.range.start, log.range.end, metadata.timestamp),
          analysisTimeRangeLength: log.range.end - log.range.start,
          collector: metadata.collector,
          parallelGCThreads: metadata.parallelGCThreads,
          concurrentGCThreads: metadata.concurrentGCThreads
        };

        analysisCounter.value--;
      });
    } else if ('IN_PROGRESS') {
      setTimeout(() => pollProgress(log), 1000);
    }
  });
}

function selectionCallback(files) {
  fileSelectorVisible.value = false;

  if (files.length == 0) {
    return;
  }

  let payload = route.query.compareTo as string;
  let newPayLoad = payload ? payload.split('~~') : [];
  files.forEach((uniqueName) => {
    logs.push({
      uniqueName
    });
    newPayLoad.push(uniqueName);
  });

  router.push({ query: { ...route.query, compareTo: newPayLoad.join('~~') } });

  analyzeLogs();
}

function deleteLog(index) {
  logs.splice(index, 1);

  let payload = route.query.compareTo as string;
  let newPayLoad = payload ? payload.split('~~') : [];
  newPayLoad.splice(index - 1, 1);
  router.push({ query: { ...route.query, compareTo: newPayLoad.join('~~') } });

  analyzeLogs();
}

function openLog(log) {
  window.open(`${log.uniqueName}?start=${log.range.start}&end=${log.range.end}`);
}

const uniqueNames = ref([]);

function analyzeLogs() {
  allAnalyzed.value = false;
  analysisCounter.value = logs.length;
  uniqueNames.value = [];
  logs.forEach((log) => {
    uniqueNames.value.push(log.uniqueName);
    axios
      .get(`/jifa-api/files/${log.uniqueName}`)
      .then(({ data }) => {
        log.fileInfo = data;
      })
      .then(() => request('analyze', log.uniqueName))
      .then(() => {
        pollProgress(log);
      });
  });
}

onMounted(() => {
  analyzeLogs();
});
</script>
<template>
  <FileSelector
    :type="GC_LOG"
    :pre-selected="uniqueNames"
    @selection-callback="selectionCallback"
    v-model:visible="fileSelectorVisible"
    v-if="fileSelectorVisible"
  />

  <el-dialog
    :width="timeRangeSelectorTargetUseUptime ? 480 : 550"
    v-model="timeRangeSelectorVisible"
    v-if="timeRangeSelectorVisible"
  >
    <template #header>
      <span class="ej-ellipsis-text">
        {{ timeRangeSelectorTarget!.fileInfo.originalName }}
      </span>
    </template>
    <div style="display: flex; justify-content: center">
      <el-space size="large">
        <template v-if="timeRangeSelectorTargetUseUptime">
          <div>
            <el-input-number
              controls-position="right"
              :step="60"
              :placeholder="gct('detail.startTime')"
              :min="Math.floor(timeRangeSelectorTarget!.metadata.startTime / 1000)"
              :max="timeRangeEnd"
              v-model="timeRangeStart"
            />
            <span style="margin: 0 12px">-</span>
            <el-input-number
              controls-position="right"
              :step="60"
              :placeholder="gct('detail.endTime')"
              :min="timeRangeStart"
              :max="Math.ceil(timeRangeSelectorTarget!.metadata.endTime / 1000)"
              v-model="timeRangeEnd"
            />
          </div>
        </template>

        <el-date-picker
          type="datetimerange"
          :clearable="false"
          :start-placeholder="gct('detail.startTime')"
          :end-placeholder="gct('detail.endTime')"
          :disabled-date="disabledDate"
          v-model="timeRange"
          v-else
        />

        <el-button type="primary" @click="applyTimeRange">{{ t('common.confirm') }}</el-button>
      </el-space>
    </div>
  </el-dialog>

  <div
    style="width: 100%; height: 100%; padding: 18px; display: flex; flex-direction: column"
    v-loading="!allAnalyzed"
  >
    <div
      style="
        margin-bottom: 18px;
        padding-right: 2px;
        display: flex;
        justify-content: space-between;
        align-items: center;
      "
    >
      <div>
        <el-button
          type="primary"
          plain
          :disabled="!allAnalyzed || loadingData"
          @click="fileSelectorVisible = true"
        >
          {{ t('analysis.selectComparisonTargets') }}
          <el-icon style="margin-left: 5px">
            <Rank />
          </el-icon>
        </el-button>
      </div>

      <div style="display: flex; align-items: center" v-if="logs.length > 1">
        <div class="better-color-block" />
        <el-text>{{ gct('betterPerformance') }}</el-text>
        <div class="worse-color-block" />
        <el-text>{{ gct('worsePerformance') }}</el-text>
      </div>
    </div>

    <template v-loading="loadingData" v-if="allAnalyzed">
      <el-table
        class="table"
        highlight-current-row
        :header-cell-style="TABLE_HEADER_CELL_STYLE"
        stripe
        border
        default-expand-all
        row-key="metric"
        :data="tableData"
      >
        <el-table-column fixed width="280" :label="gct('metric')" prop="metric" />

        <el-table-column v-for="(log, index) in logs" :fixed="index == 0 && logs.length > 1">
          <template #header>
            <div style="display: flex; align-items: center">
              <span class="clickable" @click="openLog(log)">
                {{
                  index === 0
                    ? gct('baselineFile')
                    : gct('targetFile') + (logs.length > 2 ? ' #' + index : '')
                }}
              </span>
              <el-icon
                class="close-icon"
                style="margin-left: 5px"
                @click="deleteLog(index)"
                v-if="index !== 0"
              >
                <Close />
              </el-icon>
            </div>
          </template>

          <el-table-column
            min-width="162"
            :label="gct(index === 0 ? 'metricValueOfBaseline' : 'metricValueOfTarget')"
          >
            <template #default="{ row }">
              <template v-if="row.values && row.values[index]">
                <span
                  :class="{ clickable: row.click }"
                  @click="row.click ? row.click(index) : void 0"
                >
                  {{ row.values[index].value }}
                </span>
              </template>
            </template>
          </el-table-column>

          <el-table-column min-width="100" :label="gct('metricValueDifference')" v-if="index > 0">
            <template #default="{ row }">
              <span
                :class="['metric', row.values[index].compareClass]"
                v-if="row.values && row.values[index]"
              >
                {{ row.values[index].compare }}
              </span>
            </template>
          </el-table-column>
        </el-table-column>
      </el-table>
    </template>
  </div>
</template>
<style scoped>
.table {
  flex-grow: 1;
}

.table :deep(td .cell) {
  display: flex;
  align-items: center;
}

.better-color-block,
.worse-color-block {
  height: 12px;
  width: 22px;
  border-radius: 2px;
  margin-right: 5px;
}

.clickable {
  cursor: pointer;
  color: var(--el-color-primary);
}

.better-color-block {
  background-color: var(--el-color-success);
}

.worse-color-block {
  background-color: var(--el-color-danger);
  margin-left: 10px;
}

.metric {
  font-weight: 500;
}

.better-metric {
  color: var(--el-color-success);
}

.worse-metric {
  color: var(--el-color-danger);
}

.close-icon {
  color: var(--el-color-info);
  cursor: pointer;
}

.close-icon:hover {
  color: var(--el-color-primary);
}

:deep(.el-space__item:last-child) {
  margin-right: 0 !important;
}
</style>
