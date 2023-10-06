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
import { ref } from 'vue';
import { useGCLogData } from '@/stores/gc-log-data';
import { cloneDeep } from 'lodash';
import { gct, t } from '@/i18n/i18n';
import ValueWithHint from '@/components/gclog/ValueWithHint.vue';
import { useRouter } from 'vue-router';

const GCLogData = useGCLogData();
const metadata = GCLogData.metadata;
const useUptime = GCLogData.useUptime;

const analysisConfig = ref(cloneDeep(GCLogData.analysisConfig));
if (GCLogData.selectedTimeRange) {
  analysisConfig.value.timeRange.start = GCLogData.selectedTimeRange.start;
  analysisConfig.value.timeRange.end = GCLogData.selectedTimeRange.end;
}

const router = useRouter();

const start = ref(Math.floor(analysisConfig.value.timeRange.start / 1000));
const end = ref(Math.ceil(analysisConfig.value.timeRange.end / 1000));

function startChange(low) {
  analysisConfig.value.timeRange.start = low * 1000;
}

function endChange(end) {
  analysisConfig.value.timeRange.end = end * 1000;
}

function disabledData(time: Date) {
  let start = new Date(metadata.timestamp + metadata.startTime);
  start.setHours(0, 0, 0, 0);
  let end = new Date(metadata.timestamp + metadata.endTime);
  end.setHours(0, 0, 0, 0);
  return !(start <= time && time <= end);
}

const timeRange = ref([
  new Date(metadata.timestamp + analysisConfig.value.timeRange.start),
  new Date(metadata.timestamp + analysisConfig.value.timeRange.end)
]);

function timeRangeChange([start, end]) {
  const min = metadata.timestamp;
  const max = metadata.timestamp + metadata.endTime;
  if (start.getTime() < min) {
    start = new Date(min);
  } else if (start.getTime() > max) {
    start = new Date(max);
  }
  if (end.getTime() < min) {
    end = new Date(min);
  } else if (end.getTime() > max) {
    end = new Date(max);
  }
  timeRange.value = [new Date(start), new Date(end)];
  analysisConfig.value.timeRange = {
    start: start.getTime() - min,
    end: end.getTime() - min
  };
}

function applyConfig() {
  GCLogData.setAnalysisConfig(cloneDeep(analysisConfig.value));
  GCLogData.toggleAnalysisConfigVisible();

  router.push({
    query: {
      start: analysisConfig.value.timeRange.start,
      end: analysisConfig.value.timeRange.end
    }
  });
}
</script>
<template>
  <el-dialog width="520px" :title="t('analysis.setting')" v-model="GCLogData.analysisConfigVisible">
    <div style="display: flex; justify-content: space-around">
      <el-form style="width: 400px" label-position="top" :model="analysisConfig as any">
        <el-form-item>
          <template #label>
            <ValueWithHint
              :value="gct('analysisTimeRange') + (useUptime ? '(s)' : '')"
              :hint="gct('analysisTimeRangeChooseHint')"
            />
          </template>

          <template v-if="useUptime">
            <el-input-number
              controls-position="right"
              :placeholder="gct('detail.startTime')"
              :min="Math.floor(metadata.startTime / 1000)"
              :max="end"
              v-model="start"
              @change="startChange"
            />
            <span style="margin: 0 12px">-</span>
            <el-input-number
              controls-position="right"
              :placeholder="gct('detail.endTime')"
              :min="start"
              :max="Math.ceil(metadata.endTime / 1000)"
              v-model="end"
              @change="endChange"
            />
          </template>

          <el-date-picker
            type="datetimerange"
            :clearable="false"
            :start-placeholder="gct('detail.startTime')"
            :end-placeholder="gct('detail.endTime')"
            :disabled-date="disabledData"
            v-model="timeRange"
            @change="timeRangeChange"
            v-else
          />
        </el-form-item>

        <el-form-item>
          <template #label>
            <ValueWithHint
              :value="gct('longPauseThreshold')"
              :hint="gct('longPauseThresholdHint')"
            />
          </template>

          <el-input-number
            controls-position="right"
            :min="0"
            :step="100"
            v-model="analysisConfig.longPauseThreshold"
          />
        </el-form-item>

        <el-form-item v-if="metadata.generational">
          <template #label>
            <ValueWithHint
              :value="gct('youngGCFrequentIntervalThreshold')"
              :hint="gct('youngGCFrequentIntervalThresholdHint')"
            />
          </template>

          <el-input-number
            controls-position="right"
            :min="0"
            :step="1000"
            v-model="analysisConfig.youngGCFrequentIntervalThreshold"
          />
        </el-form-item>

        <el-form-item v-if="metadata.collector === 'G1 GC' || metadata.collector === 'CMS GC'">
          <template #label>
            <ValueWithHint
              :value="gct('oldGCFrequentIntervalThreshold')"
              :hint="gct('oldGCFrequentIntervalThresholdHint')"
            />
          </template>

          <el-input-number
            controls-position="right"
            :min="0"
            :step="15000"
            v-model="analysisConfig.oldGCFrequentIntervalThreshold"
          />
        </el-form-item>

        <el-form-item>
          <template #label>
            <ValueWithHint
              :value="gct('fullGCFrequentIntervalThreshold')"
              :hint="gct('fullGCFrequentIntervalThresholdHint')"
            />
          </template>

          <el-input-number
            controls-position="right"
            :min="0"
            :step="60000"
            v-model="analysisConfig.fullGCFrequentIntervalThreshold"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="applyConfig">{{ t('common.confirm') }}</el-button>
        </el-form-item>
      </el-form>
    </div>
  </el-dialog>
</template>
