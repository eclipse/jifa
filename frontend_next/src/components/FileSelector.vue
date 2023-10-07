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
import axios from 'axios';
import { FileType } from '@/composables/file-types';
import { TABLE_HEADER_CELL_STYLE } from '@/components/styles';
import { t } from '@/i18n/i18n';
import { prettySize } from '@/support/utils';

const props = defineProps({
  visible: Boolean,
  type: {
    required: true
  },
  preSelected: {
    type: Array,
    required: false
  }
});

const emit = defineEmits(['update:visible', 'selectionCallback']);

let tableData = ref([]);

const page = ref(1);
const pageSize = ref(8);
const totalSize = ref(0);

const loading = ref();

const selectedUniqueNames = ref(new Set());

function isPreSelected(file) {
  return props.preSelected && props.preSelected.includes(file.uniqueName);
}

function isChecked(file) {
  return selectedUniqueNames.value.has(file.uniqueName) || isPreSelected(file);
}

function handleCheckedChange(file, b) {
  if (b) {
    selectedUniqueNames.value.add(file.uniqueName);
  } else {
    selectedUniqueNames.value.delete(file.uniqueName);
  }
}

function queryFiles() {
  loading.value = true;
  axios
    .get('/jifa-api/files', {
      params: {
        type: (props.type as FileType).key,
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

function handleCurrentPageChange() {
  queryFiles();
}

function confirm() {
  emit('selectionCallback', Array.from(selectedUniqueNames.value));
}

onMounted(() => {
  queryFiles();
});
</script>
<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="(newValue: boolean) => $emit('update:visible', newValue)"
  >
    <el-table
      height="432"
      :header-cell-style="TABLE_HEADER_CELL_STYLE"
      stripe
      :show-overflow-tooltip="{ showArrow: false }"
      :data="tableData"
      :select-on-indeterminate="false"
      v-loading="loading"
    >
      <el-table-column width="55">
        <template #default="{ row }">
          <template v-if="isChecked(row)">
            <el-checkbox
              checked
              :disabled="isPreSelected(row)"
              @change="(b) => handleCheckedChange(row, b)"
            />
          </template>
          <template v-else>
            <el-checkbox @change="(b) => handleCheckedChange(row, b)" />
          </template>
        </template>
      </el-table-column>

      <el-table-column :label="t('file.name')" prop="originalName" />

      <el-table-column width="100" :label="t('file.size')">
        <template #default="{ row }">
          {{ prettySize(row.size) }}
        </template>
      </el-table-column>

      <el-table-column width="180" :label="t('file.uploadedTime')" prop="createdTime" />
    </el-table>

    <div class="pagination">
      <el-pagination
        layout="prev, pager, next"
        background
        :total="totalSize"
        v-model:page-size="pageSize"
        v-model:current-page="page"
        @update:current-page="handleCurrentPageChange"
      />

      <el-button type="primary" @click="confirm">{{ t('common.confirm') }}</el-button>
    </div>
  </el-dialog>
</template>
<style scoped>
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
}
</style>
