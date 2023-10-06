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
import { prettyCount, prettySize } from '@/support/utils';
import { getIcon } from '@/components/heapdump/icon-helper';
import { Search } from '@element-plus/icons-vue';
import { useSelectedObject } from '@/composables/heapdump/selected-object';
import CommonTable from '@/components/heapdump/CommonTable.vue';
import { hdt } from '@/components/heapdump/utils';
import { OBJECT_TYPE } from '@/components/heapdump/type';
import { commonMenu as menu } from '@/components/heapdump/menu';

const { selectedObjectId } = useSelectedObject();

const parameters = reactive({
  groupBy: 'BY_CLASS',
  searchText: '',
  searchType: 'BY_NAME'
});

const searchText = ref('');

function updateSearchText() {
  parameters.searchText = searchText.value || '';
}

const column1 = {
  label: () => {
    switch (parameters.groupBy) {
      case 'BY_CLASS':
        return hdt('column.className');
      case 'BY_CLASSLOADER':
        return hdt('column.classLoader');
      case 'BY_SUPERCLASS':
        return hdt('column.superClass');
      case 'BY_PACKAGE':
        return hdt('column.package');
    }
  },
  minWidth: 250,
  sortable: false,
  property: 'id',
  content: (d) => d.label,
  icon: (d) => getIcon(false, d.type, false)
};

const objects = {
  label: () => hdt('column.objectCount'),
  width: 120,
  align: 'right',
  sortable: true,
  property: 'numberOfObjects',
  content: (d) => prettyCount(d.numberOfObjects)
};

const shallowHeap = {
  label: 'Shallow Heap',
  width: 130,
  align: 'right',
  sortable: true,
  property: 'shallowSize',
  content: (d) => prettySize(d.shallowSize)
};

const retainedHeap = {
  label: 'Retained Heap',
  width: 130,
  align: 'right',
  sortable: true,
  property: 'retainedSize',
  content: (d) =>
    d.retainedSize >= 0 ? prettySize(d.retainedSize) : '≥ ' + prettySize(-d.retainedSize)
};

const columnsByClass = [column1, objects, shallowHeap, retainedHeap];
const columnsBySuperClass = [column1, objects, shallowHeap];
const columnsByClassLoader = [column1, objects, shallowHeap, retainedHeap];
const columnsByPackage = [column1, objects, shallowHeap];

const tableProps = ref({
  columns: columnsByClass,

  apis: [
    {
      api: 'histogram',
      parameters: () => parameters,
      paged: true
    },
    {
      api: 'histogram.children',
      parameters: (d) => {
        return {
          groupBy: parameters.groupBy,
          parentObjectId: d.objectId
        };
      },
      paged: true
    }
  ],

  hasChildren: (d) => d.type !== OBJECT_TYPE.CLASS,

  defaultSortProperty: {
    prop: 'shallowSize',
    order: 'descending'
  },

  sortParameterConverter: (sortProperty) => {
    return {
      sortBy: sortProperty.prop,
      ascendingOrder: sortProperty.order === 'ascending'
    };
  },

  onRowClick: (d) => {
    if (d.hasOwnProperty('objectId')) {
      selectedObjectId.value = d.objectId;
    }
  },

  menu,

  menuDataConverter(d) {
    if (parameters.groupBy !== 'BY_CLASS') {
      return d;
    }
    return {
      ...d,
      __byHistogram: true
    };
  },

  watch: [parameters]
});

watchEffect(() => {
  switch (parameters.groupBy) {
    case 'BY_CLASS':
      tableProps.value.columns = columnsByClass;
      break;
    case 'BY_CLASSLOADER':
      tableProps.value.columns = columnsByClassLoader;
      break;
    case 'BY_SUPERCLASS':
      tableProps.value.columns = columnsBySuperClass;
      break;
    case 'BY_PACKAGE':
      tableProps.value.columns = columnsByPackage;
      break;
  }
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
        justify-content: space-between;
        align-items: center;
      "
    >
      <el-space>
        <el-text size="small">Group</el-text>
        <el-select
          v-model="parameters.groupBy"
          placeholder="Select"
          style="width: 105px"
          placement="bottom"
          size="small"
        >
          <el-option label="Class" value="BY_CLASS"></el-option>
          <el-option label="Super Class" value="BY_SUPERCLASS"></el-option>
          <el-option label="Class Loader" value="BY_CLASSLOADER"></el-option>
          <el-option label="Package" value="BY_PACKAGE"></el-option>
        </el-select>
      </el-space>

      <div>
        <el-input size="small" v-model="searchText" @change="updateSearchText">
          <template #prepend>
            <el-select
              v-model="parameters.searchType"
              style="width: 116px"
              size="small"
              placement="bottom"
            >
              <el-option label="Class Name" value="BY_NAME" />
              <el-option label="Object Count" value="BY_OBJ_NUM" />
              <el-option label="Shallow Heap" value="BY_SHALLOW_SIZE" />
              <el-option label="Retained Heap" value="BY_RETAINED_SIZE" />
            </el-select>
          </template>
          <template #append>
            <el-button :icon="Search as any" size="small" @click="updateSearchText" />
          </template>
        </el-input>
      </div>
    </div>
    <div style="flex-grow: 1; overflow: hidden">
      <CommonTable v-bind="tableProps" />
    </div>
  </div>
</template>
<style scoped>
:deep(.el-input-group__prepend) {
  background-color: var(--el-fill-color-blank);
}
</style>
