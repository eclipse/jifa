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
import DownloadLink from '@/components/common/DownloadLink.vue';
import { SetUp } from '@element-plus/icons-vue';
import { t } from '@/i18n/i18n';
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';
import { Phase, useAnalysisStore } from '@/stores/analysis';

const { request } = useAnalysisApiRequester();

const analysis = useAnalysisStore();
function clean() {
  request('clean').then(() => {
    useAnalysisStore().leaveGuard = false;
    location.reload();
  });
}
</script>
<template>
  <el-divider direction="vertical" />
  <DownloadLink />

  <template v-if="analysis.phase === Phase.SUCCESS || analysis.phase === Phase.FAILURE">
    <el-divider direction="vertical" />

    <el-button link class="ej-header-button" :icon="SetUp" @click="clean">
      {{ t('analysis.setting') }}
    </el-button>
  </template>
</template>
