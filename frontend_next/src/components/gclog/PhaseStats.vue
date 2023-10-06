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
import { onMounted, ref, watch } from 'vue';
import { useGCLogData } from '@/stores/gc-log-data';
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';
import {
  badCause,
  badDurationThreshold,
  badIntervalThreshold,
  badPhase,
  formatTimePeriod,
  getCauseHint,
  getPhaseHint,
  isOldGC,
  isPause,
  isYoungGC
} from '@/components/gclog/utils';
import { gct } from '@/i18n/i18n';
import { TABLE_HEADER_CELL_STYLE } from '@/components/styles';
import ValueWithHint from '@/components/gclog/ValueWithHint.vue';
import { VideoPause } from '@element-plus/icons-vue';

const props = defineProps({
  headerBarId: {
    type: String,
    required: true
  }
});

const { request } = useAnalysisApiRequester();
const GCLogData = useGCLogData();
const metadata = GCLogData.metadata;

const mounted = ref(false);
const loading = ref(false);
const displayModeOptions = ['pauseMode', 'structuredMode', 'causeMode'];
const columns = [
  'name',
  'count',
  'intervalAvg',
  'intervalMin',
  'durationAvg',
  'durationMax',
  'durationTotal'
];
const displayMode = ref('pauseMode');
const originalData = ref();
const tableData = ref();

function parentHasGCCause(phase) {
  return phase !== 'CMS' && phase !== 'Concurrent Mark Cycle' && phase !== 'Concurrent Undo Cycle';
}

function badCauseCount(item, parent) {
  const cause = item.name;
  if (
    parent.count >= 10 &&
    item.count / parent.count >= 0.3 &&
    (cause === 'G1 Humongous Allocation' || cause === 'GCLocker Initiated GC')
  ) {
    return {
      bad: true,
      badHint: 'badHint.badCauseCount',
      badHintArgs: { name: item.name }
    };
  }
  return { bad: false };
}

function badPhaseCount(item) {
  if (isOldGC(item.name)) {
    const youngGCCount = originalData.value
      .filter((d) => isYoungGC(d.self.name))
      .map((d) => d.self.count)
      .reduce((prev, next, index, array) => prev + next, 0);
    if (
      item.count >= 5 &&
      item.count > (youngGCCount * GCLogData.analysisConfig!.tooManyOldGCThreshold) / 100
    ) {
      return {
        bad: true,
        badHint: 'badHint.badPhaseCount',
        badHintArgs: { name: item.name }
      };
    }
  }
  return { bad: false };
}

function formatDataItem(isPhase, originalData, originalParent) {
  let analysisConfig = GCLogData.analysisConfig;
  return {
    key: (originalParent === null ? '' : originalParent.name) + originalData.name,
    name: {
      value: originalData.name,
      hint: isPhase ? getPhaseHint(originalData.name) : getCauseHint(originalData.name),
      ...(isPhase ? badPhase(originalData.name) : badCause(originalParent.name, originalData.name)),
      pause: isPhase && isPause(originalData.name, metadata)
    },
    count: {
      value: originalData.count,
      ...(isPhase ? badPhaseCount(originalData) : badCauseCount(originalData, originalParent))
    },
    intervalAvg: {
      value: formatTimePeriod(originalData.intervalAvg),
      bad:
        originalData.intervalAvg >= 0 &&
        originalData.intervalAvg <=
          badIntervalThreshold(isPhase ? originalData.name : originalParent.name, analysisConfig),
      badHint: 'badHint.badInterval',
      badHintArgs: { name: originalData.name }
    },
    intervalMin: {
      value: formatTimePeriod(originalData.intervalMin),
      bad:
        originalData.intervalMin >= 0 &&
        originalData.intervalMin <=
          badIntervalThreshold(isPhase ? originalData.name : originalParent.name, analysisConfig),
      badHint: 'badHint.badInterval',
      badHintArgs: { name: originalData.name }
    },
    durationAvg: {
      value: formatTimePeriod(originalData.durationAvg),
      bad:
        originalData.durationAvg >=
        badDurationThreshold(
          isPhase ? originalData.name : originalParent.name,
          analysisConfig,
          metadata
        ),
      badHint: 'badHint.badDuration',
      badHintArgs: { name: originalData.name }
    },
    durationMax: {
      value: formatTimePeriod(originalData.durationMax),
      bad:
        originalData.durationMax >=
        badDurationThreshold(
          isPhase ? originalData.name : originalParent.name,
          analysisConfig,
          metadata
        ),
      badHint: 'badHint.badDuration',
      badHintArgs: { name: originalData.name }
    },
    durationTotal: {
      value: formatTimePeriod(originalData.durationTotal)
    }
  };
}

