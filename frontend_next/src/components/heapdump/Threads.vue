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
import { ref } from 'vue';
import ThreadIcon from '@/assets/heapdump/thread.gif';
import FrameIcon from '@/assets/heapdump/stack_frame.gif';
import { getOutboundIcon } from '@/components/heapdump/icon-helper';
import CommonTable from '@/components/heapdump/CommonTable.vue';
import { useSelectedObject } from '@/composables/heapdump/selected-object';
import { prettySize } from '@/support/utils';
import { Search } from '@element-plus/icons-vue';
import { commonMenu as menu } from '@/components/heapdump/menu';

const { selectedObjectId } = useSelectedObject();

const parameters = ref({
  searchText: '',
  searchType: 'BY_NAME'
});

const searchText = ref('');

function updateSearchText() {
  parameters.value.searchText = searchText.value || '';
}

const columns = [
  {
    label: () => 'Thread / Frame / Object',
    minWidth: 250,
    content: (d) => {
      let tier = d.__meta.tier;
      if (tier == 0) {
        return d.object;
      } else if (tier == 1) {
        return d.stack;
      } else {
        return d.label;
      }
    },
    icon: (d) => {
      let tier = d.__meta.tier;
      if (tier == 0) {
        return ThreadIcon;
      }
      if (tier == 1) {
        return FrameIcon;
      }
      return getOutboundIcon(d.gCRoot, d.objectType);
    },
    suffixMapper: (d) => {
      if (d.__meta.tier >= 2) {
        return d.suffix;
      }
    }
  },
  {
    label: 'Name',
    width: 130,
    align: 'right',
    property: 'name'
  },
  {
    label: 'Shallow Size',
    width: 130,
    align: 'right',
    property: 'shallowHeap',
    sortable: true,
    content: (d) => prettySize(d.shallowSize),
    contentOfSummary: (d) => prettySize(d.shallowHeap)
  },
  {
    label: 'Retained Size',
    width: 130,
    align: 'right',
    property: 'retainedHeap',
    sortable: true,
    content: (d) => prettySize(d.retainedSize),
    contentOfSummary: (d) => prettySize(d.retainedHeap)
  },
  {
    label: "Locals' Retained",
    width: 130,
    align: 'right',
    property: 'maxLocalsRetainedSize'
  },
  {
    label: 'Context Class Loader',
    width: 150,
    align: 'right',
    property: 'contextClassLoader'
  },
  {
    label: 'Daemon',
    width: 80,
    align: 'center',
    property: 'daemon'
  }
];

const tableProps = ref({
  columns,
  apis: [
    {
      api: 'threads',
      parameters: () => parameters.value,
      paged: true,
      summaryApi: 'threadsSummary',
      parametersOfSummaryApi: () => parameters.value
    },
    {
      api: 'stackTrace',
      parameters: (d) => {
        return {
          objectId: d.objectId
        };
      }
    },
    {
      api: 'locals',
      parameters: (d) => {
        return {
          objectId: d.__meta.parent.objectId,
          depth: d.__meta.index + 1,
          firstNonNativeFrame: d.firstNonNativeFrame
        };
      }
    },
    {
      api: 'outbounds',
      parameters: (d) => {
        return {
          objectId: d.objectId
        };
      },
      paged: true
    }
  ],

  hasChildren: (d) => {
    let tier = d.__meta.tier;
    if (tier == 0) {
      return d.hasStack;
    } else if (tier == 1) {
      return d.hasLocal;
    } else {
      return d.hasOutbound;
    }
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
    if (d.hasOwnProperty('objectId')) {
      selectedObjectId.value = d.objectId;
    }
  },

  menu,

  hasMenu(d) {
    return d.__meta.tier >= 2;
  },

  watch: [parameters],

  spanMethod({ row, columnIndex }) {
    let tier = row.__meta.tier;
    if (tier == 1) {
      if (columnIndex == 0) {
        return [1, 4];
      } else if (columnIndex >= 1 && columnIndex <= 3) {
        return [0, 0];
      }
    } else if (tier >= 2) {
      if (columnIndex == 0) {
        return [1, 2];
      } else if (columnIndex == 1) {
        return [0, 0];
      }
    }
    return [1, 1];
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
        justify-content: right;
        align-items: center;
      "
    >
      <div>
        <el-input size="small" v-model="searchText" @change="updateSearchText">
          <template #prepend>
            <el-select
              v-model="parameters.searchType"
              style="width: 155px"
              size="small"
              placement="bottom"
            >
              <el-option label="Thread Name" value="BY_NAME" />
              <el-option label="Context Class Loader" value="BY_CONTEXT_CLASSLOADER_NAME" />
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
    <CommonTable v-bind="tableProps" />
  </div>
</template>
<style scoped>
:deep(.el-input-group__prepend) {
  background-color: var(--el-fill-color-blank);
}
</style>
