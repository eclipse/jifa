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
import { t } from '@/i18n/i18n';
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';
import { ICONS } from '@/components/heapdump/icon-helper';
import CommonContextMenu from '@/components/common/CommonContextMenu.vue';
import type { Item, PageView, Pagination, TableProperty } from '@/components/heapdump/common-table';
import { useElementSize } from '@vueuse/core';

const props = withDefaults(defineProps<TableProperty>(), {
  showOverflowTooltip: true,
  spanMethod: () => [1, 1]
});

const { request } = useAnalysisApiRequester();

const pageSize = 50;
const baseWidths = [];
const expandedWidths = reactive([]);
const realWidths = reactive([]);

const tableDiv = ref();
const { width: tableWidth } = useElementSize(tableDiv);

const table = ref(null);
const tableData = ref([]);
const filteredTableData = computed(() => {
  if (props.dataFilter) {
    return tableData.value.filter((d) => props.dataFilter(d));
  } else {
    return tableData.value;
  }
});
const loading = ref(false);
const render = ref(true);

let rowKey = 1;
let rootSummaryItem;

function resetWidths() {
  baseWidths.splice(0, baseWidths.length);
  expandedWidths.splice(0, expandedWidths.length);
  realWidths.splice(0, realWidths.length);

  props.columns.forEach((c) => {
    baseWidths.push('');
    if (c.width) {
      if (typeof c.width === 'string') {
        realWidths.push((tableWidth.value * parseFloat(c.width)) / 100);
      } else {
        realWidths.push(c.width);
      }
    } else {
      realWidths.push('');
    }
    expandedWidths.push(0);
  });
}

function adjustWidth(column) {
  let index = column.no;
  if (column.realWidth && typeof column.realWidth === 'number') {
    if (!baseWidths[index]) {
      baseWidths[index] = column.realWidth;
    }
    realWidths[index] = baseWidths[index] + expandedWidths[index];
  }
}

let sortParameters;

if (props.defaultSortProperty && props.sortParameterConverter) {
  sortParameters = props.sortParameterConverter(props.defaultSortProperty);
}

function labelOf(column) {
  let label = column.label;
  return typeof label === 'string' ? label : label();
}

function iconOf(column, item) {
  if (typeof column.icon === 'function') {
    return column.icon(item);
  }
  return column.icon;
}

function contentOf(column, item) {
  if (column.content) {
    return column.content(item);
  }
  return item[column.property];
}

function contentOfSummaryOf(column, item) {
  return column.contentOfSummary(item);
}

function prefixOf(column, item) {
  return column.prefix(item);
}

function suffixOf(column, item) {
  return column.suffix(item);
}

function apiOf(tier) {
  return props.apis[Math.min(tier, props.apis.length - 1)];
}

async function buildSummaryItem(tier, parent?: Item): Promise<Item> {
  let api = apiOf(tier);
  let summary: any = {};
  let total = undefined;

  if (api.summaryApi) {
    let parameters = api.parametersOfSummaryApi ? api.parametersOfSummaryApi(parent) : {};
    loading.value = true;
    summary = await request(api.summaryApi, parameters);
    total = summary.totalSize;
  }

  return {
    __meta: {
      rowKey: rowKey++,
      tier,
      parent,
      pagination: api.paged ? { next: 1, size: 0, total: total } : undefined,
      summary: true
    },
    ...summary
  };
}

function buildDataItem(data, summaryItem: Item, index): Item {
  let item: Item = {
    __meta: {
      rowKey: rowKey++,
      tier: summaryItem.__meta.tier,
      parent: summaryItem.__meta.parent,
      index: index
    },
    ...data
  };
  item.__meta.hasChildren = props.hasChildren ? props.hasChildren(item) : false;
  item.__hasChildren = item.__meta.hasChildren;
  return item;
}

async function loadChildren(parent: Item, treeNode, resolve) {
  const summaryItem = await buildSummaryItem(parent.__meta.tier + 1, parent);
  load(summaryItem, resolve);
}

