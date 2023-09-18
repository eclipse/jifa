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
import { onMounted, ref } from 'vue';
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';
import { t } from '@/i18n/i18n';
import { getIcon } from '@/components/heapdump/icon-helper';
import { prettySize } from '@/support/utils';
import { useSelectedObject } from '@/composables/heapdump/selected-object';

const { request } = useAnalysisApiRequester();
const { selectedObjectId } = useSelectedObject();

const records = ref();
const slices = ref();
const names = ref();
const loading = ref(true);
const noData = ref(false);

function onClick(id) {
  selectedObjectId.value = id;
}

interface Record {
  desc: any;
  paths?: any;
}

interface Report {
  useful: boolean;
  records?: Record[];
  slices?: any[];
}

onMounted(() => {
  request('leak.report').then((resp: Report) => {
    if (resp.useful) {
      records.value = resp.records;
      slices.value = resp.slices;

      names.value = [];
      records.value.forEach((r) => {
        names.value.push(r.name);
      });
    } else {
      noData.value = true;
    }
    loading.value = false;
  });
});
</script>
<template>
  <el-scrollbar v-loading="loading" v-if="!noData">
    <el-collapse v-if="names" v-model="names" style="width: 100%">
      <el-collapse-item v-for="record in records" :title="record.name" :name="record.name">
        <el-tabs tab-position="left">
          <el-tab-pane :label="t('common.description')">
            <div v-html="record.desc"></div>
          </el-tab-pane>
          <el-tab-pane :label="t('common.detail')" v-if="record.paths">
            <el-tree
              :data="record.paths"
              node-key="objectId"
              default-expand-all
              :expand-on-click-node="false"
            >
              <template #default="{ node, data }">
                <div class="ej-tree-node" @click="onClick(data.objectId)">
                  <div style="display: flex; align-items: center">
                    <img :src="getIcon(data.gCRoot, data.objectType)" style="margin-right: 5px" />
                    {{ data.label }}
                  </div>
                  <span>
                    {{ prettySize(data.shallowSize) }} / {{ prettySize(data.retainedSize) }}
                  </span>
                </div>
              </template>
            </el-tree>
          </el-tab-pane>
        </el-tabs>
      </el-collapse-item>
    </el-collapse>
  </el-scrollbar>
  <div
    style="height: 100%; display: flex; justify-content: center; align-items: center; width: 100%"
    v-else
  >
    <el-empty :image-size="200" :description="t('common.noData')" />
  </div>
</template>
<style scoped>
.ej-tree-node {
  flex: 1;
  display: flex;
  font-size: 12px;
  padding-right: 8px;
  justify-content: space-between;
}
</style>
