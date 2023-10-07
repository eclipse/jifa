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
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';
import { useGCLogData } from '@/stores/gc-log-data';
import { formatSize, formatSizeSpeed } from '@/components/gclog/utils';
import { gct } from '@/i18n/i18n';
import ValueWithHint from '@/components/gclog/ValueWithHint.vue';
import { TABLE_HEADER_CELL_STYLE } from '@/components/styles';

const { request } = useAnalysisApiRequester();
const GCLogData = useGCLogData();

let stats;
let columns = [
  'objectCreationSpeed',
  'objectPromotionSpeed',
  'objectPromotionAvg',
  'objectPromotionMax'
];
const tableData = ref();
const loading = ref(false);

function processStats() {
  let capacityInfo = GCLogData.capacityInfo;
  if (!stats || !capacityInfo) {
    return;
  }
  let analysisConfig = GCLogData.analysisConfig;
  const youngCapacity = capacityInfo.youngCapacityAvg;
  const oldCapacity = capacityInfo.oldCapacityAvg;
  const heapCapacity = capacityInfo.heapCapacityAvg;
  const highPromotionThresholdPercent = analysisConfig.highPromotionThreshold / 100;
  tableData.value = [
    {
      objectCreationSpeed: {
        value: formatSizeSpeed(stats.objectCreationSpeed),
        bad: GCLogData.metadata!.generational
          ? youngCapacity >= 0 &&
            youngCapacity / stats.objectCreationSpeed <
              analysisConfig.youngGCFrequentIntervalThreshold
          : heapCapacity >= 0 &&
            heapCapacity / stats.objectCreationSpeed <
              analysisConfig.fullGCFrequentIntervalThreshold,
        badHint: 'badHint.badObjectAllocSpeed'
      },
      objectPromotionSpeed: {
        value: formatSizeSpeed(stats.objectPromotionSpeed),
        bad:
          oldCapacity > 0 &&
          stats.objectPromotionSpeed >
            (oldCapacity * highPromotionThresholdPercent) /
              analysisConfig.youngGCFrequentIntervalThreshold,
        badHint: 'badHint.badPromotionSpeed'
      },
      objectPromotionAvg: {
        value: formatSize(stats.objectPromotionAvg),
        bad:
          oldCapacity > 0 && stats.objectPromotionAvg > oldCapacity * highPromotionThresholdPercent,
        badHint: 'badHint.badPromotionSpeed'
      },
      objectPromotionMax: {
        value: formatSize(stats.objectPromotionMax),
        bad:
          oldCapacity > 0 &&
          stats.objectPromotionMax > oldCapacity * highPromotionThresholdPercent * 2,
        badHint: 'badHint.badSinglePromotion'
      }
    }
  ];
}

function loadData() {
  loading.value = true;
  request('objectStatistics', { ...GCLogData.analysisConfig.timeRange }).then((data) => {
    stats = data;
    processStats();
    loading.value = false;
  });
}

watch(
  () => GCLogData.capacityInfo,
  () => {
    processStats();
  }
);

watch(
  () => GCLogData.analysisConfig,
  () => {
    loadData();
  }
);

onMounted(() => {
  loadData();
});
</script>
<template>
  <el-table :header-cell-style="TABLE_HEADER_CELL_STYLE" v-loading="loading" :data="tableData">
    <el-table-column v-for="column in columns" :key="column" :label="gct(column)">
      <template #default="{ row }">
        <ValueWithHint
          :value="row[column].value"
          :danger="row[column].bad"
          :dangerHint="gct(row[column].badHint)"
        />
      </template>
    </el-table-column>
  </el-table>
</template>