function sortEventItems(items) {
  const order = {};
  metadata.allEventTypes.forEach((event, index) => (order[event] = index));
  items.sort((i1, i2) => order[i1.name.value] - order[i2.name.value]);
}

function buildTableData() {
  let data = [];
  originalData.value.forEach((originalParent) => {
    let mode = displayMode.value;
    if (mode === 'causeMode' && !parentHasGCCause(originalParent.self.name)) {
      return;
    }
    let parentData = null;
    const parentNeeded =
      mode === 'structuredMode' ||
      mode === 'causeMode' ||
      (mode === 'importantMode' &&
        metadata.importantEventTypes.indexOf(originalParent.self.name) >= 0) ||
      (mode === 'pauseMode' && metadata.mainPauseEventTypes.indexOf(originalParent.self.name) >= 0);
    if (parentNeeded) {
      parentData = formatDataItem(true, originalParent.self, null);
      data.push(parentData);
    }
    if (mode === 'structuredMode' || mode === 'causeMode') {
      parentData!.children = [];
    }
    if (mode === 'causeMode') {
      if (originalParent.causes) {
        originalParent.causes.forEach((originalCause) => {
          parentData.children.push(formatDataItem(false, originalCause, originalParent.self));
        });
      }
    } else if (originalParent.phases) {
      originalParent.phases.forEach((originalPhase) => {
        if (mode === 'structuredMode') {
          parentData.children.push(formatDataItem(true, originalPhase, originalParent.self));
        } else if (
          (mode === 'importantMode' &&
            metadata.importantEventTypes.indexOf(originalPhase.name) >= 0) ||
          (mode === 'pauseMode' && metadata.mainPauseEventTypes.indexOf(originalPhase.name) >= 0)
        ) {
          data.push(formatDataItem(true, originalPhase, originalParent.self));
        }
      });
    }
    if (parentNeeded && parentData!.children) {
      if (mode === 'structuredMode') {
        sortEventItems(parentData!.children);
      } else {
        parentData!.children.sort((d1, d2) => d2.count.value - d1.count.value);
      }
    }
  });
  sortEventItems(data);
  tableData.value = data;
}

function loadData() {
  loading.value = true;
  request('phaseStatistics', { ...GCLogData.analysisConfig.timeRange }).then((data) => {
    originalData.value = data.parents;
    buildTableData();
    loading.value = false;
  });
}

watch(
  () => GCLogData.analysisConfig,
  () => {
    loadData();
  }
);

onMounted(() => {
  loadData();
  mounted.value = true;
});
</script>
<template>
  <teleport :to="`#${headerBarId}`" v-if="mounted">
    <div style="display: flex; align-items: center">
      <el-select style="width: 220px" v-model="displayMode" @change="buildTableData">
        <el-option
          v-for="option in displayModeOptions"
          :key="option"
          :label="gct(`phaseStats.${option}`)"
          :value="option"
        ></el-option>
      </el-select>
    </div>
  </teleport>

  <el-table
    class="table"
    default-expand-all
    :header-cell-style="TABLE_HEADER_CELL_STYLE"
    :data="tableData"
    :show-overflow-tooltip="{ showArrow: false }"
    row-key="key"
    v-loading="loading"
  >
    <el-table-column
      v-for="(column, index) in columns"
      :label="gct(`phaseStats.${column}`)"
      :width="index === 0 ? 320 : ''"
    >
      <template #default="scope">
        <el-tooltip
          v-if="scope.row[column].pause"
          :content="gct('stwTooltip')"
          placement="left"
          :show-arrow="false"
        >
          <el-icon style="margin-right: 8px">
            <VideoPause />
          </el-icon>
        </el-tooltip>

        <ValueWithHint
          :value="scope.row[column].value"
          :hint="scope.row[column].hint ? gct(scope.row[column].hint) : undefined"
          :danger="scope.row[column].bad"
          :danger-hint="
            scope.row[column].badHint
              ? gct(scope.row[column].badHint, scope.row[column].badHintArgs)
              : undefined
          "
        >
        </ValueWithHint>
      </template>
    </el-table-column>
  </el-table>
</template>
<style scoped>
.table :deep(td .cell) {
  display: flex;
  align-items: center;
  overflow: hidden;
}

.table :deep(.el-table__expand-icon),
.table :deep(.el-table__indent),
.table :deep(.el-table__placeholder) {
  flex-shrink: 0;
}
</style>
