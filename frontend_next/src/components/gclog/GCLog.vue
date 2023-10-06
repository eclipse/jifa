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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';
import Container from '@/components/gclog/Container.vue';
import { gct } from '@/i18n/i18n';
import { useGCLogData } from '@/stores/gc-log-data';
import VMOptions from '@/components/gclog/VMOptions.vue';
import Overview from '@/components/gclog/Overview.vue';
import MemoryStats from '@/components/gclog/MemoryStats.vue';
import TimeGraph from '@/components/gclog/TimeGraph.vue';
import ObjectStats from '@/components/gclog/ObjectStats.vue';
import Details from '@/components/gclog/Details.vue';
import PhaseStats from '@/components/gclog/PhaseStats.vue';
import Diagnosis from '@/components/gclog/Diagnosis.vue';
import Pause from '@/components/gclog/Pause.vue';
import { useHeaderToolbar } from '@/composables/header-toolbar';
import Toolbar from '@/components/gclog/Toolbar.vue';
import { storeToRefs } from 'pinia';
import { useRoute } from 'vue-router';
import Comparison from '@/components/gclog/Comparison.vue';
import AnalysisConfig from '@/components/gclog/AnalysisConfig.vue';

const { request } = useAnalysisApiRequester();
const GCLogData = useGCLogData();

const loading = ref(true);

const { showDetails } = storeToRefs(GCLogData);
const detailsLazySwitch = ref(false);

watch(showDetails, (v) => {
  if (v && !detailsLazySwitch.value) {
    detailsLazySwitch.value = true;
  }
});

interface View {
  id: string;
  title: string;
  component: any;
  useHeaderBar?: boolean;
}

function view(id: string, title: string, component, useHeaderBar?): View {
  return { id, title, component, useHeaderBar };
}

const basicInfo = view('basic-information', 'basicInfo', Overview);
const diagnose = view('diagnose', 'diagnose.diagnose', Diagnosis);
const timeGraph = view('time-graph', 'timeGraph.timeGraph', TimeGraph, true);
const pauseInfo = view('pause-information', 'pauseInfo.pauseInfo', Pause);
const memoryStats = view('memory-statistics', 'memoryStats.memoryStats', MemoryStats);
const phaseStatsAndCause = view(
  'phase-statistics-and-cause',
  'phaseStats.phaseStatsAndCause',
  PhaseStats,
  true
);
const objectStats = view('object-stats', 'objectStats', ObjectStats);
const vmOptions = view('vm-options', 'vmOptions.vmOptions', VMOptions, true);

const views: View[] = [
  basicInfo,
  diagnose,
  timeGraph,
  pauseInfo,
  memoryStats,
  phaseStatsAndCause,
  objectStats,
  vmOptions
];

const markIndex = ref(-1);
const markTop = computed(() => `calc((40px - 20px) / 2 + ${Math.max(markIndex.value, 0) * 40}px)`);
const markOpacity = computed(() => (markIndex.value >= 0 ? 1 : 0));

const scroll = ref();

let handleScrollDisabled = false;

function handleScroll({ scrollTop: top }) {
  if (handleScrollDisabled) {
    handleScrollDisabled = false;
    return;
  }
  if (top == 0) {
    markIndex.value = -1;
    return;
  }
  let viewHeight = scroll.value.wrapRef.offsetHeight;
  if (Math.abs(scroll.value.wrapRef.children[0].offsetHeight - top - viewHeight) < 1) {
    markIndex.value = views.length - 1;
    return;
  }
  let previewVisibleHeight;
  for (let i = 0; i < views.length; i++) {
    let e = document.getElementById(views[i].id);
    if (e.offsetTop + e.offsetHeight <= top) {
      continue;
    }
    if (e.offsetTop >= top + viewHeight) {
      markIndex.value = i - 1;
      break;
    }
    let visibleHeight = e.offsetHeight;
    visibleHeight -= Math.max(top - e.offsetTop, 0);
    visibleHeight -= Math.max(e.offsetTop + e.offsetHeight - (top + viewHeight), 0);
    if (previewVisibleHeight) {
      markIndex.value = visibleHeight >= previewVisibleHeight ? i : i - 1;
      break;
    } else {
      if (visibleHeight / e.offsetHeight >= 0.75) {
        markIndex.value = i;
        break;
      }
      previewVisibleHeight = visibleHeight;
    }
  }
}

