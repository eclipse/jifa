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
import { Phase, useAnalysisStore } from '@/stores/analysis';
import { InfoFilled } from '@element-plus/icons-vue';
import { t } from '@/i18n/i18n';

import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';

const { request } = useAnalysisApiRequester();
const analysis = useAnalysisStore();

const emit = defineEmits(['confirmAnalysisOptions']);

const options = reactive({
  keep_unreachable_objects: true,
  strictness: 'stop',
  discard_objects: false,
  discard_pattern: '',
  discard_ratio: 0,
  additional_options: ''
});

function onConfirm() {
  request('clean').then(() => {
    analysis.leaveGuard = false;
    analysis.setPhase(Phase.INIT);
    analysis.setShowSetupPage(false);
    emit('confirmAnalysisOptions', options);
  });
}

function onCancel() {
  analysis.setShowSetupPage(false);
}

const enableDiscard = computed(() => options.discard_objects);
</script>
<template>
  <el-form
    :model="options"
    label-position="right"
    label-width="200px"
    style="width: 600px"
    size="large"
  >
    <el-form-item :label="hdt('option.labelOfKeepUnreachableObjects')">
      <el-switch v-model="options.keep_unreachable_objects"></el-switch>
      <el-popover
        placement="top"
        :width="600"
        trigger="hover"
        :show-arrow="false"
        :popper-style="{ padding: 0 }"
      >
        <template #reference>
          <el-icon class="ej-icon" style="margin-left: 8px" size="18">
            <InfoFilled />
          </el-icon>
        </template>
        <template #default>
          <el-alert
            type="info"
            style="word-break: keep-all"
            :closable="false"
            :description="hdt('option.descOfKeepUnreachableObjects')"
          >
          </el-alert>
        </template>
      </el-popover>
    </el-form-item>

    <el-form-item :label="hdt('option.labelOfStrictness')" slot="reference">
      <el-radio-group v-model="options.strictness">
        <el-radio-button label="stop">stop</el-radio-button>
        <el-radio-button label="warn">warn</el-radio-button>
        <el-radio-button label="permissive">permissive</el-radio-button>
      </el-radio-group>
      <el-popover
        placement="top"
        :width="600"
        trigger="hover"
        :show-arrow="false"
        :popper-style="{ padding: 0 }"
      >
        <template #reference>
          <el-icon class="ej-icon" style="margin-left: 8px" size="18">
            <InfoFilled />
          </el-icon>
        </template>
        <template #default>
          <el-alert
            type="info"
            style="word-break: keep-all"
            :closable="false"
            :title="hdt('option.descOfStrictness')"
          >
            <div slot="default">
              <p>stop: {{ hdt('option.descOfStopStrictness') }}</p>
              <p>warn: {{ hdt('option.descOfWarnStrictness') }}</p>
              <p>permissive: {{ hdt('option.descOfPermissiveStrictness') }}</p>
            </div>
          </el-alert>
        </template>
      </el-popover>
    </el-form-item>

    <!-- Discard Objects -->
    <el-form-item :label="hdt('option.labelOfDiscardObjects')">
      <el-switch v-model="options.discard_objects"></el-switch>
      <el-popover
        placement="top"
        :width="600"
        trigger="hover"
        :show-arrow="false"
        :popper-style="{ padding: 0 }"
      >
        <template #reference>
          <el-icon class="ej-icon" style="margin-left: 8px" size="18">
            <InfoFilled />
          </el-icon>
        </template>
        <template #default>
          <el-alert
            type="info"
            style="word-break: keep-all"
            :closable="false"
            :title="hdt('option.descOfDiscardObjects')"
          >
            <div slot="default">
              <p>{{ hdt('option.descOfDiscardObjectsDetail') }}</p>
              <p>discard_pattern: {{ hdt('option.descOfDiscardObjectsPattern') }}</p>
              <p>discard_ratio: {{ hdt('option.descOfDiscardObjectsRatio') }}</p>
            </div>
          </el-alert>
        </template>
      </el-popover>
    </el-form-item>
    <el-form-item :label="hdt('option.labelOfDiscardObjectsPattern')" v-if="enableDiscard">
      <el-input
        v-model="options.discard_pattern"
        placeholder="eg:byte\[\]|java\.lang\.String||java\.lang\.String\[\]"
        clearable
        class="discard-pattern-form-input"
        @keydown.enter="(e) => e.preventDefault()"
      >
      </el-input>
    </el-form-item>
    <el-form-item :label="hdt('option.labelOfDiscardObjectsRatio')" v-if="enableDiscard">
      <el-input
        v-model="options.discard_ratio"
        placeholder="0~100"
        class="discard-ratio-form-input"
        @keydown.enter="(e) => e.preventDefault()"
      >
      </el-input>
    </el-form-item>
    <!-- Discard Objects end -->
    <el-form-item :label="hdt('option.labelAdditionalAnalyseOptions')">
      <el-input
        v-model="options.additional_options"
        placeholder="options for eclipse memory analyser, eg: k1=v1 k2=v2 k3='v3 with blank characters'"
        class="additional-options-form-input"
        clearable
        @keydown.enter="(e) => e.preventDefault()"
      >
      </el-input>
      <el-popover
        placement="top"
        :width="600"
        trigger="hover"
        :show-arrow="false"
        :popper-style="{ padding: 0 }"
      >
        <template #reference>
          <el-icon class="ej-icon" style="margin-left: 8px" size="18">
            <InfoFilled />
          </el-icon>
        </template>
        <template #default>
          <el-alert
            type="info"
            style="word-break: keep-all"
            :closable="false"
            :description="hdt('option.descAdditionalAnalyseOptions')"
          >
          </el-alert>
        </template>
      </el-popover>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="onConfirm">{{ t('common.confirm') }}</el-button>
      <el-button
        @click="onCancel"
        v-if="analysis.phase !== Phase.INIT && analysis.phase !== Phase.SETUP"
      >
        {{ t('common.cancel') }}
      </el-button>
    </el-form-item>
  </el-form>
</template>
<style scoped>
.discard-ratio-form-input {
  width: 120px;
}
.discard-pattern-form-input {
  width: 420px;
}

.additional-options-form-input {
  width: 350px;
}
</style>
