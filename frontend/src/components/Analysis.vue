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
import { useRoute } from 'vue-router';
import { useAnalysisStore } from '@/stores/analysis';
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';
import { ElNotification } from 'element-plus';
import { onBeforeRouteLeave } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import type { FileType } from '@/composables/file-types';
import { t } from '@/i18n/i18n';
import axios from 'axios';

const props = defineProps<{
  target: string;
}>();

const route = useRoute();
const analysis = useAnalysisStore();
analysis.setTarget(route.meta.fileType as FileType, props.target);

const setupView = toRaw(analysis.fileType?.setupComponent);
const analysisView = toRaw(analysis.fileType?.analysisComponent);

const { request } = useAnalysisApiRequester();

enum Phase {
  INIT,
  SETUP,
  ANALYZING,
  SUCCESS,
  FAILURE
}

const phase = ref(Phase.INIT);
const progress = ref(0);
const log = ref('');
const analysisLogDiv = ref(null);

const progressStatus = computed(() => {
  if (phase.value === Phase.FAILURE) {
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
    parameters = { options };
  }
  request('analyze', parameters)
    .then(() => {
      phase.value = Phase.ANALYZING;
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
              onClose: () => (phase.value = Phase.SUCCESS)
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
  phase.value = Phase.FAILURE;
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
  window.addEventListener('beforeunload', beforeWindowUnload);

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
        phase.value = Phase.SETUP;
      } else {
        analyze();
      }
    })
    .catch(e => handleError(e.response?.data?.message ? e.response.data.message : e));
});

onUnmounted(() => {
  document.title = 'Eclipse Jifa';
  window.removeEventListener('beforeunload', beforeWindowUnload);
});
</script>
<template>
  <transition mode="out-in">
    <div class="ej-common-view-div" v-if="phase == Phase.INIT" v-loading="true"></div>
    <div
      class="ej-common-view-div"
      style="display: flex; flex-direction: column; justify-content: start"
      v-else-if="phase == Phase.ANALYZING || phase == Phase.FAILURE"
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
        :class="[phase == Phase.FAILURE ? 'ej-analysis-error-log-div' : 'ej-analysis-log-div']"
      >
        <p style="font-weight: bold">{{ t('analysis.log') }}</p>
        <p v-if="log" style="white-space: pre-line">{{ log }}</p>
      </div>
    </div>
    <div
      class="ej-common-view-div"
      style="display: flex; flex-direction: column; justify-content: center; align-items: center"
      v-else-if="phase === Phase.SETUP"
    >
      <component :is="setupView" @confirmAnalysisOptions="analyze"></component>
    </div>
    <component :is="analysisView" v-else-if="phase == Phase.SUCCESS"></component>
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
