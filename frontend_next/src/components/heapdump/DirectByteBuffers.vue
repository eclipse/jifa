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
import { ref } from 'vue';
import { prettyCount, prettySize } from '@/support/utils';
import { ICONS } from '@/components/heapdump/icon-helper';
import { useSelectedObject } from '@/composables/heapdump/selected-object';
import CommonTable from '@/components/heapdump/CommonTable.vue';
import { commonMenu as menu } from '@/components/heapdump/menu';

const { selectedObjectId } = useSelectedObject();

const tableProps = ref({
  columns: [
    {
      label: () => 'Label',
      minWidth: 250,
      sortable: false,
      content: (d) => d.label,
      icon: () => ICONS.objects.instance_obj
    },
    {
      label: () => 'position',
      width: 120,
      align: 'right',
      content: (d) => prettyCount(d.position)
    },
    {
      label: 'limit',
      width: 130,
      align: 'right',
      content: (d) => prettySize(d.limit)
    },
    {
      label: 'capacity',
      width: 130,
      align: 'right',
      content: (d) => prettySize(d.capacity),
      contentOfSummary: (d) => prettySize(d.capacity)
    }
  ],

  apis: [
    {
      api: 'directByteBuffer.records',
      paged: true,
      summaryApi: 'directByteBuffer.summary'
    }
  ],

  onRowClick: (d) => {
    if (d.hasOwnProperty('objectId')) {
      selectedObjectId.value = d.objectId;
    }
  },

  menu
});
</script>
<template>
  <CommonTable v-bind="tableProps" />
</template>
