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
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';
import { gct } from '@/i18n/i18n';
import { TABLE_HEADER_CELL_STYLE } from '@/components/styles';
import { useGCLogData } from '@/stores/gc-log-data';
import { formatSize, hasOldGC } from '@/components/gclog/utils';
import ValueWithHint from '@/components/gclog/ValueWithHint.vue';

const { request } = useAnalysisApiRequester();

const GCLogData = useGCLogData();
const metadata = GCLogData.metadata;

const tableData = ref();
const metrics = ref([]);
const loading = ref(false);

function getValueHint(generation, metric) {
  if (
    generation === 'metaspace' &&
    metric === 'capacityAvg' &&
    !metadata.metaspaceCapacityReliable
  ) {
    return 'memoryStats.metaspaceCapacity';
  }
}

function badValueCheck(row, metric, generation, heapCapacity) {
  const analysisConfig = GCLogData.analysisConfig;
  if (metric === 'usedAvgAfterFullGC' || metric === 'usedAvgAfterOldGC') {
    // check leak
    if (row[metric] < 0 || row.capacityAvg < 0) {
      return { bad: false };
    }
    const percentage = (row[metric] / row.capacityAvg) * 100;
    return {
      bad:
        (generation === 'old' && percentage >= analysisConfig.highOldUsageThreshold) ||
        (generation === 'humongous' && percentage >= analysisConfig.highHumongousUsageThreshold) ||
        (generation === 'heap' && percentage >= analysisConfig.highHeapUsageThreshold) ||
        (generation === 'metaspace' && percentage >= analysisConfig.highMetaspaceUsageThreshold),
      badHint: gct('badHint.badUsageAfterGC')
    };
  } else if (metric === 'capacityAvg' && (generation === 'young' || generation === 'old')) {
    // check small generation
    if (row[metric] < 0 || heapCapacity < 0) {
      return { bad: false };
    }
    const percentage = (row[metric] / heapCapacity) * 100;
    return {
      bad: percentage < analysisConfig.smallGenerationThreshold,
      badHint: gct(`badHint.${generation}TooSmall`)
    };
  }
}

function getMetricHint(metric) {
  const hint = [gct(`memoryStats.${metric}Hint`)];
  if (metadata.collector === 'G1 GC' && metric === 'capacityAvg') {
    hint.push(gct('memoryStats.g1DynamicCapacity'));
  }
  return hint;
}

function loadData() {
  loading.value = true;
  request('memoryStatistics', { ...GCLogData.analysisConfig.timeRange }).then((data) => {
    let generations = [];
    if (metadata.generational) {
      generations.push('young');
      generations.push('old');
    }
    if (metadata.collector === 'G1 GC' && metadata.logStyle === 'unified') {
      generations.push('humongous');
    }
    generations.push('heap', 'metaspace');

    metrics.value = ['capacityAvg', 'usedMax', 'usedAvgAfterFullGC'];
    if (hasOldGC(metadata)) {
      metrics.value.push('usedAvgAfterOldGC');
    }

    let d = [];
    const capacityData = {};
    const heapCapacity = data.heap.capacityAvg;
    generations.forEach((generation) => {
      capacityData[generation + 'CapacityAvg'] = data[generation].capacityAvg;
      let item: any = {};
      metrics.value.forEach((metric) => {
        item[metric] = {
          value: formatSize(data[generation][metric]),
          hint: getValueHint(generation, metric),
          ...badValueCheck(data[generation], metric, generation, heapCapacity)
        };
      });
      item.generation = generation;
      if (generation === 'humongous') {
        item.hint = 'generation.humongousHint';
      }
      d.push(item);
    });

    GCLogData.setCapacityData(capacityData);

    tableData.value = d;
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
});
</script>
<template>
  <el-table :header-cell-style="TABLE_HEADER_CELL_STYLE" :data="tableData" v-loading="loading">
    <el-table-column :label="gct('memoryStats.memoryArea')">
      <template #default="scope">
        <ValueWithHint
          :value="scope.row.generation ? gct(`generation.${scope.row.generation}`) : undefined"
          :hint="scope.row.hint ? gct(scope.row.hint) : undefined"
        />
      </template>
    </el-table-column>
    <el-table-column v-for="metric in metrics" :key="metric">
      <template #header>
        <ValueWithHint :value="gct(`memoryStats.${metric}`)" :hint="getMetricHint(metric)" />
      </template>
      <template #default="scope">
        <ValueWithHint
          :value="scope.row[metric].value"
          :hint="scope.row[metric].hint ? gct(scope.row[metric].hint) : undefined"
          :danger="scope.row[metric].bad"
          :danger-hint="scope.row[metric].badHint"
        />
      </template>
    </el-table-column>
  </el-table>
</template>
