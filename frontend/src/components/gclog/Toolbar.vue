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
import { gct, t } from '@/i18n/i18n';
import DownloadLink from '@/components/common/DownloadLink.vue';
import { ArrowLeftBold, List, Rank, SetUp } from '@element-plus/icons-vue';
import { useGCLogData } from '@/stores/gc-log-data';
import { Phase, useAnalysisStore } from '@/stores/analysis';
import { useRoute } from 'vue-router';

const analysis = useAnalysisStore();
const GCLogData = useGCLogData();

const route = useRoute();
const showComparisonView = route.query.hasOwnProperty('compareTo');

function gotoComparison() {
  window.open(`${analysis.target}?compareTo=`);
}
</script>
<template>
  <template v-if="!showComparisonView">
    <el-divider direction="vertical" />
    <DownloadLink />

    <template v-if="analysis.phase === Phase.SUCCESS">
      <el-divider direction="vertical" />

      <el-button link class="ej-header-button" :icon="Rank" @click="gotoComparison">
        {{ t('analysis.comparison') }}
      </el-button>

      <el-divider direction="vertical" />

      <el-button
        link
        class="ej-header-button"
        :icon="SetUp"
        @click="GCLogData.toggleAnalysisConfigVisible()"
      >
        {{ t('analysis.setting') }}
      </el-button>

      <el-divider direction="vertical" />

      <el-button
        link
        class="ej-header-button"
        :icon="GCLogData.showDetails ? ArrowLeftBold : List"
        @click="GCLogData.toggleDetails()"
      >
        {{ gct(GCLogData.showDetails ? 'header.backToMainView' : 'header.showDetails') }}
      </el-button>
    </template>
  </template>
</template>
