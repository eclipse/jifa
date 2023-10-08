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
import { EventType, listen } from '@/components/heapdump/event-bus';
import { t } from '@/i18n/i18n';
import References from '@/components/heapdump/References.vue';
import { hdt } from '@/components/heapdump/utils';
import ClassReferences from '@/components/heapdump/ClassReferences.vue';
import PathToGCRoots from '@/components/heapdump/PathToGCRoots.vue';
import MergedPathToGCRoots from '@/components/heapdump/MergedPathToGCRoots.vue';

let tabId = 1;

const tabs = ref([]);
const empty = computed(() => tabs.value.length == 0);
defineExpose({ empty });

const activeTabName = ref();

function handleRemove(name) {
  const allTabs = tabs.value;
  let activeName = activeTabName.value;
  if (activeName === name) {
    allTabs.forEach((tab, index) => {
      if (tab.name === name) {
        const nextTab = allTabs[index + 1] || allTabs[index - 1];
        if (nextTab) {
          activeName = nextTab.name;
        }
      }
    });
  }
  activeTabName.value = activeName;
  tabs.value = allTabs.filter((tab) => tab.name !== name);
}

function title(key, label) {
  return `${hdt('dynamicTab.' + key)}「${label}」`;
}

function createTab(prefixKey, label, component, data) {
  let id = tabId++;
  let name = `${id}`;
  tabs.value.push({
    id,
    name,
    prefixKey,
    label,
    component: shallowRef(component),
    data
  });
  activeTabName.value = name;
}

listen(EventType.OBJECT_OUTBOUNDS, (payload) => {
  const { label, className, objectId, __byHistogram: byHistogram } = payload;
  createTab('objectOutbounds', label || className, References, { objectId, byHistogram });
});

listen(EventType.OBJECT_INBOUNDS, (payload) => {
  const { label, className, objectId, __byHistogram: byHistogram } = payload;
  createTab('objectInbounds', label || className, References, {
    objectId,
    outbounds: false,
    byHistogram
  });
});

listen(EventType.CLASS_OUTBOUNDS, ({ objectId, label, className }) => {
  createTab('classOutbounds', label || className, ClassReferences, { objectId });
});

listen(EventType.CLASS_INBOUNDS, ({ objectId, label, className }) => {
  createTab('classInbounds', label || className, ClassReferences, { objectId, outbounds: false });
});

listen(EventType.PATH_TO_GC_ROOTS, ({ objectId, label, className }) => {
  createTab('pathToGCRoots', label || className, PathToGCRoots, { objectId });
});

listen(EventType.MERGED_PATH_TO_GC_ROOTS, (payload) => {
  const { label, className, objectId, __byHistogram: byHistogram } = payload;
  let objectIdsOrClassId = byHistogram ? objectId : [objectId];
  createTab('mergedPathToGCRoots', label || className, MergedPathToGCRoots, {
    objectIdsOrClassId,
    byHistogram
  });
});
</script>
<template>
  <el-tabs
    class="ej-tab"
    v-model="activeTabName"
    type="card"
    closable
    @tab-remove="handleRemove"
    v-if="tabs.length"
  >
    <el-tab-pane
      v-for="tab in tabs"
      :key="tab.id"
      :name="tab.name"
      :label="title(tab.prefixKey, tab.label)"
    >
      <component :is="tab.component" v-bind="tab.data"></component>
    </el-tab-pane>
  </el-tabs>
  <div
    style="height: 100%; display: flex; justify-content: center; align-items: center; width: 100%"
    v-else
  >
    <el-empty :image-size="200" :description="t('common.noData')" />
  </div>
</template>
