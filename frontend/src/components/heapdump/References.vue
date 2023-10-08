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
import { prettySize } from '@/support/utils';
import CommonTable from '@/components/heapdump/CommonTable.vue';
import { getIcon, getInboundIcon, getOutboundIcon } from '@/components/heapdump/icon-helper';
import { useSelectedObject } from '@/composables/heapdump/selected-object';
import { commonMenu as menu } from '@/components/heapdump/menu';

defineOptions({
  inheritAttrs: false
});

const props = defineProps({
  outbounds: {
    type: Boolean,
    default: true
  },
  objectId: {
    type: Number,
    required: true
  },
  byHistogram: {
    type: Boolean,
    default: false
  }
});

const { selectedObjectId } = useSelectedObject();

const tableProps = ref({
  columns: [
    {
      label: () => 'Class Name',
      minWidth: 250,
      content: (d) => d.label,
      prefix: (d) => d.prefix,
      icon: (d) =>
        props.outbounds
          ? getOutboundIcon(d.gCRoot, d.objectType)
          : d.__meta.tier == 0
          ? getIcon(d.gCRoot, d.objectType)
          : getInboundIcon(d.gCRoot, d.objectType),
      suffix: (d) => d.suffix
    },
    {
      label: 'Shallow Size',
      width: 130,
      align: 'right',
      property: 'shallowHeap',
      content: (d) => prettySize(d.shallowSize)
    },
    {
      label: 'Retained Size',
      width: 130,
      align: 'right',
      property: 'retainedHeap',
      content: (d) => prettySize(d.retainedSize)
    }
  ],

  apis: [
    {
      api: props.byHistogram ? 'histogram.objects' : 'object',
      parameters: () => {
        return {
          [props.byHistogram ? 'classId' : 'objectId']: props.objectId
        };
      },
      respMapper: (resp) => (props.byHistogram ? resp : [resp]),
      paged: props.byHistogram
    },
    {
      api: props.outbounds ? 'outbounds' : 'inbounds',
      parameters: (d) => {
        return {
          objectId: d.objectId
        };
      },
      paged: true
    }
  ],

  hasChildren: () => true,

  onRowClick: (d) => {
    if (d.hasOwnProperty('objectId')) {
      selectedObjectId.value = d.objectId;
    }
  },

  menu
});
</script>
<template>
  <CommonTable ref="table" v-bind="tableProps" />
</template>