function handleClick(index) {
  handleScrollDisabled = true;
  markIndex.value = index;
  scroll.value.setScrollTop(document.getElementById(views[index].id).offsetTop);
}

const route = useRoute();
const showComparisonView = route.query.hasOwnProperty('compareTo');

onMounted(() => {
  request('metadata').then((metadata) => {
    let timeRange;
    if (route.query.start && route.query.end) {
      let start = parseInt(route.query.start as string);
      let end = parseInt(route.query.end as string);
      if (start < end) {
        timeRange = {
          start,
          end
        };
      }
    }
    GCLogData.init(metadata, timeRange);
    loading.value = false;
  });
  if (!showComparisonView) {
    useHeaderToolbar().set(Toolbar);
  }
});

onUnmounted(() => {
  if (GCLogData.showDetails) {
    GCLogData.toggleDetails();
  }
  useHeaderToolbar().reset();
});
</script>
<template>
  <div class="ej-common-view-div" style="padding: 0" v-loading="loading">
    <template v-if="!loading && !showComparisonView">
      <AnalysisConfig v-if="GCLogData.analysisConfigVisible" />

      <el-scrollbar ref="scroll" @scroll="handleScroll" v-show="!showDetails">
        <div style="display: flex; justify-content: space-between">
          <div style="flex: 1 0"></div>

          <div style="width: 1200px; overflow: hidden">
            <Container
              v-for="item in views"
              :header="gct(item.title)"
              :id="item.id"
              :header-bar-id="`${item.id}-header-bar`"
            >
              <component
                :is="item.component"
                v-bind="item.useHeaderBar ? { headerBarId: `${item.id}-header-bar` } : {}"
              />
            </Container>
          </div>

          <div class="view-container">
            <div class="view">
              <div class="mark" />
              <ul>
                <li
                  v-for="(item, index) in views"
                  :class="{ 'view-item': true, active: markIndex === index }"
                  @click="handleClick(index)"
                >
                  {{ gct(item.title) }}
                </li>
              </ul>
            </div>
          </div>
        </div>
      </el-scrollbar>

      <div style="flex-grow: 1; height: 100%; padding: 18px; overflow: hidden" v-show="showDetails">
        <Details v-if="detailsLazySwitch" />
      </div>
    </template>

    <Comparison v-if="!loading && showComparisonView" />
  </div>
</template>
<style scoped>
.view-container {
  flex: 1 0;
  position: relative;
}

.view {
  position: sticky;
  top: 18px;
  display: none;
}

@media (min-width: 800px) {
  .view-container {
    flex: 1.618 0;
  }

  .view {
    display: block;
  }
}

ul {
  padding: 0 15px;
}

li {
  list-style: none;
  height: 40px;
  display: flex;
  align-items: center;
  padding-left: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 400;
}

.view-item {
  white-space: nowrap;
  color: var(--el-text-color-secondary);
  transition: color 0.2s;
}

.view-item:hover {
  color: var(--el-text-color-primary);
}

.active {
  color: var(--el-text-color-primary);
  font-weight: 500;
}

.mark {
  position: absolute;
  left: 12px;
  top: v-bind(markTop);
  opacity: v-bind(markOpacity);
  width: 4px;
  border-radius: 3px;
  height: 20px;
  background-color: var(--el-color-primary);
  transition:
    top 0.25s,
    opacity 0.25s;
}
</style>
