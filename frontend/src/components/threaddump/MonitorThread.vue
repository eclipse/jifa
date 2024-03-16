<!--
    Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<script setup lang="ts">
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';

const props = defineProps({
  id: {
    required: true
  },
  title: {
    type: String,
    required: true
  }
});

const { request } = useAnalysisApiRequester();

const loading = ref(false);

const pageSize = 10;
const states = ref();
const stateData = ref();

function toggleContent(row, state) {
  if (row.contentLoaded) {
    row.contentVisible = !row.contentVisible;
    return;
  }

  stateData.value[state].loading = true;
  let id = row.id;
  request('rawContentOfThread', {
    id
  }).then((content) => {
    row.content = content.join('\n');
    row.contentLoaded = true;
    row.contentVisible = true;
    stateData.value[state].loading = false;
  });
}

function loadThreads(state) {
  let data = stateData.value[state];
  data.loading = true;
  request('threadsByMonitor', {
    id: props.id,
    state,
    page: data.page,
    pageSize
  }).then((pageView) => {
    data.tableData = pageView.data.map((thread) => ({
      ...thread,
      content: '',
      contentLoaded: false,
      contentVisible: false
    }));
    data.loading = false;
  });
}

function loadStates() {
  loading.value = true;
  request('threadCountsByMonitor', {
    id: props.id
  }).then((data) => {
    let _states = [];
    let _stateData = {};
    for (let k in data) {
      if (data[k] > 0) {
        _states.push(k);
        _stateData[k] = {
          page: 1,
          totalSize: data[k],
          moreThanOnePage: data[k] > pageSize,
          tableData: [],
          loading: false
        };
      }
    }
    states.value = _states;
    stateData.value = _stateData;
    loading.value = false;

    for (let k in _stateData) {
      loadThreads(k);
    }
  });
}

watch([props], () => {
  loadStates();
});

onMounted(() => {
  loadStates();
});
</script>
<template>
  <el-alert :title="title" type="success" :closable="false" />

  <div v-loading="loading">
    <div style="margin-top: 20px" v-for="state in states" :key="state">
      <el-tag style="margin-bottom: 10px" disable-transitions>{{ state }}</el-tag>
      <el-table
        stripe
        :show-header="false"
        :max-height="`${40 * pageSize}px`"
        v-bind="stateData[state].moreThanOnePage ? { height: `${40 * pageSize}px` } : {}"
        :data="stateData[state].tableData"
        v-loading="stateData[state].loading"
      >
        <el-table-column>
          <template #default="{ row }">
            <span class="clickable" @click="toggleContent(row, state)">{{ row.name }}</span>
            <div class="content" v-if="row.contentVisible && row.contentLoaded">
              <el-scrollbar>
                {{ row.content }}
              </el-scrollbar>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination" v-if="stateData[state].moreThanOnePage">
        <el-pagination
          layout="total, prev, pager, next"
          background
          :total="stateData[state].totalSize"
          :page-size="pageSize"
          v-model:current-page="stateData[state].page"
          @current-change="loadThreads(state)"
          :disabled="stateData[state].loading"
        />
      </div>
    </div>
  </div>
</template>
<style scoped>
.pagination {
  margin-top: 15px;
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  overflow: hidden;
}

.content {
  margin-top: 5px;
  border-radius: 8px;
  padding: 7px;
  background-color: rgba(var(--el-color-primary-rgb), 0.1);
  white-space: pre;
  font-size: 14px;
  line-height: 1.5;
  overflow-y: auto;
}
</style>
