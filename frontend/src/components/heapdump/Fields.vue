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
import { useSelectedObject } from '@/composables/heapdump/selected-object';
import { hdt } from '@/components/heapdump/utils';
import CommonTable from '@/components/heapdump/CommonTable.vue';

const props = defineProps({
  static: {
    type: Boolean,
    default: false
  }
});

const { selectedObjectId } = useSelectedObject();

function type2String(fieldType) {
  switch (fieldType) {
    case 2:
      return 'ref';
    case 4:
      return 'boolean';
    case 5:
      return 'char';
    case 6:
      return 'float';
    case 7:
      return 'double';
    case 8:
      return 'byte';
    case 9:
      return 'short';
    case 10:
      return 'int';
    case 11:
      return 'long';
  }
  return 'unknown';
}

const tableProps = ref({
  columns: [
    {
      label: () => hdt('field.name'),
      width: 140,
      property: 'name'
    },
    {
      label: () => hdt('field.value'),
      content: (o) => (o.fieldType === 2 && !o.value ? 'null' : o.value)
    },
    {
      label: () => hdt('field.type'),
      width: 65,
      content: (o) => type2String(o.fieldType)
    }
  ],
  apis: [
    {
      api: props.static ? 'inspector.staticFields' : 'inspector.fields',
      parameters: () => {
        return { objectId: selectedObjectId.value };
      },
      paged: true
    }
  ],
  watch: [selectedObjectId]
});
</script>
<template>
  <CommonTable ref="table" v-bind="tableProps" v-if="selectedObjectId >= 0" />
</template>
