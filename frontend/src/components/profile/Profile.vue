<!--
    Copyright (c) 2024 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->

<script setup lang="ts">
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';
import '@/components/profile/flame-graph.js';
import { toReadableValue } from '@/components/profile/utils';
import { CircleCloseFilled, FullScreen, Search, Filter } from '@element-plus/icons-vue';
import { t } from '@/i18n/i18n';
import type { Column } from 'element-plus';

const { request } = useAnalysisApiRequester();

const loading = ref(false);
const selectedDimensionIndex = ref(null);
const selectedFilterIndex = ref(0);
const flameGraph = ref(null);
const perfDimensions = ref([]);
const symbolTable = ref({});
const threadSplit = ref(null);
const totalWeight = ref(0);
const filterValuesMap = ref([]);
const filterValueList = ref([]);
const topFilterValueList = ref([]);
const filter = ref(null);
const toggleFilterValuesChecked = ref(true);
const flameGraphDataSource = ref(null);
const flameGraphEmptyData = ref({ format: 'line', data: [] });
const taskName = ref(null);
const flameGraphModalVisible = ref(false);
const hasData = ref(false);

onMounted(() => {
  loading.value = true;
  let options = {};
  request('metadata', options).then((metadata) => {
    loading.value = false;
    perfDimensions.value = metadata.perfDimensions;
    selectedDimensionIndex.value = 0;
    selectedFilterIndex.value = 0;
    queryGraph();
  });
});

const checkedCount = computed(() => {
  let count = 0;
  for (let v of filterValueList.value) {
    if (v.checked) {
      count++;
    }
  }
  return count;
});

function rootTextGenerator(ds: any, information: any) {
  return (
    'Total ' +
    perfDimensions.value[selectedDimensionIndex.value].key +
    ': ' +
    format(information.totalWeight)
  );
}

function textGenerator(ds: any, frame: number) {
  return symbolTable.value[frame];
}

function titleGenerator(ds: any, frame: number, information: any) {
  const text = information.text;
  let i1 = text.lastIndexOf('.');
  if (i1 > 0) {
    let i2 = text.lastIndexOf('.', i1 - 1);
    if (i2 > 0) {
      if (!isNaN(Number(text.substring(i2 + 1, i1)))) {
        // java lambda ?
        let i3 = text.lastIndexOf('.', i2 - 1);
        if (i3 > 0) {
          return text.substring(i3 + 1);
        }
      } else {
        return text.substring(i2 + 1);
      }
    }
  }
  return text;
}

function detailsGenerator(ds: any, frame: number, information: any) {
  const text = information.text;
  let i1 = text.lastIndexOf('.');
  if (i1 > 0) {
    let i2 = text.lastIndexOf('.', i1 - 1);
    if (i2 > 0) {
      let p;
      if (!isNaN(Number(text.substring(i2 + 1, i1)))) {
        // java lambda ?
        let i3 = text.lastIndexOf('.', i2 - 1);
        if (i3 > 0) {
          p = text.substring(0, i3);
        }
      } else {
        p = text.substring(0, i2);
      }
      if (p) {
        return { package: p };
      }
    }
  }
  return null;
}

function footTextGenerator(dataSource: any, frame: number, information: any) {
  let sw = information.selfWeight;
  let w = information.weight;
  let tw = information.totalWeight;
  let value = Math.round((w / tw) * 100 * 100) / 100;
  if (w === sw || sw === 0) {
    return value + '% - ' + format(w);
  }
  return value + '% - ' + format(w) + '(' + format(sw) + ')';
}

function hashCodeGenerator(ds: any, frame: number, information: any) {
  let text = information.text;
  if (text.startsWith('java') || text.startsWith('jdk') || text.startsWith('JVM')) {
    return 0;
  }
  let i1 = text.lastIndexOf('.');
  if (i1 !== -1) {
    let i2 = text.lastIndexOf('.', i1 - 1);
    if (i2 === -1) {
      text = text.substring(0, i1);
    }
    text = text.substring(0, i2);
  }

  let hash = 0;
  for (let i = 0; i < text.length; i++) {
    hash = 31 * hash + (text.charCodeAt(i) & 0xff);
    hash &= 0xffffffff;
  }
  return hash;
}

function format(v: number) {
  return toReadableValue(perfDimensions.value[selectedDimensionIndex.value].unit, v);
}

