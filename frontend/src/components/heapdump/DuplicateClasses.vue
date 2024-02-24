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
import CommonTable from '@/components/heapdump/CommonTable.vue';
import {getIcon, ICONS} from '@/components/heapdump/icon-helper';
import {prettyCount} from '@/support/utils';
import {useSelectedObject} from '@/composables/heapdump/selected-object';
import {OBJECT_TYPE} from '@/components/heapdump/type';
import {Search} from '@element-plus/icons-vue';

const { selectedObjectId } = useSelectedObject();

const parameters = ref({
  searchText: '',
  searchType: 'BY_NAME'
});

const searchText = ref('');

function updateSearchText() {
  parameters.value.searchText = searchText.value || '';
}

const tableProps = ref({
  columns: [
    {
      label: () => 'Class Name / Class Loader',
      minWidth: 250,
      sortable: false,
      content: (d) => d.label,
      icon: (d) =>
        d.__meta.tier == 0 ? ICONS.objects.class : getIcon(d.gCRoot, OBJECT_TYPE.CLASSLOADER)
    },
    {
      label: () => 'Class Loader Count',
      width: 150,
      align: 'right',
      content: (d) => prettyCount(d.count)
    },
    {
      label: 'Defined Classes',
      width: 130,
      align: 'right',
      content: (d) => prettyCount(d.definedClassesCount)
    },
    {
      label: 'Object Count',
      width: 130,
      align: 'right',
      content: (d) => prettyCount(d.instantiatedObjectsCount)
    }
  ],

  apis: [
    {
      api: 'duplicatedClasses.classes',
      parameters: () => parameters.value,
      paged: true
    },
    {
      api: 'duplicatedClasses.classLoaders',
      parameters: (d) => {
        return {
          index: d.index
        };
      },
      paged: true
    }
  ],

  hasChildren: (d) => d.__meta.tier == 0 && d.count > 0,

  onRowClick: (d) => {
    if (d.hasOwnProperty('objectId')) {
      selectedObjectId.value = d.objectId;
    }
  },

  watch: [parameters]
});
</script>
<template>
  <div style="height: 100%; display: flex; flex-direction: column">
    <div
      style="
        height: 32px;
        flex-shrink: 0;
        padding-left: 8px;
        margin-bottom: 8px;
        overflow: hidden;
        display: flex;
        justify-content: right;
        align-items: center;
      "
    >
      <div>
        <el-input size="small" v-model="searchText" @change="updateSearchText">
          <template #prepend>
            <el-select
              v-model="parameters.searchType"
              style="width: 142px"
              size="small"
              placement="bottom"
            >
              <el-option label="Class Name" value="BY_NAME"></el-option>
              <el-option label="Class Loader Count" value="BY_CLASSLOADER_COUNT"></el-option>
            </el-select>
          </template>
          <template #append>
            <el-button :icon="Search as any" size="small" @click="updateSearchText" />
          </template>
        </el-input>
      </div>
    </div>
    <CommonTable v-bind="tableProps" />
  </div>
</template>
<style scoped>
:deep(.el-input-group__prepend) {
  background-color: var(--el-fill-color-blank);
}
</style>