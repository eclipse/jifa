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
import { prettyCount, prettySize } from '@/support/utils';
import { getIcon } from '@/components/heapdump/icon-helper';
import { commonMenu as menu } from '@/components/heapdump/menu';
import { useSelectedObject } from '@/composables/heapdump/selected-object';

const { selectedObjectId } = useSelectedObject();

// only support FROM_GC_ROOTS now
const grouping = 'FROM_GC_ROOTS';

const props = defineProps({
  objectIdsOrClassId: {
    required: true
  },
  byHistogram: {
    type: Boolean,
    default: false
  }
});

const tableProps = ref({
  columns: [
    {
      label: () => 'Class Name',
      minWidth: 250,
      icon: (d) => getIcon(d.gCRoot, d.objectType),
      content: (d) => d.className,
      suffix: (d) => d.suffix
    },
    {
      label: 'Ref. Objects',
      width: 130,
      align: 'right',
      content: (d) => prettyCount(d.refObjects)
    },
    {
      label: 'Shallow Heap',
      width: 130,
      align: 'right',
      content: (d) => prettySize(d.shallowHeap)
    },
    {
      label: 'Ref. Shallow Heap',
      width: 130,
      align: 'right',
      content: (d) => prettySize(d.refShallowHeap)
    },
    {
      label: 'Retained Heap',
      width: 130,
      align: 'right',
      content: (d) => prettySize(d.retainedHeap)
    }
  ],

  apis: [
    {
      api: props.byHistogram
        ? 'mergePathToGCRoots.roots.byClassId'
        : 'mergePathToGCRoots.roots.byObjectIds',
      parameters() {
        return {
          [props.byHistogram ? 'classId' : 'objectIds']: props.objectIdsOrClassId,
          grouping
        };
      },
      paged: true
    },
    {
      api: props.byHistogram
        ? 'mergePathToGCRoots.children.byClassId'
        : 'mergePathToGCRoots.children.byObjectIds',
      parameters(d) {
        let objectIdPathInGCPathTree = [];
        let p = d;
        do {
          objectIdPathInGCPathTree.push(p.objectId);
          p = p.__meta.parent;
        } while (p);
        objectIdPathInGCPathTree = objectIdPathInGCPathTree.reverse();
        return {
          [props.byHistogram ? 'classId' : 'objectIds']: props.objectIdsOrClassId,
          objectIdPathInGCPathTree,
          grouping
        };
      },
      paged: true
    }
  ],

  onRowClick: (d) => {
    if (d.hasOwnProperty('objectId')) {
      selectedObjectId.value = d.objectId;
    }
  },

  menu,

  hasChildren: () => true
});
</script>
<template>
  <CommonTable ref="table" v-bind="tableProps" />
</template>
