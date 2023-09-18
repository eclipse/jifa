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
import { prettySize } from '@/support/utils';
import CommonTable from '@/components/heapdump/CommonTable.vue';
import {
  getClassRefInboundIcon,
  getClassRefOutboundIcon,
  ICONS
} from '@/components/heapdump/icon-helper';
import { useSelectedObject } from '@/composables/heapdump/selected-object';
import { commonMenu as menu } from '@/components/heapdump/menu';

const { selectedObjectId } = useSelectedObject();

const props = defineProps({
  outbounds: {
    type: Boolean,
    default: true
  },
  objectId: {
    type: Number,
    required: true
  }
});

const tableProps = ref({
  columns: [
    {
      label: () => 'Class Name',
      minWidth: 250,
      content: (d) => d.label,
      prefix: (d) => d.prefix,
      icon: (d) =>
        props.outbounds
          ? getClassRefOutboundIcon(d.type)
          : d.__meta.tier == 0
          ? ICONS.objects.class
          : getClassRefInboundIcon(d.type),
      suffix: (d) => d.suffix
    },
    {
      label: 'Objects',
      width: 130,
      align: 'right',
      content: (d) => prettySize(d.objects)
    },
    {
      label: 'Shallow Heap',
      width: 130,
      align: 'right',
      content: (d) => prettySize(d.shallowSize)
    }
  ],

  apis: [
    {
      api: props.outbounds ? 'classReference.outbounds.class' : 'classReference.inbounds.class',
      parameters: () => {
        return {
          objectId: props.objectId
        };
      },
      respMapper: (resp) => [resp]
    },
    {
      api: props.outbounds
        ? 'classReference.outbounds.children'
        : 'classReference.inbounds.children',
      parameters: (d) => {
        return {
          objectIds: d.objectIds
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
