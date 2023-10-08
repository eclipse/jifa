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
import type { Item } from '@/components/heapdump/common-table';
import { hdt } from '@/components/heapdump/utils';
import { prettyCount, prettySize } from '@/support/utils';
import { getIcon, getOutboundIcon, ICONS } from '@/components/heapdump/icon-helper';
import { useSelectedObject } from '@/composables/heapdump/selected-object';

const { selectedObjectId } = useSelectedObject();

const tableProps = ref({
  columns: [
    {
      label: 'Class Name',
      minWidth: 250,
      icon(d) {
        switch (d.__meta.tier) {
          case 0:
            return ICONS.roots;
          case 1:
            return ICONS.objects.class;
          case 2:
            return getIcon(d.gCRoot, d.objectType);
          default:
            return getOutboundIcon(d.gCRoot, d.objectType);
        }
      },
      content(d) {
        switch (d.__meta.tier) {
          case 0:
            return d.className;
          case 1:
            return d.className;
          default:
            return d.label;
        }
      },
      prefix(d) {
        if (d.__meta.tier <= 2) {
          return '';
        }
        return d.prefix;
      },
      suffix(d) {
        if (d.__meta.tier <= 2) {
          return '';
        }
        return d.suffix;
      }
    },
    {
      label: () => hdt('column.objectCount'),
      width: 120,
      align: 'right',
      content: (d) => prettyCount(d.objects)
    },
    {
      label: 'Shallow Heap',
      width: 130,
      align: 'right',
      content(d) {
        if (d.__meta.tier < 2) {
          return '';
        }
        return prettySize(d.shallowSize);
      }
    },
    {
      label: 'Retained Heap',
      width: 130,
      align: 'right',
      content(d) {
        if (d.__meta.tier < 2) {
          return '';
        }
        return prettySize(d.retainedSize);
      }
    }
  ],

  apis: [
    {
      api: 'GCRoots'
    },
    {
      api: 'GCRoots.classes',
      parameters(d: Item) {
        return {
          rootTypeIndex: d.__meta.index
        };
      },
      paged: true
    },
    {
      api: 'GCRoots.class.objects',
      parameters(d: Item) {
        return {
          rootTypeIndex: d.__meta.parent?.__meta.index,
          classIndex: d.__meta.index
        };
      },
      paged: true
    },
    {
      api: 'outbounds',
      parameters(d) {
        return {
          objectId: d.objectId
        };
      },
      paged: true
    }
  ],

  hasChildren() {
    return true;
  },

  onRowClick(d) {
    if (d.__meta.tier > 0 && d.hasOwnProperty('objectId')) {
      selectedObjectId.value = d.objectId;
    }
  }
});
</script>
<template>
  <CommonTable v-bind="tableProps" />
</template>
