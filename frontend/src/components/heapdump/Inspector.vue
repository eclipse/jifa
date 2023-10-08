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
import { hdt } from '@/components/heapdump/utils';
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';
import { useSelectedObject } from '@/composables/heapdump/selected-object';
import { Search } from '@element-plus/icons-vue';
import { getIcon, ICONS } from './icon-helper';
import { OBJECT_TYPE } from './type';
import { prettySize } from '@/support/utils';
import Fields from '@/components/heapdump/Fields.vue';
import { Place } from '@element-plus/icons-vue';
import { emit, EventType } from '@/components/heapdump/event-bus';

const { request } = useAnalysisApiRequester();

const { selectedObjectId } = useSelectedObject();

const address = ref('');
const infoItems = ref(null);
const value = ref(null);

const loading1 = ref(false);
const loading2 = ref(false);

watch(selectedObjectId, () => {
  load();
});

function search() {
  let v = address.value;
  if (v.startsWith('0x') || v.startsWith('0X')) {
    v = v.substring(2);
  }
  let i = parseInt(v, 16);
  if (Number.isNaN(i)) {
    return;
  }
  request('inspector.addressToId', {
    address: i
  }).then((id) => {
    selectedObjectId.value = id;
    emit(EventType.OBJECT_OUTBOUNDS, { objectId: id, label: '0x' + i.toString(16) });
  });
}

function load() {
  if (selectedObjectId.value >= 0) {
    loading1.value = true;
    let params = { objectId: selectedObjectId.value };
    request('inspector.objectView', params).then((obj) => {
      let oa = '0x' + obj.objectAddress.toString(16);

      let tmp = [];
      tmp.push({ data: oa, icon: shallowRef(Place), copyable: true });
      let name = obj.name;
      let p = '';
      let dotPos = name.lastIndexOf('.');
      if (dotPos >= 0) {
        p = name.substring(0, dotPos);
        name = name.substring(dotPos + 1);
      }
      tmp.push({ data: name, icon: getIcon(obj.gCRoot, obj.objectType) });
      tmp.push({ data: p || '(default)', icon: ICONS.objects.package });
      tmp.push({ data: obj.classLabel, icon: getIcon(obj.classGCRoot, OBJECT_TYPE.CLASS) });
      tmp.push({
        data: obj.superClassName ? obj.superClassName : ' ',
        icon: ICONS.objects.superclass
      });
      tmp.push({
        data: obj.classLoaderLabel,
        icon: getIcon(obj.classLoaderGCRoot, OBJECT_TYPE.CLASSLOADER)
      });
      tmp.push({ data: prettySize(obj.shallowSize) + ' (shallow size)', icon: ICONS.size });
      tmp.push({ data: prettySize(obj.retainedSize) + ' (retained size)', icon: ICONS.size });
      tmp.push({ data: obj.gcRootInfo, icon: ICONS.decorations.gc_root });

      infoItems.value = tmp;
      // activeTab.value = OBJECT_TYPE.CLASS === obj.objectType ? 'Statics' : 'Attributes'
      loading1.value = false;
    });

    loading2.value = true;
    request('inspector.value', params).then((v) => {
      value.value = [{ value: v }];
      loading2.value = false;
    });
  } else {
    infoItems.value = [];
    value.value = null;
  }
}

const activeTab = ref('Attributes');

onMounted(() => load());
</script>
<template>
  <div class="ej-inspector-div" style="height: 394px; flex-shrink: 0">
    <el-tabs class="ej-tab" model-value="Inspector">
      <el-tab-pane name="Inspector" :label="hdt('tab.inspector')">
        <div style="height: 100%; display: flex; flex-direction: column">
          <div style="height: 32px; display: flex; align-items: center; flex-shrink: 0">
            <el-input style="width: 100%" size="small" v-model="address" @change="search" clearable>
              <template #append>
                <el-button :icon="Search" size="small" @click="search" />
              </template>
            </el-input>
          </div>
          <el-table
            style="height: 100%"
            empty-text=" "
            :data="infoItems"
            size="small"
            stripe
            :show-overflow-tooltip="{ showArrow: false }"
            v-loading="loading1"
            v-if="selectedObjectId >= 0"
          >
            <el-table-column>
              <template #default="{ row }">
                <div style="display: flex; align-items: center">
                  <el-icon v-if="typeof row.icon === 'object'" style="margin-right: 5px" size="16">
                    <component :is="row.icon" />
                  </el-icon>
                  <img v-else :src="row.icon" alt="" style="margin-right: 5px" />
                  <div class="ej-table-content-div">
                    {{ row.data }}
                  </div>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
  <div class="ej-inspector-div" style="flex-grow: 1; margin-top: 7px; overflow: hidden">
    <el-tabs class="ej-tab" v-model="activeTab">
      <el-tab-pane name="Statics" :label="hdt('tab.statics')">
        <Fields static />
      </el-tab-pane>
      <el-tab-pane name="Attributes" :label="hdt('tab.attributes')">
        <Fields />
      </el-tab-pane>
      <el-tab-pane name="Value" :label="hdt('tab.value')">
        <el-table
          size="small"
          :data="value"
          v-if="selectedObjectId >= 0 && value && value[0].value"
          style="height: 100%"
          highlight-current-row
          :header-cell-style="{
            background: 'var(--el-fill-color-light)',
            color: 'var(--el-text-color-primary)'
          }"
        >
          <el-table-column :label="hdt('tab.value')">
            <div style="white-space: pre-wrap">
              {{ value[0].value }}
            </div>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.ej-inspector-div {
  background-color: var(--el-bg-color);
  border-radius: var(--ej-common-border-radius);
  padding: 5px 8px;
}

.ej-table-content-div {
  overflow-x: auto;
}

.ej-table-content-div::-webkit-scrollbar {
  display: none;
}
</style>
