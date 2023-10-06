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
import { t } from '@/i18n/i18n';
import { useAnalysisStore } from '@/stores/analysis';
import { Download } from '@element-plus/icons-vue';

const analysis = useAnalysisStore();
</script>
<template>
  <el-button
    link
    tag="a"
    class="ej-header-button"
    :icon="Download"
    :href="`/jifa-api/files/${analysis.fileId}/download`"
    target="_blank"
    >{{ t('header.download') }}
  </el-button>
</template>
