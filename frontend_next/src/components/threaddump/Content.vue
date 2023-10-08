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
import { tdt } from '@/i18n/i18n';

const { request } = useAnalysisApiRequester();

const lineNo = ref(1);
const lineLimit = 512;
const reachEnd = ref(false);

const loadedContent = ref('');
const loading = ref(false);

function loadMore() {
  loading.value = true;
  request('content', {
    lineNo: lineNo.value,
    lineLimit
  }).then((data) => {
    let { content } = data;
    if (content && content.length > 0) {
      loadedContent.value += content.join('\n');
      loadedContent.value += '\n';
      lineNo.value += content.length;
    }
    reachEnd.value = data.end;
    loading.value = false;
  });
}

onMounted(() => {
  loadMore();
});
</script>
<template>
  <div class="container">
    <el-scrollbar max-height="500">
      <div class="content">
        {{ loadedContent }}
      </div>
    </el-scrollbar>

    <div style="margin-top: 15px; display: flex; justify-content: center" v-if="!reachEnd">
      <el-button type="primary" @click="loadMore" :loading="loading">
        {{ loadedContent.length ? tdt('loadMoreFileContent') : tdt('loadFileContent') }}
      </el-button>
    </div>
  </div>
</template>
<style scoped>
.container {
  margin: 0 8px;
  border-radius: 6px 6px 8px 8px;
  padding: 7px;
  border-top: 8px solid var(--el-color-primary);
  background-color: rgba(var(--el-color-primary-rgb), 0.1);
}

.content {
  white-space: pre;
  font-size: 14px;
  line-height: 1.5;
}
</style>