function load(summaryItem: Item, resolve?) {
  if (summaryItem.__meta.pagination?.total == 0) {
    if (loading.value) {
      loading.value = false;
    }
    return;
  }

  loading.value = true;

  let tier = summaryItem.__meta.tier;
  let index = Math.min(tier, props.apis.length - 1);

  let api = props.apis[index];
  let parameters = api.parameters ? api.parameters(summaryItem.__meta.parent) : {};
  let pageParameters = api.paged ? { page: summaryItem.__meta.pagination?.next, pageSize } : {};

  parameters = {
    ...parameters,
    ...sortParameters,
    ...pageParameters
  };

  request(api.api, parameters).then(async (result) => {
    if (summaryItem === rootSummaryItem && tableData.value.length == 0 && props.columnAdjuster) {
      if (props.columnAdjuster(result)) {
        render.value = false;
        await nextTick();
        resetWidths();
        render.value = true;
        await nextTick();
      }
    }

    if (api.respMapper) {
      result = api.respMapper(result);
    }

    let callResolve = false;
    let data;
    if (tier == 0) {
      data = tableData.value;
    } else {
      data = (table.value as any).store.states.lazyTreeNodeMap.value[
        summaryItem.__meta.parent?.__meta.rowKey
      ];
      if (!data) {
        data = [];
        callResolve = true;
      }
    }

    if (data.length > 0) {
      data.splice(data.length - 1, 1);
    }

    let index = data.length;
    if (api.paged) {
      let pv = result as PageView;
      pv.data.forEach((d) => data.push(buildDataItem(d, summaryItem, index++)));

      if (data.length > 1) {
        let pagination = summaryItem.__meta.pagination as Pagination;
        pagination.size = data.length;
        pagination.total = pv.totalSize;
        pagination.next++;
        data.push(summaryItem);
      }
    } else {
      (result as []).forEach((d) => data.push(buildDataItem(d, summaryItem, index++)));
    }

    if (callResolve) {
      resolve(data);
    }

    nextTick(() => (loading.value = false));
  });
}

async function reset() {
  resetWidths();
  tableData.value.splice(0, tableData.value.length);
  rootSummaryItem = await buildSummaryItem(0);
}

async function handleSortChange(sort) {
  if (sort.order == null) {
    sort = props.defaultSortProperty;
  }
  sortParameters = props.sortParameterConverter(sort);
  await reset();
  load(rootSummaryItem);
}

if (props.watch && props.watch.length > 0) {
  watch(props.watch, async () => {
    render.value = false;
    await reset();
    nextTick(async () => {
      render.value = true;
      load(rootSummaryItem);
    });
  });
}

watch(tableWidth, async () => {
  if (rootSummaryItem) {
    resetWidths();
  } else {
    await reset();
    load(rootSummaryItem);
  }
});

function alignClass(align) {
  if (align === 'center') {
    return 'ej-table-cell-center';
  } else if (align === 'right') {
    return 'ej-table-cell-right';
  }
  return '';
}

const contextmenu = ref();

function rowContextmenuCallback(row, column, event) {
  if (contextmenu.value) {
    if (!props.hasMenu || props.hasMenu(row)) {
      let data = props.menuDataConverter ? props.menuDataConverter(row) : row;
      contextmenu.value.show(event, data);
    } else {
      event.preventDefault();
    }
  } else {
    event.preventDefault();
  }
}

