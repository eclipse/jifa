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
import { formatPercentage, formatTimePeriod } from '@/components/gclog/utils';
import { currentLocale, gct } from '@/i18n/i18n';
import ValueWithHint from '@/components/gclog/ValueWithHint.vue';
import { TABLE_HEADER_CELL_STYLE } from '@/components/styles';
import * as echarts from 'echarts';
import { isDark } from '@/composables/theme';

const { request } = useAnalysisApiRequester();
const GCLogData = useGCLogData();
const metadata = GCLogData.metadata;

const loadingDistribution = ref(false);
const loadingStats = ref(false);

let pauseDistribution;

const partitions = [0, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000];
const columns = ['throughput', 'pauseAvg', 'pauseMedian', 'pauseP99', 'pauseP999', 'pauseMax'];
const tableData = ref();

const fixedColor = {
  'Young GC': '#5470c6',
  'Mixed GC': '#fac858',
  'Full GC': '#ee6666'
};

const autoColor = ['#91cc75', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'];

const chartRef = ref();
let chartInstance;

function getColumnHint(column) {
  if (column === 'throughput') {
    return gct(`pauseInfo.${column}Hint`);
  }
  return '';
}

function subArray(array, subArrayIndex) {
  const result = [];
  subArrayIndex.forEach((i) => result.push(array[i]));
  return result;
}

function formatYAxis(low, high) {
  if (typeof high === 'undefined') {
    return low >= 1000 ? `>${(low / 1000).toFixed(0)} s` : `>${low} ms`;
  } else if (low >= 1000) {
    return `${(low / 1000).toFixed(0)} ~ ${(high / 1000).toFixed(0)}s`;
  } else {
    return `${low} ~ ${high} ms`;
  }
}

function loadStats() {
  loadingStats.value = true;
  let analysisConfig = GCLogData.analysisConfig;
  request('pauseStatistics', { ...analysisConfig.timeRange }).then((data) => {
    tableData.value = [
      {
        throughput: {
          value: formatPercentage(data.throughput),
          bad:
            data.throughput >= 0 && data.throughput <= analysisConfig.badThroughputThreshold / 100,
          badHint: 'badHint.badThroughput'
        },
        pauseAvg: {
          value: formatTimePeriod(data.pauseAvg),
          bad: data.pauseAvg >= analysisConfig.longPauseThreshold,
          badHint: 'badHint.badPause'
        },
        pauseMedian: {
          value: formatTimePeriod(data.pauseMedian),
          bad: data.pauseMedian >= analysisConfig.longPauseThreshold,
          badHint: 'badHint.badPause'
        },
        pauseP99: {
          value: formatTimePeriod(data.pauseP99),
          bad: data.pauseP99 >= analysisConfig.longPauseThreshold,
          badHint: 'badHint.badPause'
        },
        pauseP999: {
          value: formatTimePeriod(data.pauseP999),
          bad: data.pauseP999 >= analysisConfig.longPauseThreshold,
          badHint: 'badHint.badPause'
        },
        pauseMax: {
          value: formatTimePeriod(data.pauseMax),
          bad: data.pauseMax >= analysisConfig.longPauseThreshold,
          badHint: 'badHint.badPause'
        }
      }
    ];
    loadingStats.value = false;
  });
}

function render() {
  if (chartInstance) {
    chartInstance.dispose();
  }

  chartInstance = echarts.init(chartRef.value, isDark.value ? 'dark' : null);

  const subArrayIndex = [];
  for (let i = 0; i < partitions.length; i++) {
    for (let key of Object.keys(pauseDistribution)) {
      if (pauseDistribution[key][i] !== 0) {
        subArrayIndex.push(i);
        break;
      }
    }
  }

  const yAxisData = subArray(
    partitions.map((value, index) => formatYAxis(partitions[index], partitions[index + 1])),
    subArrayIndex
  );

  const series = metadata.mainPauseEventTypes
    .filter((e) => pauseDistribution.hasOwnProperty(e))
    .map((name) => {
      const series: any = {
        name: name,
        type: 'bar',
        stack: 'total',
        label: {
          show: true
        },
        emphasis: {
          focus: 'series'
        },
        data: subArray(pauseDistribution[name], subArrayIndex).map((v) => (v === 0 ? undefined : v))
      };
      if (fixedColor.hasOwnProperty(name)) {
        series.itemStyle = {
          color: fixedColor[name]
        };
      }
      return series;
    });
  const option = {
    color: autoColor,
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
      name: gct('pauseInfo.pauseCount')
    },
    yAxis: {
      type: 'category',
      name: gct('pauseInfo.pauseTime'),
      data: yAxisData
    },
    series: series
  };

  chartInstance.setOption(option);
}

function loadDistribution() {
  loadingDistribution.value = true;

  request('pauseDistribution', {
    ...GCLogData.analysisConfig.timeRange,
    partitions
  }).then((data) => {
    pauseDistribution = data;
    render();
    loadingDistribution.value = false;
  });
}

function loadData() {
  loadDistribution();
  loadStats();
}

watch([currentLocale, isDark], () => {
  render();
});

watch(
  () => GCLogData.analysisConfig,
  () => {
    loadData();
  }
);

function resizeChart() {
  if (chartInstance) {
    chartInstance.resize();
  }
}

onMounted(() => {
  window.addEventListener('resize', resizeChart);
  loadData();
});

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart);
});
</script>
<template>
  <div style="height: 500px" ref="chartRef" v-loading="loadingDistribution" />
  <el-table :header-cell-style="TABLE_HEADER_CELL_STYLE" :data="tableData" v-loading="loadingStats">
    <el-table-column v-for="column in columns" :key="column">
      <template #header>
        <ValueWithHint :value="gct(`pauseInfo.${column}`)" :hint="getColumnHint(column)" />
      </template>
      <template #default="{ row }">
        <ValueWithHint
          :value="row[column].value"
          :danger="row[column].bad"
          :danger-hint="gct(row[column].badHint)"
        />
      </template>
    </el-table-column>
  </el-table>
</template>
