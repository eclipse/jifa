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
import type { Item, SubMenu } from '@/composables/contextmenu';

const props = defineProps<{
  items: (SubMenu | Item)[];
  data?: any;
}>();

function titleOf(i: Item | SubMenu) {
  if (typeof i.title === 'string') {
    return i.title;
  }
  return i.title();
}
</script>
<template>
  <template v-for="item in items">
    <template v-if="'items' in item">
      <v-contextmenu-submenu :title="titleOf(item)">
        <CommonContextMenuContent :items="item.items" :data="data" />
      </v-contextmenu-submenu>
    </template>
    <template v-else>
      <v-contextmenu-item @click="(item as Item).onClick(data)">{{
        titleOf(item)
      }}</v-contextmenu-item>
    </template>
    <v-contextmenu-divider v-if="item.followedByDivider" />
  </template>
</template>
