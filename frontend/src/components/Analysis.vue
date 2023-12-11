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
import { onBeforeRouteLeave, useRoute } from 'vue-router';
import { Phase, useAnalysisStore } from '@/stores/analysis';
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';
import { ElMessageBox, ElNotification } from 'element-plus';
import type { FileType } from '@/composables/file-types';
import { t } from '@/i18n/i18n';
import axios from 'axios';
import { useHeaderToolbar } from '@/composables/header-toolbar';

const props = defineProps<{
  target: string;
}>();

const route = useRoute();
const analysis = useAnalysisStore();
analysis.setTarget(route.meta.fileType as FileType, props.target);

const toolBarComponent = toRaw(analysis.fileType?.toolBarComponent);
const setupComponent = toRaw(analysis.fileType?.setupComponent);
const analysisComponent = toRaw(analysis.fileType?.analysisComponent);

const { request } = useAnalysisApiRequester();

const progress = ref(0);
const log = ref('');
const analysisLogDiv = ref(null);

const progressStatus = computed(() => {
  if (analysis.phase === Phase.FAILURE) {
    return { status: 'exception' };
  }
  if (progress.value === 100) {
    return { status: 'success' };
  }
  return {};
});

function analyze(options?) {
  let parameters;
  if (options) {
    options = { ...options };
    let additionalOptions = options.additional_options;
    delete options.additional_options;
    if (additionalOptions && additionalOptions.trim()) {
      const pairs = additionalOptions.match(/(\w+)=('(?:\\.|[^'\\])*'|\S+)/g);

      for (let pair of pairs) {
        const index = pair.indexOf('=');
        const key = pair.slice(0, index);
        const value = pair.slice(index + 1);
        // delete qouta on value if exists.
        const cleanedValue = value.replace(/^'(.*)'$/, '$1');
        options[key] = cleanedValue
      }
    }
    parameters = { options };
  }
  request('analyze', parameters)
    .then(() => {
      analysis.setPhase(Phase.ANALYZING);
      nextTick(() => pollProgress());
    })
    .catch(handleError);
}

function pollProgress() {
  request('progressOfAnalysis')
    .then(async (data) => {
      let percent = data.percent;
      if (data.message) {
        log.value = data.message;
      }

      await nextTick();

      let state = data.state;
      if (state === 'IN_PROGRESS') {
        if (percent >= 1) {
          progress.value = 99;
        } else {
          progress.value = Math.floor(percent * 100);
        }
        setTimeout(pollProgress, 1000);
      } else if (state === 'SUCCESS') {
        progress.value = 100;
        setTimeout(
          () =>
            ElNotification({
              title: t('analysis.success'),
              type: 'success',
              offset: 200,
              duration: 500,
              showClose: false,
              onClose: () => analysis.setPhase(Phase.SUCCESS)
            }),
          500
        );
      } else {
        handleError();
      }
    })
    .catch(handleError);
}

function handleError(error?) {
  if (error) {
    log.value = error;
  }
  analysis.setPhase(Phase.FAILURE);
}

onBeforeRouteLeave((to, from, next) => {
  if (!analysis.leaveGuard) {
    next();
    analysis.leaveGuard = true;
    return;
  }
  ElMessageBox.confirm(t('common.messageBeforeLeave'))
    .then(() => next())
    .catch(() => next(false));
});

async function beforeWindowUnload(e) {
  if (analysis.leaveGuard) {
    e.preventDefault();
    e.returnValue = t('common.messageBeforeLeave');
  }
}

onMounted(() => {
  useHeaderToolbar().set(toolBarComponent);
  window.addEventListener('beforeunload', beforeWindowUnload);

  analysis.setPhase(Phase.INIT);
  axios
    .get(`/jifa-api/files/${props.target}`)
    .then((resp) => {
      let id = resp.data.id as number;
      let filename = resp.data.originalName as string;
      analysis.setIdAndFilename(id, filename);
      if (filename.length > 12) {
        document.title = `${filename.substring(0, 12)} ... · Eclipse Jifa`;
      } else {
        document.title = `${filename} · Eclipse Jifa`;
      }
      return request('needOptionsForAnalysis');
    })
    .then((need) => {
      if (need) {
        analysis.setPhase(Phase.SETUP);
      } else {
        analyze();
      }
    })
    .catch((e) => handleError(e.response?.data?.message ? e.response.data.message : e));
});

onUnmounted(() => {
  useHeaderToolbar().reset();
  analysis.setPhase(null);
  window.removeEventListener('beforeunload', beforeWindowUnload);
  document.title = 'Eclipse Jifa';
});
</script>
<template>
  <transition mode="out-in">
    <div class="ej-common-view-div" v-if="analysis.phase == Phase.INIT" v-loading="true"></div>
    <div
      class="ej-common-view-div"
      style="display: flex; flex-direction: column; justify-content: center; align-items: center"
      v-else-if="analysis.phase === Phase.SETUP || analysis.showSetupPage === true"
    >
      <component :is="setupComponent" @confirmAnalysisOptions="analyze"></component>
    </div>
    <div
      class="ej-common-view-div"
      style="display: flex; flex-direction: column; justify-content: start"
      v-else-if="analysis.phase == Phase.ANALYZING || analysis.phase == Phase.FAILURE"
    >
      <el-progress
        :stroke-width="35"
        text-inside
        striped
        striped-flow
        :duration="64"
        style="margin-bottom: 15px"
        :percentage="progress"
        v-bind="progressStatus"
      />
      <div
        ref="analysisLogDiv"
        :class="[
          analysis.phase == Phase.FAILURE ? 'ej-analysis-error-log-div' : 'ej-analysis-log-div'
        ]"
      >
        <p style="font-weight: bold">{{ t('analysis.log') }}</p>
        <p v-if="log" style="white-space: pre-line">{{ log }}</p>
      </div>
    </div>
    <component :is="analysisComponent" v-else-if="analysis.phase == Phase.SUCCESS"></component>
  </transition>
</template>

<style scoped>
.ej-analysis-log-div,
.ej-analysis-error-log-div {
  overflow: auto;
  border-radius: 6px 6px 8px 8px;
  margin: 0 3px;
  padding: 7px;
  line-height: 1.5;
}

.ej-analysis-log-div {
  border-top: 8px solid var(--el-color-primary);
  background-color: rgba(var(--el-color-primary-rgb), 0.1);
}

.ej-analysis-error-log-div {
  border-top: 8px solid var(--el-color-error);
  background-color: rgba(var(--el-color-error-rgb), 0.1);
}

p {
  word-break: break-all;
  margin: 0;
}
</style>
