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
import { onMounted, onUnmounted, ref } from 'vue';
import type { Menu } from '@/composables/contextmenu';
import CommonContextMenuContent from '@/components/common/CommonContextMenuContent.vue';

const props = defineProps<{
  menu: Menu;
}>();

const data = ref();

const contextmenu = ref();

const show = function (event, payload) {
  data.value = payload;
  contextmenu.value.show(event);
};

function hideOnClick(e) {
  let div = contextmenu.value.contextmenuRef;
  if (div && !div.contains(e.target)) {
    contextmenu.value.hide();
  }
}

function hideOnScrollOrResize() {
  let div = contextmenu.value.contextmenuRef;
  if (div) {
    contextmenu.value.hide();
  }
}

onMounted(() => {
  window.addEventListener('click', hideOnClick, true);
  window.addEventListener('scroll', hideOnScrollOrResize, true);
  window.addEventListener('resize', hideOnScrollOrResize, true);
});

onUnmounted(() => {
  window.removeEventListener('click', hideOnClick, true);
  window.removeEventListener('scroll', hideOnScrollOrResize, true);
  window.removeEventListener('resize', hideOnScrollOrResize, true);
});

defineExpose({ show });
</script>
<template>
  <v-contextmenu ref="contextmenu">
    <CommonContextMenuContent :items="menu.items" :data="data" />
  </v-contextmenu>
</template>
