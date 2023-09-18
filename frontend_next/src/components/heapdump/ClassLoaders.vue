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
import { prettyCount } from '@/support/utils';
import { ICONS } from '@/components/heapdump/icon-helper';
import { useSelectedObject } from '@/composables/heapdump/selected-object';
import CommonTable from '@/components/heapdump/CommonTable.vue';
import { commonMenu as menu } from '@/components/heapdump/menu';

const { selectedObjectId } = useSelectedObject();

const tableProps = ref({
  columns: [
    {
      label: () => 'Class Name',
      minWidth: 250,
      sortable: false,
      icon: (d) => {
        if (d.__meta.tier == 0 || d.classLoader) {
          return d.hasParent ? ICONS.objects.out.classloader_obj : ICONS.objects.classloader_obj;
        }
        return ICONS.objects.class;
      },
      prefix: (d) => d.prefix,
      content: (d) => d.label
    },
    {
      label: () => 'Defined Classes',
      width: 120,
      align: 'right',
      content: (d) => {
        return d.classLoader ? prettyCount(d.definedClasses) : null;
      },
      contentOfSummary: (d) => prettyCount(d.definedClasses)
    },
    {
      label: 'Object Count',
      width: 130,
      align: 'right',
      content: (d) => prettyCount(d.numberOfInstances),
      contentOfSummary: (d) => prettyCount(d.numberOfInstances)
    }
  ],

  apis: [
    {
      api: 'classLoaderExplorer.classLoader',
      paged: true,
      summaryApi: 'classLoaderExplorer.summary'
    },
    {
      api: 'classLoaderExplorer.children',
      parameters(d) {
        return {
          classLoaderId: d.objectId
        };
      },
      paged: true
    }
  ],

  hasChildren: (d) => d.classLoader,

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
<style scoped></style>
