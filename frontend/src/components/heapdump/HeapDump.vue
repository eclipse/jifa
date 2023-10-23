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
import Inspector from '@/components/heapdump/Inspector.vue';
import Overview from '@/components/heapdump/Overview.vue';
import DominatorTree from '@/components/heapdump/DominatorTree.vue';
import Histogram from '@/components/heapdump/Histogram.vue';
import Threads from '@/components/heapdump/Threads.vue';
import SystemProperties from '@/components/heapdump/SystemProperties.vue';
import Query from '@/components/heapdump/Query.vue';
import GCRoots from '@/components/heapdump/GCRoots.vue';
import UnreachableObjects from '@/components/heapdump/UnreachableObjects.vue';
import ClassLoaders from '@/components/heapdump/ClassLoaders.vue';
import DirectByteBuffers from '@/components/heapdump/DirectByteBuffers.vue';
import DuplicateClasses from '@/components/heapdump/DuplicateClasses.vue';
import LeakSuspects from '@/components/heapdump/LeakSuspects.vue';
import DynamicTabs from '@/components/heapdump/DynamicTabs.vue';
import { MoreFilled } from '@element-plus/icons-vue';
import { listenAll } from '@/components/heapdump/event-bus';
import { makeFirstLetterLowercase } from '@/support/utils';
import { useDebouncedRef } from '@/composables/debounced-ref';
import { useSelectedObject } from '@/composables/heapdump/selected-object';

const activeTab = ref('Overview');
const lastActiveTab = ref();

const nameOfDynamicTabs = 'DynamicTabs';

listenAll(() => {
  if (activeTab.value !== nameOfDynamicTabs) {
    lastActiveTab.value = activeTab.value;
    activeTab.value = nameOfDynamicTabs;
  }
});

const tabs = {
  Overview,
  LeakSuspects,
  DominatorTree,
  Histogram,
  Threads,
  ClassLoaders,
  Query,
  GCRoots,
  DirectByteBuffers,
  DuplicateClasses,
  UnreachableObjects,
  SystemProperties
};

const width = useDebouncedRef(window.innerWidth);
const height = useDebouncedRef(window.innerHeight);

const inspectorVisible = computed(() => width.value >= 900 && height.value >= 580);

function handleResize() {
  width.value = window.innerWidth;
  height.value = window.innerHeight;
}

onMounted(() => {
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  useSelectedObject().reset();
});

const dynamicTabs = ref();
watchEffect(() => {
  let dt = dynamicTabs.value;
  if (dt && dt.empty && activeTab.value === nameOfDynamicTabs) {
    activeTab.value = lastActiveTab.value;
  }
});
</script>
<template>
  <div class="ej-heap-dump-container">
    <div class="ej-heap-dump-main">
      <el-tabs class="ej-tab" v-model="activeTab">
        <el-tab-pane
          v-for="(tab, key) in tabs"
          :key="key"
          :name="key"
          :label="hdt(`tab.${makeFirstLetterLowercase(key)}`)"
          lazy
        >
          <component :is="tab" />
        </el-tab-pane>

        <!-- Shouldn't make DynamicTabs lazy -->
        <el-tab-pane :name="nameOfDynamicTabs">
          <template #label>
            <el-icon>
              <MoreFilled />
            </el-icon>
          </template>
          <DynamicTabs ref="dynamicTabs" />
        </el-tab-pane>
      </el-tabs>
    </div>
    <div class="ej-heap-dump-inspector" v-if="inspectorVisible">
      <Inspector />
    </div>
  </div>
</template>

<style scoped>
.ej-heap-dump-container {
  height: 100%;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
}

.ej-heap-dump-main {
  height: 100%;
  flex-grow: 1;
  background-color: var(--el-bg-color);
  border-radius: var(--ej-common-border-radius);
  padding: 5px 8px;
  overflow: hidden;
}

.ej-heap-dump-inspector {
  height: 100%;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  width: 400px;
  margin-left: 7px;
}
</style>