function hasMore(item: Item) {
  return item.__meta.pagination?.total > item.__meta.pagination?.size;
}
</script>
<template>
  <CommonContextMenu :menu="menu" ref="contextmenu" v-if="menu" />

  <div ref="tableDiv" style="width: 100%; height: 100%; overflow: hidden; position: relative">
    <el-table
      ref="table"
      class="ej-table"
      border
      resizable
      empty-text=" "
      size="small"
      :show-overflow-tooltip="showOverflowTooltip ? { showArrow: false } : false"
      highlight-current-row
      :data="filteredTableData"
      lazy
      stripe
      row-key="__meta.rowKey"
      :span-method="spanMethod"
      :load="loadChildren"
      v-loading="loading"
      :indent="20"
      :default-sort="defaultSortProperty"
      :tree-props="{ hasChildren: '__hasChildren' }"
      v-if="render"
      @sort-change="handleSortChange"
      @row-contextmenu="rowContextmenuCallback"
      :header-cell-style="{
        background: 'var(--el-fill-color-light)',
        color: 'var(--el-text-color-primary)'
      }"
      @row-click="(row) => (onRowClick ? onRowClick(row) : void 0)"
    >
      <el-table-column
        v-for="(c, i) in columns"
        :width="realWidths[i]"
        :fixed="c.fix || false"
        :min-width="c.minWidth ? c.minWidth : ''"
        :sortable="c.sortable ? 'custom' : false"
        :prop="c.property"
        :class-name="alignClass(c.align)"
        :header-align="c.align"
        :label="labelOf(c)"
      >
        <template #default="scope">
          <template v-if="!scope.row.__meta.summary">
            <div
              class="ej-table-content-div"
              :style="{
                paddingRight: i == columns.length - 1 && c.align === 'right' ? '5px' : 0,
                cursor: onRowClick ? 'pointer' : 'auto',
                overflow: 'hidden'
              }"
            >
              <img v-if="c.icon" :src="iconOf(c, scope.row)" alt="" class="ej-img" />
              <div class="ej-table-content-div">
                <span class="ej-prefix" v-if="c.prefix && prefixOf(c, scope.row)">{{
                  prefixOf(c, scope.row)
                }}</span
                >{{ contentOf(c, scope.row)
                }}<span class="ej-suffix" v-if="c.suffix && suffixOf(c, scope.row)"
                  >{{ suffixOf(c, scope.row) }}
                </span>
              </div>
            </div>
          </template>

          <template v-else>
            <template v-if="c.contentOfSummary">
              <div
                class="ej-table-content-div"
                :style="{
                  paddingRight: i == columns.length - 1 && c.align === 'right' ? '5px' : 0
                }"
              >
                {{ contentOfSummaryOf(c, scope.row) }}
              </div>
            </template>
            <template v-else-if="i === 0">
              <el-tooltip
                placement="right"
                :content="`『 ${t('common.clickToLoadMore')} 』`"
                :show-arrow="false"
                :disabled="!hasMore(scope.row)"
              >
                <div
                  class="ej-table-content-div"
                  style="cursor: pointer"
                  @click="hasMore(scope.row) ? load(scope.row) : void 0"
                >
                  <img
                    alt=""
                    :src="hasMore(scope.row) ? ICONS.misc.sumPlusIcon : ICONS.misc.sumIcon"
                    class="ej-img"
                  />
                  <span
                    >{{
                      scope.row.__meta.pagination.size.toLocaleString()
                    }}&nbsp;<strong>/</strong>&nbsp;{{
                      scope.row.__meta.pagination.total.toLocaleString()
                    }}</span
                  >
                </div>
              </el-tooltip>
            </template>
          </template>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
<style scoped>
.ej-table {
  height: 100%;
}

.ej-table :deep(td .cell) {
  display: flex;
  align-items: center;
}

.ej-table :deep(.el-table__expand-icon),
.ej-table :deep(.el-table__indent),
.ej-table :deep(.el-table__placeholder) {
  flex-shrink: 0;
}

.ej-table :deep(th .el-space__item:nth-last-child(1)) {
  margin-right: 0 !important;
}

.ej-table-content-div {
  display: flex;
  align-items: center;
  overflow-x: auto;
}

.ej-table-content-div::-webkit-scrollbar {
  display: none;
}

:deep(.ej-table-cell-center) > .cell {
  justify-content: center;
}

:deep(.ej-table-cell-right) > .cell {
  justify-content: right;
}

.ej-img {
  margin-right: 5px;
}

.ej-prefix {
  margin-right: 5px;
  font-weight: bold;
}

.ej-suffix {
  margin-left: 5px;
  font-weight: bold;
  color: var(--el-text-color-secondary);
}
</style>
