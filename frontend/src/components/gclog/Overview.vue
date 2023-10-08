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
import { useGCLogData } from '@/stores/gc-log-data';
import { formatTimePeriod, formatTimeRange } from '@/components/gclog/utils';
import { Brush, Clock, Document, Timer } from '@element-plus/icons-vue';
import { useAnalysisStore } from '@/stores/analysis';
import { gct, t } from '@/i18n/i18n';

const analysis = useAnalysisStore();

const GCLogData = useGCLogData();
const metadata = GCLogData.metadata;

const logTimeRange = formatTimeRange(metadata.startTime, metadata.endTime, metadata.timestamp);
const logDuration = formatTimePeriod(metadata.endTime - metadata.startTime);

const configTimeRange = computed(() =>
  formatTimeRange(
    GCLogData.analysisConfig.timeRange.start,
    GCLogData.analysisConfig.timeRange.end,
    metadata.timestamp
  )
);

const configDuration = computed(() =>
  formatTimePeriod(
    GCLogData.analysisConfig.timeRange.end - GCLogData.analysisConfig.timeRange.start
  )
);
</script>
<template>
  <el-descriptions :column="2" direction="vertical" border>
    <el-descriptions-item>
      <template #label>
        <div class="label">
          <el-icon class="icon">
            <Document />
          </el-icon>
          {{ t('file.name') }}
        </div>
      </template>
      {{ analysis.filename }}
    </el-descriptions-item>

    <el-descriptions-item>
      <template #label>
        <div class="label">
          <el-icon class="icon">
            <Clock />
          </el-icon>
          {{ gct('logTimeRange') }}
        </div>
      </template>
      {{ logTimeRange }}{{ t('common.comma') }}{{ gct('duration').toLowerCase() }} {{ logDuration }}
    </el-descriptions-item>

    <el-descriptions-item>
      <template #label>
        <div class="label">
          <el-icon class="icon">
            <Brush />
          </el-icon>
          {{ gct('algorithm') }}
        </div>
      </template>
      {{ metadata.collector }}
    </el-descriptions-item>

    <el-descriptions-item>
      <template #label>
        <div class="label">
          <el-icon class="icon">
            <Timer />
          </el-icon>
          {{ gct('analysisTimeRange') }}
        </div>
      </template>
      <el-text
        type="primary"
        style="cursor: pointer"
        @click="GCLogData.toggleAnalysisConfigVisible()"
      >
        {{ configTimeRange }}
      </el-text>
      {{ t('common.comma') }}{{ gct('duration').toLowerCase() }} {{ configDuration }}
    </el-descriptions-item>
  </el-descriptions>
</template>
<style scoped>
.label {
  display: flex;
  align-items: center;
}

.icon {
  margin-right: 5px;
}
</style>
