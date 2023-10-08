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
const props = defineProps({
  header: {
    type: String,
    required: true
  },
  headerBarId: {
    type: String,
    required: true
  }
});
</script>
<template>
  <div class="card-container">
    <el-card class="card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>{{ header }}</span>
          <div :id="headerBarId"></div>
        </div>
      </template>

      <slot></slot>
    </el-card>
  </div>
</template>
<style scoped>
:deep(.el-card__header) {
  height: 56px !important;
  padding: 0 20px !important;
}

.card-header {
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-container {
  display: flex;
  justify-content: center;
}

.card {
  width: 100%;
  margin: 18px;
}
</style>