const configuration = ref({
  rootTextGenerator: rootTextGenerator,
  textGenerator: textGenerator,
  titleGenerator: titleGenerator,
  detailsGenerator: detailsGenerator,
  footTextGenerator: footTextGenerator,
  hashCodeGenerator: hashCodeGenerator,
  stackTraceFilter: null,
  showHelpButton: false
});

async function queryGraph() {
  clearFlameGraph();
  await queryFlameGraph(false, []);
  buildFilterValueByThreads();
}

async function queryFlameGraph(include: boolean, taskSet: any) {
  let options = {
    dimension: perfDimensions.value[selectedDimensionIndex.value].key,
    include: include,
    taskSet: taskSet
  };

  flameGraph.value = document.getElementById('flame-graph');
  flameGraph.value.configuration = configuration.value;

  await request('flameGraph', options).then((response) => {
    loading.value = false;
    threadSplit.value = response.threadSplit;
    symbolTable.value = response.symbolTable;
    flameGraphDataSource.value = response.data;
    if (flameGraphDataSource.value.length == 0) {
      hasData.value = false;
    } else {
      hasData.value = true;
    }
    flameGraph.value.dataSource = {
      format: 'line',
      data: response.data
    };
  });
}

function onDimensionIndexChange() {
  toggleFilterValuesChecked.value = true;
  queryGraph();
}

async function onSelectedFilterIndexChange() {
  if (selectedFilterIndex.value !== null) {
    toggleFilterValuesChecked.value = true;
    let filterName =
      perfDimensions.value[selectedDimensionIndex.value].filters[selectedFilterIndex.value].key;
    await queryFlameGraph(false, []);
    if (filterName === 'Thread') {
      buildFilterValueByThreads();
    } else if (filterName === 'Class') {
      buildFilterValueByClass();
    } else if (filterName === 'Method') {
      buildFilterValueByMethod();
    }
  }
}

function buildFilterValueByThreads() {
  let fv = [];
  let map = {};
  let index = 0;
  if (!threadSplit.value) {
    return;
  }

  totalWeight.value = 0;
  for (let key in threadSplit.value) {
    let threadValue = threadSplit.value[key];
    totalWeight.value += threadValue;
    let v = map[key];
    if (v) {
      v.weight += threadValue;
    } else {
      v = {
        key,
        weight: threadValue,
        checked: true
      };
      map[key] = v;
      fv[index++] = v;
    }
  }

  fv.sort((i, j) => {
    return j.weight - i.weight;
  });

  filterValuesMap.value = map;
  filterValueList.value = fv;
  topFilterValueList.value = fv.slice(0, 200);
  filter.value = null;
  flameGraph.value.configuration.stackTraceFilter = filter.value;

  refreshFlameGraph();
}

function buildFilterValueByClass() {
  buildFilterValue((d) => {
    let v = symbolTable.value[d[0][d[0].length - 1]];
    if (v) {
      let index = v.lastIndexOf('.');
      if (index >= 0) {
        return v.substring(0, index);
      }
      return v;
    } else {
      return 'undefined';
    }
  });
}

function buildFilterValueByMethod() {
  buildFilterValue((d) => symbolTable.value[d[0][d[0].length - 1]]);
}

function buildFilterValue(keyExtractor: any) {
  let fv = [];
  let map = {};
  let index = 0;
  let dataSource = flameGraphDataSource.value;
  if (!dataSource) {
    return;
  }
  totalWeight.value = 0;
  for (let i = 0; i < dataSource.length; i++) {
    totalWeight.value += dataSource[i][1];

    let key = keyExtractor(dataSource[i]);
    let v = map[key];
    if (v) {
      v.weight += dataSource[i][1];
    } else {
      v = {
        key,
        weight: dataSource[i][1],
        checked: true
      };
      map[key] = v;
      fv[index++] = v;
    }
  }

  fv.sort((i, j) => {
    return j.weight - i.weight;
  });

  filterValuesMap.value = map;
  filterValueList.value = fv;
  topFilterValueList.value = fv.slice(0, 200);
  filter.value = (d, s) => {
    return filterValuesMap.value[keyExtractor(s)]
      ? filterValuesMap.value[keyExtractor(s)].checked
      : false;
  };

  flameGraph.value.configuration.stackTraceFilter = filter.value;
  refreshFlameGraph();
}

