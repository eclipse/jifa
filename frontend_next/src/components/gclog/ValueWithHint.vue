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
import { InfoFilled, WarningFilled } from '@element-plus/icons-vue';

const props = defineProps({
  value: {
    required: false
  },
  hint: {
    required: false
  },
  danger: {
    required: false
  },
  dangerHint: {
    required: false
  }
});
</script>
<template>
  <el-space size="small">
    <slot>
      <span :class="{ danger }">
        {{ value }}
      </span>
    </slot>
    <el-tooltip placement="right" :show-arrow="false" v-if="hint || (danger && dangerHint)">
      <template #content>
        <div style="max-width: 600px">
          <template v-if="typeof hint === 'string'">
            <div>{{ hint }}</div>
          </template>
          <template v-else>
            <div v-for="item in hint">{{ item }}</div>
          </template>

          <template v-if="typeof dangerHint === 'string'">
            <div>{{ dangerHint }}</div>
          </template>
          <template v-else>
            <div v-for="item in dangerHint">{{ item }}</div>
          </template>
        </div>
      </template>
      <el-icon>
        <InfoFilled v-if="!danger" />
        <WarningFilled v-else />
      </el-icon>
    </el-tooltip>
  </el-space>
</template>
<style scoped>
:deep(.el-space__item:last-child) {
  margin-right: 0 !important;
}

.danger {
  color: var(--el-color-danger);
  font-weight: 600;
}
</style>
