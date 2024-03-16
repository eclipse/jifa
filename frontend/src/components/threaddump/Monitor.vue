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
import { rawMonitorToString } from '@/components/threaddump/util';
import { Lock } from '@element-plus/icons-vue';
import MonitorThread from '@/components/threaddump/MonitorThread.vue';

const { request } = useAnalysisApiRequester();

const dialogVisible = ref(false);

const loading = ref(false);

const page = ref(1);
const pageSize = 8;
const totalSize = ref(0);
const moreThanOnePage = computed(() => totalSize.value > pageSize);
const tableData = ref();

function loadMonitors() {
  loading.value = true;
  request('monitors', {
    page: page.value,
    pageSize
  }).then((pageView) => {
    totalSize.value = pageView.totalSize;
    tableData.value = pageView.data.map((monitor) => ({
      ...monitor,
      content: rawMonitorToString(monitor)
    }));
    loading.value = false;
  });
}

const selectedMonitorId = ref();
const dialogTitle = ref('');
function showMonitorThreadDialog(row) {
  selectedMonitorId.value = row.id;
  dialogTitle.value = row.content;
  dialogVisible.value = true;
}

onMounted(() => {
  loadMonitors();
});
</script>
<template>
  <el-dialog v-model="dialogVisible">
    <MonitorThread :id="selectedMonitorId" :title="dialogTitle" />
  </el-dialog>

  <el-table
    stripe
    :show-header="false"
    v-bind="moreThanOnePage ? { height: `${40 * pageSize}px` } : {}"
    :data="tableData"
    v-loading="loading"
  >
    <el-table-column>
      <template #default="{ row }">
        <div class="clickable" style="display: flex; align-items: center">
          <el-icon>
            <Lock />
          </el-icon>
          <span style="margin-left: 8px" @click="showMonitorThreadDialog(row)">{{
            row.content
          }}</span>
        </div>
      </template>
    </el-table-column>
  </el-table>
  <div class="pagination" v-if="moreThanOnePage">
    <el-pagination
      layout="total, prev, pager, next"
      background
      :total="totalSize"
      :page-size="pageSize"
      v-model:current-page="page"
      @current-change="loadMonitors"
      :disabled="loading"
    />
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
</style>
