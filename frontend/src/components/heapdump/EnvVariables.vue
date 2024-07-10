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
import CommonTable from '@/components/heapdump/CommonTable.vue';
import { Search } from '@element-plus/icons-vue';
import { hdt } from '@/components/heapdump/utils';
import { t } from '@/i18n/i18n';

const search = ref('');

const tableProps = ref({
  columns: [
    {
      label: () => hdt('column.key'),
      width: '40%',
      property: 'key'
    },
    {
      label: () => hdt('column.value'),
      width: '60%',
      property: 'value'
    }
  ],

  apis: [
    {
      api: 'envVariables',
      respMapper: (d) => {
        let result = [];
        for (let key in d) {
          result.push({
            key,
            value: d[key]
          });
        }
        return result;
      },
      paged: false
    }
  ],

  showOverflowTooltip: false,

  dataFilter: (d) =>
    !search.value ||
    d.key.toLowerCase().includes(search.value.toLowerCase()) ||
    d.value.toLowerCase().includes(search.value.toLowerCase())
});
</script>
<template>
  <div style="height: 100%; position: relative">
    <div
      style="
        position: absolute;
        right: 10px;
        z-index: 10;
        height: 32px;
        flex-shrink: 0;
        margin-bottom: 8px;
        overflow: hidden;
        display: flex;
        align-items: center;
      "
    >
      <el-input
        v-model="search"
        size="small"
        style="width: 180px"
        :placeholder="t('common.search')"
        :suffix-icon="Search as any"
      />
    </div>
    <CommonTable ref="table" v-bind="tableProps" />
  </div>
</template>
