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
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';
import { useGCLogData } from '@/stores/gc-log-data';
import { COLORS, formatTimeRange } from '@/components/gclog/utils';
import { currentLocale, gct, t } from '@/i18n/i18n';
import * as echarts from 'echarts';
import { WarningFilled } from '@element-plus/icons-vue';
import { isDark } from '@/composables/theme';

const { request } = useAnalysisApiRequester();

const GCLogData = useGCLogData();
const useUptime = GCLogData.metadata.timestamp < 0;
const loading = ref(false);
const noProblem = ref(true);
const sites = ref([]);
const problem = ref([]);
const suggestions = ref([]);
const chartRef = ref();
let chartInstance;
let diagnoseInfo;

function dealMostSeriousProblem(mostSeriousProblem) {
  sites.value = mostSeriousProblem.sites;
  suggestions.value = mostSeriousProblem.suggestions.map((suggestion) => ({
    name: suggestion.name,
    params: suggestion.params
  }));

  const time = mostSeriousProblem.sites
    .map((range) => formatTimeRange(range.start, range.end, GCLogData.metadata.timestamp))
    .map((s, index) => `$$${s}|setTimeRangeByIndex|${index}$$`)
    .join(', ');
  const string = gct('diagnose.problemTemplate', {
    time: time,
    problem: t(mostSeriousProblem.problem.name, mostSeriousProblem.problem.params)
  });
  problem.value = parseStringWithAction(string);
}

function parseStringWithAction(s) {
  return s.split('$$').map((split, index) => {
    if (index % 2 === 0) {
      return { text: split };
    } else {
      const parts = split.split('|');
      return {
        text: parts[0],
        action: () => {
          setTimeRangeByIndex(parts[2]);
        }
      };
    }
  });
}

function dealGraph(seriousProblems) {
  nextTick(() => {
    if (chartInstance) {
      chartInstance.dispose();
    }
    chartInstance = echarts.init(chartRef.value, isDark.value ? 'dark' : null);
    chartInstance.setOption({
      color: COLORS,
      legend: {
        top: 10,
        orient: 'horizontal'
      },
      xAxis: {},
      yAxis: {
        type: 'value',
        show: false,
        min: 0,
        max: 1
      },
      series: []
    });

    const option = {
      xAxis: {
        type: useUptime ? 'value' : 'time',
        min: transformTime(GCLogData.analysisConfig.timeRange.start),
        max: transformTime(GCLogData.analysisConfig.timeRange.end)
      },
      series: Object.keys(seriousProblems).map((type, index) => {
        return {
          symbol: 'circle',
          symbolSize: 5,
          name: gct('diagnose.abnormal.' + type),
          type: 'scatter',
          sampling: 'lttb',
          data: seriousProblems[type].map((x) => [transformTime(x), index * 0.2])
        };
      })
    };

    chartInstance.setOption(option, { replaceMerge: ['xAxis', 'series'] });
  });
}

function transformTime(time) {
  return useUptime ? time / 1000 : time + GCLogData.metadata.timestamp;
}

function setTimeRangeByIndex(index) {
  const timeRange = sites.value[parseInt(index)];
  GCLogData.toggleAnalysisConfigVisible(timeRange);
}

function processDiagnoseInfo() {
  noProblem.value = Object.keys(diagnoseInfo.seriousProblems).length === 0;
  if (noProblem.value) {
    if (chartInstance) {
      chartInstance.dispose();
      chartInstance = null;
    }
    loading.value = false;
    return;
  }
  dealMostSeriousProblem(diagnoseInfo.mostSeriousProblem);
  dealGraph(diagnoseInfo.seriousProblems);
}

function loadData() {
  loading.value = true;
  request('diagnoseInfo', { config: GCLogData.analysisConfig }).then((data) => {
    diagnoseInfo = data;
    processDiagnoseInfo();
    loading.value = false;
  });
}

watch(
  () => GCLogData.analysisConfig,
  () => {
    loadData();
  }
);

watch([currentLocale, isDark], () => {
  processDiagnoseInfo();
});

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
  <div v-loading="loading">
    <el-text v-if="noProblem">{{ gct('diagnose.noProblem') }}</el-text>
    <div v-else>
      <div style="height: 150px" ref="chartRef" />

      <el-divider style="margin: 15px 0" />

      <div style="padding: 0 10px">
        <el-space direction="vertical" alignment="normal" size="large">
          <el-space>
            <el-text size="large">
              <el-icon>
                <WarningFilled />
              </el-icon>
            </el-text>

            <el-text size="large">
              <template v-for="part in problem">
                <el-text
                  size="large"
                  type="primary"
                  tag="b"
                  style="cursor: pointer"
                  @click="part.action"
                  v-if="part.action"
                >
                  {{ part.text }}
                </el-text>
                <el-text size="large" v-else>{{ part.text }}</el-text>
              </template>
            </el-text>
          </el-space>

          <el-text size="large" tag="b">{{ gct('diagnose.solution') }}</el-text>

          <el-text size="large" v-for="(suggestion, index) in suggestions" :key="suggestion">
            {{ index + 1 }}. {{ t(suggestion.name, suggestion.params) }}
          </el-text>
        </el-space>
      </div>
    </div>
  </div>
</template>
<style scoped>
:deep(.el-space .el-space__item:last-child) {
  padding-bottom: 0 !important;
}
</style>
