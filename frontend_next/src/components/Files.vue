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
import { useRouter } from 'vue-router';
import { Calendar, Delete, Histogram, MoreFilled, Operation } from '@element-plus/icons-vue';
import Download from 'vue-material-design-icons/Download.vue';
import Upload from 'vue-material-design-icons/Upload.vue';
import FileTransferForm from '@/components/forms/FileTransferForm.vue';
import { fileTypeMap } from '@/composables/file-types';
import axios from 'axios';
import { prettySize } from '@/support/utils';
import { useEnv } from '@/stores/env';
import { t } from '@/i18n/i18n';
import { TABLE_HEADER_CELL_STYLE } from '@/components/styles';

const env = useEnv();
const router = useRouter();
const type = ref('');
const transferFormVisible = ref(false);

watch(type, () => {
  page.value = 1;
  queryFiles();
});

let tableData = ref([]);

const page = ref(1);
const pageSize = ref(25);
const pageSizes = [25, 50, 100];
const totalSize = ref(0);

const loading = ref(false);

function queryFiles() {
  loading.value = true;
  axios
    .get('/jifa-api/files', {
      params: {
        type: type.value,
        page: page.value,
        pageSize: pageSize.value
      }
    })
    .then((resp) => {
      tableData.value = resp.data.data;
      totalSize.value = resp.data.totalSize;
      loading.value = false;
    });
}

function deleteFile(id) {
  axios.delete(`/jifa-api/files/${id}`).then(() => {
    if (tableData.value.length == 1) {
      page.value = Math.max(1, page.value - 1);
    }
    queryFiles();
  });
}

function handleCurrentPageChange() {
  queryFiles();
}

function handlePageSizeChange() {
  page.value = 1;
  queryFiles();
}

function transferCompletionCallback(id) {
  transferFormVisible.value = false;
  axios.get(`/jifa-api/files/${id}`).then((resp) => {
    let file = resp.data;
    analyze(file.type, file.uniqueName);
  });
}

function analyze(type: string, uniqueName: string) {
  router.push({ path: `/${fileTypeMap.get(type).routePath}/${uniqueName}` });
}

onMounted(() => {
  queryFiles();
});
</script>

<template>
  <div class="ej-common-view-div ej-file-view-div">
    <!-- Should put the form inside the div or transition is not working -->
    <FileTransferForm
      @transfer-completion-callback="(id) => transferCompletionCallback(id)"
      v-model:visible="transferFormVisible"
      v-if="transferFormVisible"
    />

    <div class="ej-file-view-bar">
      <el-space>
        <el-text style="font-weight: 600">{{ t('file.type') }}</el-text>
        <el-select v-model="type" placeholder="Select" style="width: 130px">
          <el-option :label="t('file.all')" value="" />
          <el-option
            v-for="[key, meta] in fileTypeMap"
            :key="key"
            :label="t(`file.${meta.labelKey}`)"
            :value="key"
          ></el-option>
        </el-select>
      </el-space>

      <el-button style="margin-left: 10px" type="primary" plain @click="transferFormVisible = true">
        {{ t('file.new') }}
        <el-icon style="margin-left: 5px" size="16">
          <Upload />
        </el-icon>
      </el-button>
    </div>

    <el-table
      class="ej-file-table"
      :header-cell-style="TABLE_HEADER_CELL_STYLE"
      stripe
      size="large"
      :show-overflow-tooltip="{ showArrow: false }"
      :data="tableData"
      v-loading="loading"
    >
      <el-table-column type="index" width="50" label="#" />

      <el-table-column min-width="300" :label="t('file.name')" prop="originalName" />

      <el-table-column width="150" :label="t('file.type')">
        <template #default="{ row }">
          <el-tag size="small" type="info" disable-transitions
            >{{ t(`file.${fileTypeMap.get(row.type).labelKey}`) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column width="150" :label="t('file.size')">
        <template #default="{ row }">
          {{ prettySize(row.size) }}
        </template>
      </el-table-column>

      <el-table-column width="200" prop="createdTime">
        <template #header>
          <div class="ej-file-table-header">
            <el-icon style="margin-right: 8px" size="18">
              <Calendar />
            </el-icon>
            {{ t('file.uploadedTime') }}
          </div>
        </template>
      </el-table-column>

      <el-table-column width="180" fixed="right">
        <template #header>
          <div class="ej-file-table-header">
            <el-icon style="margin-right: 8px" size="18">
              <Operation />
            </el-icon>
            {{ t('file.operations') }}
          </div>
        </template>
        <template #default="{ row }">
          <el-space :size="4">
            <el-button size="small" type="primary" plain @click="analyze(row.type, row.uniqueName)"
              >{{ t('file.analyze') }}
              <el-icon class="el-icon--right">
                <Histogram />
              </el-icon>
            </el-button>
            <el-popover
              placement="right"
              :popper-style="{ 'min-width': '60px', width: '60px', padding: '5px 0' }"
              trigger="click"
              :show-arrow="false"
            >
              <template #reference>
                <el-icon class="ej-file-operations" size="16">
                  <MoreFilled />
                </el-icon>
              </template>
              <template #default>
                <div
                  class="ej-file-operation"
                  @click="window.open(`/jifa-api/files/${row.id}/download`)"
                >
                  <el-icon>
                    <Download style="height: 14px" />
                  </el-icon>
                </div>
                <div class="ej-file-operation" @click="deleteFile(row.id)">
                  <el-icon>
                    <Delete />
                  </el-icon>
                </div>
              </template>
            </el-popover>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <div class="ej-file-tale-pagination">
      <el-pagination
        layout="total, sizes, prev, pager, next, jumper"
        background
        :total="totalSize"
        v-model:page-size="pageSize"
        @update:page-size="handlePageSizeChange"
        :page-sizes="pageSizes"
        v-model:current-page="page"
        @update:current-page="handleCurrentPageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.ej-file-view-div {
  display: flex;
  flex-direction: column;
}

.ej-file-view-bar {
  flex-shrink: 0;
  padding-left: 12px;
}

.ej-file-table {
  flex-grow: 1;
  margin: 15px 0;
}

.ej-file-table-header {
  display: flex;
  align-items: center;
}

.ej-file-operations {
  rotate: 90deg;
}

.ej-file-operations:hover {
  color: var(--el-menu-hover-text-color);
  cursor: pointer;
}

.ej-file-operation {
  padding: 5px 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: var(--el-text-color-regular);
  cursor: pointer;
}

.ej-file-operation:hover {
  background-color: var(--el-menu-hover-bg-color);
  color: var(--el-menu-hover-text-color);
}

.ej-file-tale-pagination {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
}
</style>
