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
import { reactive } from 'vue';
import { hdt } from '@/components/heapdump/utils';
import { InfoFilled } from '@element-plus/icons-vue';
import { t } from '@/i18n/i18n';

const emit = defineEmits(['confirmAnalysisOptions']);

const options = reactive({
  keepUnreachableObjects: true,
  strictness: 'stop'
});

function onConfirm() {
  emit('confirmAnalysisOptions', options);
}
</script>
<template>
  <el-form
    :model="options"
    label-position="right"
    label-width="200px"
    style="width: 600px"
    size="large"
  >
    <el-form-item label="Keep unreachable objects">
      <el-switch v-model="options.keepUnreachableObjects"></el-switch>
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

    <el-form-item label="Strictness" slot="reference">
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

    <el-form-item>
      <el-button type="primary" @click="onConfirm">{{ t('common.confirm') }}</el-button>
    </el-form-item>
  </el-form>
</template>
<style scoped></style>