async function handleFilterValuesChecked(checked: boolean, index: number) {
  let filterName =
    perfDimensions.value[selectedDimensionIndex.value].filters[selectedFilterIndex.value].key;
  if (filterName === 'Thread') {
    let taskSet = [];
    let include = !toggleFilterValuesChecked.value;
    for (let v of filterValueList.value) {
      if (toggleFilterValuesChecked.value) {
        if (!v.checked) {
          taskSet.push(v.key);
        }
      } else {
        if (v.checked) {
          taskSet.push(v.key);
        }
      }
    }
    clearFlameGraph();
    if (!toggleFilterValuesChecked.value && taskSet.length === 0) {
      hasData.value = false;
      return;
    }
    await queryFlameGraph(include, taskSet);
  } else {
    if (flameGraph.value.dataSource && flameGraph.value.dataSource.data.length == 0) {
      restoreFlameGraph();
    } else {
      refreshFlameGraph();
    }
    let anyChecked = false;
    for (let v of filterValueList.value) {
      if (v.checked) {
        anyChecked = true;
        break;
      }
    }
    hasData.value = anyChecked;
  }
}

async function handleToggleFilterValuesChecked(checked: boolean) {
  for (let v of filterValueList.value) {
    v.checked = toggleFilterValuesChecked.value;
  }

  let filterName =
    perfDimensions.value[selectedDimensionIndex.value].filters[selectedFilterIndex.value].key;
  if (filterName === 'Thread') {
    clearFlameGraph();
    if (toggleFilterValuesChecked.value) {
      await queryFlameGraph(false, null);
    } else {
      hasData.value = false;
    }
  } else {
    if (toggleFilterValuesChecked.value) {
      if (flameGraph.value.dataSource && flameGraph.value.dataSource.data.length == 0) {
        restoreFlameGraph();
      } else {
        refreshFlameGraph();
      }
      hasData.value = true;
    } else {
      clearFlameGraph();
      hasData.value = false;
    }
  }
}

function restoreFlameGraph() {
  flameGraph.value.dataSource = {
    format: 'line',
    data: flameGraphDataSource.value
  };
}

function refreshFlameGraph() {
  flameGraph.value.dispatchEvent(new CustomEvent('re-render'));
}

function clearFlameGraph() {
  if (flameGraph.value) {
    flameGraph.value.dataSource = flameGraphEmptyData.value;
  }
}

function openFlameGraphModal() {
  flameGraphModalVisible.value = true;
  nextTick(() => {
    let flameGraphModal = window.document.getElementById('flame-graph-in-modal');
    flameGraphModal.configuration = configuration.value;
    flameGraphModal.dataSource = {
      format: 'line',
      data: flameGraphDataSource.value
    };
  });
}

async function queryByTaskName() {
  clearFlameGraph();
  if (!taskName.value) {
    await queryGraph();
    return;
  }
  let taskSet = [];
  let include = true;
  let arr = taskName.value.split(',');
  for (let v of filterValueList.value) {
    v.checked = false;
  }
  for (let name of arr) {
    taskSet.push(name);
    for (let v of filterValueList.value) {
      if (v.key.includes(name)) {
        v.checked = true;
      }
    }
  }

  await queryFlameGraph(include, taskSet);
}

const columns: Column[] = [
  {
    key: 'key',
    dataKey: 'key',
    width: 300,
    style: {
      padding: '0 10px 0 0'
    }
  }
];

onUnmounted(() => {});
</script>

