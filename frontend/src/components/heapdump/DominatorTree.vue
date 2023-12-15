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
import { prettyCount, prettyPercentage, prettySize } from '@/support/utils';
import { getIcon, ICONS } from '@/components/heapdump/icon-helper';
import { Search } from '@element-plus/icons-vue';
import { useSelectedObject } from '@/composables/heapdump/selected-object';
import CommonTable from '@/components/heapdump/CommonTable.vue';
import { hdt } from '@/components/heapdump/utils';
import { commonMenu as menu } from '@/components/heapdump/menu';
import { OBJECT_TYPE } from '@/components/heapdump/type';

const { selectedObjectId } = useSelectedObject();

const parameters = reactive({
  groupBy: 'NONE',
  searchText: '',
  searchType: 'by_name'
});

const searchText = ref('');

function updateSearchText() {
  parameters.searchText = searchText.value || '';
}

const className = {
  label: () => hdt('column.className'),
  minWidth: 250,
  content: (d) => d.label,
  icon: (d) =>
    parameters.groupBy === 'BY_CLASS'
      ? ICONS.objects.class
      : getIcon(d.gCRoot, d.objectType, d.objType),
  prefix: (d) => d.prefix,
  suffix: (d) => d.suffix
};

const objects = {
  label: () => hdt('column.objectCount'),
  width: 120,
  align: 'right',
  sortable: true,
  property: 'Objects',
  content: (d) => prettyCount(d.objects)
};

const shallowHeap = {
  label: 'Shallow Heap',
  width: 130,
  align: 'right',
  sortable: true,
  property: 'shallowHeap',
  content: (d) => prettySize(d.shallowSize)
};

const retainedHeap = {
  label: 'Retained Heap',
  width: 130,
  align: 'right',
  sortable: true,
  property: 'retainedHeap',
  content: (d) => prettySize(d.retainedSize)
};

const percentage = {
  label: () => hdt('column.percentage'),
  width: 130,
  align: 'right',
  sortable: true,
  property: 'percent',
  content: (d) => prettyPercentage(d.percent)
};

const columns1 = [className, shallowHeap, retainedHeap, percentage];

const columns2 = [className, objects, shallowHeap, retainedHeap, percentage];

const tableProps = ref({
  columns: columns1,

  apis: [
    {
      api: 'dominatorTree.roots',
      parameters: () => parameters,
      paged: true
    },
    {
      api: 'dominatorTree.children',
      parameters: (item) => {
        let idPathInResultTree = [];
        let p = item;
        do {
          idPathInResultTree.push(p.objectId);
          p = p.__meta.parent;
        } while (p);
        return {
          ...parameters,
          parentObjectId: item.objectId,
          idPathInResultTree: idPathInResultTree.reverse()
        };
      },
      paged: true
    }
  ],

  hasChildren: (d) => {
    if (parameters.groupBy === 'BY_PACKAGE') {
      return d.objectType == OBJECT_TYPE.PACKAGE;
    }
    return true;
  },

  defaultSortProperty: {
    prop: 'retainedHeap',
    order: 'descending'
  },

  sortParameterConverter: (sortProperty) => {
    return {
      sortBy: sortProperty.prop,
      ascendingOrder: sortProperty.order === 'ascending'
    };
  },

  onRowClick: (d) => {
    if (d.hasOwnProperty('objectId') && parameters.groupBy !== 'BY_PACKAGE') {
      selectedObjectId.value = d.objectId;
    }
  },

  menu,
  watch: [parameters]
});

watchEffect(() => {
  tableProps.value.columns = parameters.groupBy === 'NONE' ? columns1 : columns2;
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
        <el-text size="small" truncated>Group</el-text>

        <el-select
          v-model="parameters.groupBy"
          placeholder="Select"
          style="width: 105px"
          placement="bottom"
          size="small"
        >
          <el-option label="Object" value="NONE"></el-option>
          <el-option label="Class" value="BY_CLASS"></el-option>
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
              <el-option label="Class Name" value="by_name" />
              <el-option label="Shallow Heap" value="by_shallow_size" />
              <el-option label="Retained Heap" value="by_retained_size" />
              <el-option label="Percent" value="by_percent" />
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
