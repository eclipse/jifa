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
import { useGCLogData } from '@/stores/gc-log-data';
import EventView from '@/components/gclog/EventView.vue';
import { TABLE_HEADER_CELL_STYLE } from '@/components/styles';
import { gct } from '@/i18n/i18n';
import { useDebouncedRef } from '@/composables/debounced-ref';

const { request } = useAnalysisApiRequester();

const GCLogData = useGCLogData();
const metadata = GCLogData.metadata;

const startTime = Math.floor(metadata.startTime / 1000);
const endTime = Math.ceil(metadata.endTime / 1000);

const filter = reactive({
  eventType: null as string | number,
  gcCause: null as string | number,
  logTimeLow: null as number | null,
  logTimeHigh: null as number | null,
  pauseTimeLow: null as number | null
});

const pagination = reactive({
  page: 1,
  pageSize: 50
});

const timeRange = ref([
  new Date(metadata.timestamp + metadata.startTime),
  new Date(metadata.timestamp + metadata.endTime)
]);
const startTimeLow = useDebouncedRef(startTime, 300);
const startTimeHigh = useDebouncedRef(endTime, 300);
const tableData = ref([]);
const totalSize = ref(0);
const pageSizes = [25, 50, 100];
const loading = ref(false);

function timeRangeChange(range) {
  if (range && range.length === 2) {
    filter.logTimeLow = range[0].getTime() - metadata.timestamp;
    filter.logTimeHigh = range[1].getTime() - metadata.timestamp;
  }
}

watch(startTimeLow, (v) => {
  if (v) {
    filter.logTimeLow = v * 1000;
  } else {
    filter.logTimeLow = null;
  }
});

watch(startTimeHigh, (v) => {
  if (v) {
    filter.logTimeHigh = v * 1000;
  } else {
    filter.logTimeHigh = null;
  }
});

watch(filter, () => {
  pagination.page = 1;
  load();
});

function disabledDate(time: Date) {
  let start = new Date(metadata.timestamp + metadata.startTime);
  start.setHours(0, 0, 0, 0);
  let end = new Date(metadata.timestamp + metadata.endTime);
  end.setHours(0, 0, 0, 0);
  return !(start <= time && time <= end);
}

function handleCurrentPageChange() {
  load();
}

function handlePageSizeChange() {
  pagination.page = 1;
  load();
}

function load() {
  loading.value = true;
  request('gcDetails', {
    filter,
    pagingRequest: pagination,
    config: GCLogData.analysisConfig
  }).then((pv) => {
    tableData.value = pv.data.map((e) => dealGCEvent(e));
    totalSize.value = pv.totalSize;
    loading.value = false;
  });
}

function dealGCEvent(gcEvent) {
  const result = {
    ...gcEvent,
    key: gcEvent.info.id + 1
  };
  if (gcEvent.phases.length > 0) {
    result.hasChildren = true;
  }
  return result;
}

function expandEvent(row, treeNode, resolve) {
  resolve(row.phases.map((e) => dealGCEvent(e)));
}

onMounted(() => {
  load();
});
</script>
<template>
  <div style="width: 100%; height: 100%; display: flex; flex-direction: column">
    <div style="flex-shrink: 0">
      <el-scrollbar>
        <div style="display: flex; justify-content: space-between">
          <div>
            <el-space>
              <el-text size="small" style="font-weight: 600" truncated>{{
                gct('detail.eventType')
              }}</el-text>
              <el-select size="small" v-model="filter.eventType" clearable>
                <el-option
                  v-for="option in metadata.parentEventTypes"
                  :key="option"
                  :label="option"
                  :value="option"
                ></el-option>
              </el-select>

              <el-text size="small" style="font-weight: 600; margin-left: 10px" truncated
                >{{ gct('gcCause') }}
              </el-text>
              <el-select size="small" v-model="filter.gcCause" clearable>
                <el-option
                  v-for="option in metadata.causes"
                  :key="option"
                  :label="option"
                  :value="option"
                ></el-option>
              </el-select>

              <el-text size="small" style="font-weight: 600; margin-left: 10px" truncated
                >{{ gct('detail.pauseTimeGE') }}
              </el-text>
              <el-input-number
                size="small"
                v-model="filter.pauseTimeLow"
                controls-position="right"
                :min="0"
                :step="100"
                value-on-clear="min"
              ></el-input-number>
            </el-space>
          </div>

          <div>
            <el-space>
              <el-date-picker
                size="small"
                type="datetimerange"
                :clearable="false"
                :start-placeholder="gct('detail.startTime')"
                :end-placeholder="gct('detail.endTime')"
                :disabled-date="disabledDate"
                v-model="timeRange"
                @change="timeRangeChange"
                v-if="metadata.timestamp >= 0"
              >
              </el-date-picker>

              <template v-else>
                <el-input-number
                  size="small"
                  controls-position="right"
                  :step="60"
                  :placeholder="gct('detail.startTime')"
                  :min="startTime"
                  :max="startTimeHigh"
                  v-model="startTimeLow"
                />
                <span style="margin: 0 12px">-</span>
                <el-input-number
                  size="small"
                  controls-position="right"
                  :step="60"
                  :placeholder="gct('detail.endTime')"
                  :min="startTimeLow"
                  :max="endTime"
                  v-model="startTimeHigh"
                />
              </template>
            </el-space>
          </div>
        </div>
      </el-scrollbar>
    </div>

    <el-table
      class="table"
      size="small"
      :header-cell-style="TABLE_HEADER_CELL_STYLE"
      highlight-current-row
      stripe
      lazy
      row-key="key"
      :data="tableData"
      :load="expandEvent"
      v-loading="loading"
    >
      <el-table-column :label="gct('detail.event')">
        <template #default="{ row }">
          <EventView :gcEvent="row.info" />
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        layout="total, sizes, prev, pager, next, jumper"
        background
        :small="true"
        :total="totalSize"
        v-model:page-size="pagination.pageSize"
        @update:page-size="handlePageSizeChange"
        :page-sizes="pageSizes"
        v-model:current-page="pagination.page"
        @update:current-page="handleCurrentPageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.table {
  flex-grow: 1;
  margin: 15px 0;
}

.table :deep(td .cell) {
  display: flex;
  align-items: center;
}

.pagination {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
}
</style>
