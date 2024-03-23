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
import { frameToString, monitorsToStrings } from '@/components/threaddump/util';
import {
  CirclePlus,
  CirclePlusFilled,
  Lock,
  Remove,
  RemoveFilled,
  VideoPause
} from '@element-plus/icons-vue';

const { request } = useAnalysisApiRequester();

const loading = ref(false);

const page = ref(1);
const pageSize = 8;
const totalSize = ref(0);
const moreThanOnePage = computed(() => totalSize.value > pageSize);
const tableData = ref();
const tableRef = ref();

let rowKey = 1;

let autoExpandLimit = 8;
const nextAutoExpandRow = ref();

function resetAutoExpand() {
  autoExpandLimit = 8;
  nextAutoExpandRow.value = null;
}

function autoExpand(row) {
  if (autoExpandLimit) {
    nextAutoExpandRow.value = row;
    autoExpandLimit--;
  } else {
    resetAutoExpand();
    loading.value = false;
  }
}

watchEffect(() => {
  let row = nextAutoExpandRow.value;
  if (row) {
    tableRef.value.store.loadOrToggle(row);
  }
});

function alertTypeOfMonitor(monitor) {
  if (monitor.indexOf('waiting') !== -1 || monitor.indexOf('parking') !== -1) {
    return 'warning';
  }
  return 'info';
}

function iconOfMonitors(monitors) {
  for (let i = 0; i < monitors.length; i++) {
    if (monitors[i].indexOf('waiting') !== -1 || monitors[i].indexOf('parking') !== -1) {
      return VideoPause;
    }
  }
  return Lock;
}

function buildFrame(frame) {
  return {
    ...frame,
    rowKey: rowKey++,
    frame: frameToString(frame),
    monitors: frame.monitors ? monitorsToStrings(frame.monitors) : null,
    hasChildren: !frame.end
  };
}

function loadChildrenData(parent, _, resolve) {
  loadBySummary({
    __summary: true,
    rowKey: rowKey++,
    parent,
    resolve,
    next: 1
  });
}

function loadBySummary(summary) {
  loadData(summary.parent.id, summary.next, summary);
}

function loadData(parentId, page, summary?) {
  loading.value = true;
  request('callSiteTree', {
    parentId,
    page,
    pageSize
  }).then((pageView) => {
    if (parentId === 0) {
      totalSize.value = pageView.totalSize;
      tableData.value = pageView.data.map(buildFrame);
      loading.value = false;
    } else {
      let callResolve = false;
      let table = tableRef.value.store.states.lazyTreeNodeMap.value[summary.parent.rowKey];
      if (!table) {
        summary.total = pageView.totalSize;
        table = [];
        callResolve = true;
      }

      if (table.length > 0) {
        table.splice(table.length - 1, 1);
      }

      pageView.data.forEach((frame) => {
        table.push(buildFrame(frame));
      });

      summary.size = table.length;
      summary.next++;

      if (pageView.totalSize > 1) {
        table.push(summary);
      }

      if (callResolve) {
        summary.resolve(table);
      }

      if (table.length === 1 && table[0].hasChildren) {
        autoExpand(table[0]);
      } else {
        resetAutoExpand();
        loading.value = false;
      }
    }
  });
}

onMounted(() => {
  loadData(0, page.value);
});
</script>
<template>
  <el-table
    stripe
    :show-header="false"
    v-bind="moreThanOnePage ? { height: `${40 * pageSize}px` } : {}"
    :data="tableData"
    row-key="rowKey"
    lazy
    :load="loadChildrenData"
    ref="tableRef"
    v-loading="loading"
  >
    <el-table-column>
      <template #default="{ row }">
        <template v-if="!row.__summary">
          <span>{{ row.frame }}</span>
          <el-popover
            width=""
            placement="right"
            :show-arrow="false"
            v-if="row.monitors && row.monitors.length"
          >
            <el-alert
              v-for="(monitor, index) in row.monitors"
              :key="monitor"
              :title="monitor"
              :type="alertTypeOfMonitor(monitor)"
              :closable="false"
              :style="index > 0 ? 'margin-top: 5px' : ''"
            />
            <template #reference>
              <el-icon style="margin-left: 10px; color: var(--el-color-warning)">
                <component :is="iconOfMonitors(row.monitors)" />
              </el-icon>
            </template>
          </el-popover>
        </template>

        <div
          :style="{
            display: 'flex',
            alignItems: 'center',
            cursor: row.size < row.total ? 'pointer' : 'default'
          }"
          @click="row.size < row.total ? loadBySummary(row) : void 0"
          v-else
        >
          <el-icon style="margin-right: 10px">
            <CirclePlusFilled style="color: var(--el-color-primary)" v-if="row.size < row.total" />
            <RemoveFilled style="color: var(--el-color-info)" v-else />
          </el-icon>
          {{ row.size }} / {{ row.total }}
        </div>
      </template>
    </el-table-column>

    <el-table-column width="150" prop="weight"> </el-table-column>
  </el-table>
  <div class="pagination" v-if="moreThanOnePage">
    <el-pagination
      layout="total, prev, pager, next"
      background
      :total="totalSize"
      :page-size="pageSize"
      v-model:current-page="page"
      @current-change="loadData(0, page)"
      :disabled="loading"
    />
  </div>
</template>
<style scoped>
:deep(td .cell) {
  display: flex;
  align-items: center;
}

:deep(.el-table__expand-icon),
:deep(.el-table__indent),
:deep(.el-table__placeholder) {
  flex-shrink: 0;
}

.pagination {
  margin-top: 15px;
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  overflow: hidden;
}
</style>