<template>
  <div class="ej-common-view-div">
    <div style="height: 100%; width: 100%; display: flex">
      <div class="ej-profile-container">
        <div style="height: 32px; width: 100%; display: flex; align-items: center">
          <div style="flex-grow: 1; display: flex; align-items: center; overflow: hidden">
            <el-select
              v-model="selectedDimensionIndex"
              placeholder="Select"
              style="width: 210px"
              @change="onDimensionIndexChange"
            >
              <el-option
                v-for="(item, index) in perfDimensions"
                :key="index"
                :label="item.key"
                :value="index"
              />
            </el-select>

            <el-text v-if="totalWeight" truncated style="margin-left: 8px">
              {{ format(totalWeight) + ' ' }}
            </el-text>

            <el-text type="info" v-if="totalWeight" truncated style="margin-left: 8px">
              {{ t('profile.flameGraph.copyMethod') }}
            </el-text>
          </div>

          <el-icon
            style="flex-shrink: 0; cursor: pointer"
            @click="openFlameGraphModal"
            v-if="hasData"
          >
            <FullScreen />
          </el-icon>
        </div>

        <el-empty description="No Data" v-if="!hasData" />

        <div class="ej-profile-main">
          <flame-graph id="flame-graph" downward></flame-graph>
        </div>

        <el-dialog v-model="flameGraphModalVisible" fullscreen>
          <flame-graph id="flame-graph-in-modal" downward></flame-graph>
        </el-dialog>
      </div>

      <div class="ej-profile-inspector">
        <div>
          <el-select
            v-model="selectedFilterIndex"
            placeholder="Select"
            style="width: 100%"
            @change="onSelectedFilterIndexChange"
          >
            <template #prefix>
              <el-icon size="16">
                <Filter />
              </el-icon>
            </template>
            <el-option
              v-for="(item, index) in selectedDimensionIndex !== null
                ? perfDimensions[selectedDimensionIndex].filters
                : []"
              :key="index"
              :label="item.key"
              :value="index"
            />
          </el-select>
        </div>

        <div
          style="margin-top: 8px"
          v-if="
            selectedDimensionIndex != null &&
            perfDimensions[selectedDimensionIndex].filters[selectedFilterIndex].key === 'Thread'
          "
        >
          <el-input
            v-model="taskName"
            :placeholder="t('profile.placeholder.threadName')"
            style="width: 100%"
            @change="queryByTaskName"
          >
            <template #prefix>
              <el-icon :size="16">
                <Search />
              </el-icon>
            </template>
          </el-input>
        </div>

        <div style="margin-top: 8px; flex-grow: 1; overflow: hidden">
          <el-auto-resizer>
            <template #default="{ height, width }">
              <el-table-v2
                :columns="columns"
                :data="topFilterValueList"
                :width="width"
                :height="height"
                :header-height="30"
                :row-height="65"
              >
                <template #header>
                  <div
                    style="width: 100%; display: flex; justify-content: end; align-items: center"
                  >
                    <el-text truncated> {{ checkedCount }}/{{ filterValueList.length }}</el-text>
                    <el-checkbox
                      v-model="toggleFilterValuesChecked"
                      @change="(checked) => handleToggleFilterValuesChecked(checked)"
                      style="margin-left: 8px; margin-right: 4px; height: 16px"
                    />
                  </div>
                </template>
                <template #cell="{ rowData: data, rowIndex: index }">
                  <div
                    style="
                      width: 100%;
                      height: 100%;
                      display: flex;
                      justify-content: space-between;
                      align-items: center;
                    "
                  >
                    <div
                      style="
                        height: 100%;
                        flex-grow: 1;
                        padding: 5px;
                        overflow: hidden;
                        display: flex;
                        flex-direction: column;
                        justify-content: space-between;
                      "
                    >
                      <div>
                        <el-text truncated>
                          {{ data.key }}
                        </el-text>
                      </div>
                      <div>
                        <el-text tag="b" size="small" truncated>
                          {{ format(filterValueList[index].weight) }}
                        </el-text>
                      </div>
                      <div style="width: 100%">
                        <el-progress
                          :percentage="
                            Math.round((filterValueList[index].weight / totalWeight) * 100)
                          "
                          :color="filterValueList[index].checked ? '#ff8200' : 'grey'"
                        />
                      </div>
                    </div>

                    <el-checkbox
                      v-model="filterValueList[index].checked"
                      @change="(checked) => handleFilterValuesChecked(checked, index)"
                    />
                  </div>
                </template>
              </el-table-v2>
            </template>
          </el-auto-resizer>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ej-profile-container {
  height: 100%;
  display: flex;
  flex-grow: 1;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
}

.ej-profile-main {
  height: 100%;
  flex-grow: 1;
  background-color: var(--el-bg-color);
  border-radius: var(--ej-common-border-radius);
  padding: 5px 0;
  overflow: hidden;
  margin-top: 5px;
}

.ej-profile-inspector {
  height: 100%;
  width: 300px;
  margin-left: 15px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
}
</style>
