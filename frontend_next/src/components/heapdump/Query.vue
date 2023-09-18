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
import { nextTick, ref, watch, computed } from 'vue';
import { Search } from '@element-plus/icons-vue';
import CommonTable from '@/components/heapdump/CommonTable.vue';
import { hdt } from '@/components/heapdump/utils';
import { getIcon } from '@/components/heapdump/icon-helper';
import { prettySize } from '@/support/utils';
import { Link } from '@element-plus/icons-vue';
import { t } from '@/i18n/i18n';
import { useSelectedObject } from '@/composables/heapdump/selected-object';
import { commonMenu as menu } from '@/components/heapdump/menu';

const { selectedObjectId } = useSelectedObject();

const oqlLink =
  'https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.mat.ui.help%2Freference%2Foqlsyntax.html&cp%3D55_4_2';
const sqlLink = 'https://github.com/vlsi/mat-calcite-plugin#sample';

const input = ref(null);
const type = ref('oql');
const query = ref('');
const last = ref('');

watch(type, (t) => {
  tableProps.value.apis[0].api = t === 'oql' ? 'oql' : 'sql';
  query.value = '';
  last.value = '';
});

const link = computed(() => (type.value === 'oql' ? oqlLink : sqlLink));

const showDataTable = ref(false);
const textResult = ref(null);
const processing = ref(false);

function doQuery() {
  if (query.value) {
    let q = query.value.trim();
    if (q && q !== last.value) {
      last.value = q;
      textResult.value = null;
      processing.value = true;
      showDataTable.value = false;
      nextTick(() => {
        showDataTable.value = true;
      });
    }
  }
}

const className = {
  label: () => hdt('column.className'),
  minWidth: 250,
  content: (d) => d.label,
  icon: (d) => getIcon(d.gCRoot, d.objectType, d.objType),
  suffixMapper: (d) => d.suffix
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

const TREE = 1;
const TABLE = 2;
const TEXT = 3;

const columnsOfTreeResult = ref([className, shallowHeap, retainedHeap]);
const columnsOfTextResult = ref([]);

const tableProps = ref({
  columns: [],

  apis: [
    {
      api: 'oql',
      parameters() {
        return {
          [type.value]: query.value
        };
      },
      respMapper(r) {
        if (r.type == TEXT) {
          showDataTable.value = false;
          textResult.value = [{ text: r.text }];
          processing.value = false;
          input.value.focus();
          return {
            data: [],
            totalSize: 0
          };
        }
        pushHistory(query.value.trim());
        processing.value = false;
        input.value.focus();
        return r.pv;
      },
      paged: true
    },
    {
      api: 'outbounds',
      parameters(d) {
        return {
          objectId: d.objectId
        };
      },
      paged: true
    }
  ],

  columnAdjuster(r) {
    if (r.type == TREE) {
      if (tableProps.value.columns === columnsOfTreeResult.value) {
        return false;
      }
      tableProps.value.hasChildren = (d) => d.hasOutbound;
      tableProps.value.columns = columnsOfTreeResult.value;
    } else if (r.type == TABLE) {
      tableProps.value.hasChildren = undefined;
      let columns = [];
      for (let i = 0; i < r.columns.length; i++) {
        const index = i;
        columns.push({
          label: r.columns[index],
          content: (d) => d.values[index] || 'null'
        });
      }
      tableProps.value.columns = columns;
    } else if (r.type == TEXT) {
      tableProps.value.hasChildren = undefined;
      tableProps.value.columns = columnsOfTextResult.value;
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

  onRowClick(d) {
    if (d.hasOwnProperty('objectId')) {
      selectedObjectId.value = d.objectId;
    }
  },

  menu,

  hasMenu(d) {
    return d.hasOwnProperty('objectId');
  }
});

const oqlHistory = ref([]);
const sqlHistory = ref([]);

function pushHistory(q) {
  let history = type.value === 'oql' ? oqlHistory : sqlHistory;
  q = q.trim();
  for (let i = 0; i < history.value.length; i++) {
    if (history.value[i].value === q) {
      return;
    }
  }
  history.value.unshift({ value: q });
  if (history.value.length > 8) {
    history.value.pop();
  }
}

function matchHistory(q) {
  let history = type.value === 'oql' ? oqlHistory : sqlHistory;
  return history.value.filter((i) => i.value.toLowerCase().indexOf(q.toLowerCase()) == 0);
}

function fetchSuggestions(q, cb) {
  cb(matchHistory(q));
}
</script>
<template>
  <div style="height: 100%; display: flex; flex-direction: column">
    <div
      style="
        flex-shrink: 0;
        margin-bottom: 8px;
        overflow: hidden;
        display: flex;
        align-items: center;
      "
    >
      <el-select v-model="type" placeholder="Select" style="width: 145px" fit-input-width>
        <template #prefix>
          <el-icon class="ej-link-icon" size="16">
            <Link
              @click="
                (e) => {
                  window.open(link);
                  e.stopPropagation();
                }
              "
            />
          </el-icon>
        </template>
        <template #default>
          <el-option label="MAT OQL" value="oql" />
          <el-option label="Calcite SQL" value="sql" />
        </template>
      </el-select>
      <el-autocomplete
        ref="input"
        v-model="query"
        @keyup.enter="doQuery"
        :placeholder="hdt('placeholder.query', type === 'oql' ? ['MAT OQL'] : ['Calcite SQL'])"
        style="flex-grow: 1; margin-left: 7px"
        :fetch-suggestions="fetchSuggestions"
        :trigger-on-focus="false"
        clearable
        :disabled="processing"
        fit-input-width
      >
        <template #append>
          <el-button :icon="Search" @click="doQuery" />
        </template>
      </el-autocomplete>
    </div>

    <div style="flex-grow: 1; overflow: hidden">
      <CommonTable v-bind="tableProps" v-if="showDataTable" />

      <el-table
        size="small"
        :data="textResult"
        v-if="textResult"
        style="height: 100%"
        :header-cell-style="{
          background: 'var(--el-fill-color-light)',
          color: 'var(--el-text-color-primary)'
        }"
      >
        <el-table-column :label="t('common.result')">
          <div style="white-space: pre-wrap">
            {{ textResult[0].text }}
          </div>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
<style scoped>
.ej-link-icon:hover {
  color: var(--el-menu-hover-text-color);
  cursor: pointer;
}

:deep(.el-input-group__prepend) {
  background-color: var(--el-fill-color-blank);
}
</style>
