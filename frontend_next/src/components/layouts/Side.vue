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
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { DataAnalysis, Files } from '@element-plus/icons-vue';
import { useAnalysisStore } from '@/stores/analysis';
import type { FileType } from '@/composables/file-types';

const router = useRouter();
const route = useRoute();

const defaultActive = computed(() => (route.meta && route.meta.fileType ? 'Analysis' : route.name));

const analysis = useAnalysisStore();

function onSelect(index) {
  if (index === 'Analysis') {
    router.push({
      path: `/${(<FileType>analysis.fileType).routePath}/${analysis.target}`
    });
  } else {
    analysis.leaveGuard = false;
    router.push({ name: index });
  }
}
</script>
<template>
  <div class="ej-side">
    <el-menu class="ej-side-menu" :default-active="defaultActive" @select="onSelect" collapse>
      <el-menu-item index="Files">
        <el-icon>
          <Files />
        </el-icon>
      </el-menu-item>
      <el-menu-item index="Analysis" :disabled="!analysis.target">
        <el-icon>
          <DataAnalysis />
        </el-icon>
      </el-menu-item>
    </el-menu>
  </div>
</template>
<style scoped>
.ej-side {
  position: fixed;
  top: var(--ej-header-height);
  left: 0;
  bottom: 0;
  transition: left 0.3s;
}

.ej-side-menu {
  height: 100%;
  background-color: var(--el-bg-color);
}
</style>
