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
import { gct } from '@/i18n/i18n';
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';
import { Guide } from '@element-plus/icons-vue';

const props = defineProps({
  headerBarId: {
    type: String,
    required: true
  }
});

const { request } = useAnalysisApiRequester();

interface Options {
  gcRelated: { text: string }[];
  other: { text: string }[];
}

const mounted = ref(false);
const loading = ref();
const options = ref<Options>();

function hasGCRelatedOptions() {
  return options.value && options.value!.gcRelated && options.value!.gcRelated.length > 0;
}

function hasOtherOptions() {
  return options.value && options.value!.other && options.value!.other.length > 0;
}

function hasOptions() {
  return hasGCRelatedOptions() && hasOtherOptions();
}

onMounted(() => {
  loading.value = true;
  request('vmOptions').then((data) => {
    options.value = data;
    loading.value = false;
  });
  mounted.value = true;
});
</script>
<template>
  <teleport :to="`#${headerBarId}`" v-if="mounted">
    <el-link
      style="font-size: 14px"
      :underline="false"
      href="https://chriswhocodes.com/vm-options-explorer.html"
      target="_blank"
    >
      <el-icon size="16" style="margin-right: 4px"><Guide /></el-icon>
      VM Options Explorer
    </el-link>
  </teleport>

  <div v-loading="loading">
    <div v-if="hasOptions()">
      <template v-if="hasGCRelatedOptions()">
        <el-text size="large">{{ gct('vmOptions.gcRelatedOptions') }}</el-text>
        <br />
        <el-space style="margin-top: 16px" wrap>
          <el-tag size="large" v-for="option in options!.gcRelated" :key="option.text">
            {{ option.text }}
          </el-tag>
        </el-space>
      </template>

      <el-divider style="margin: 20px 0" v-if="hasGCRelatedOptions() && hasOptions()" />

      <template v-if="hasOtherOptions">
        <el-text size="large">{{ gct('vmOptions.otherOptions') }}</el-text>
        <br />
        <el-space style="margin-top: 16px" wrap>
          <el-tag size="large" v-for="option in options!.other" :key="option.text">
            {{ option.text }}
          </el-tag>
        </el-space>
      </template>
    </div>

    <el-text v-else>{{ gct('vmOptions.unknown') }}</el-text>
  </div>
</template>
