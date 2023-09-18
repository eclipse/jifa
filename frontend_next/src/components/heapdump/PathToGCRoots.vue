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
import { onMounted, ref } from 'vue';
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';
import { getIcon, getInboundIcon } from '@/components/heapdump/icon-helper';
import { prettySize } from '@/support/utils';
import { useSelectedObject } from '@/composables/heapdump/selected-object';
import { t } from '@/i18n/i18n';
import CommonContextMenu from '@/components/common/CommonContextMenu.vue';
import { commonMenu as menu } from '@/components/heapdump/menu';

const props = defineProps({
  objectId: {
    type: Number,
    required: true
  }
});

const { request } = useAnalysisApiRequester();

const loading = ref(false);

const tree = ref([]);
let count = 0;

function merge(parent, children) {
  for (let i = 0; i < children.length; i++) {
    let found = false;
    let child = children[i];
    for (let j = 0; j < parent.children.length; j++) {
      let oldChild = parent.children[j];
      if (oldChild.objectId === child.objectId) {
        found = true;
        merge(oldChild, child.children);
        break;
      }
    }

    if (!found) {
      parent.children.push(child);
    }
  }
}

function load() {
  loading.value = true;
  request('pathToGCRoots', {
    skip: count,
    objectId: props.objectId,
    count: 25
  }).then((data: any) => {
    if (tree.value.length == 0) {
      tree.value.push(data.tree);
    } else {
      merge(tree.value[0], data.tree.children);
      // remove the load more item
      tree.value.splice(1, 1);
    }

    count += data.count;
    if (data.hasMore) {
      tree.value.push({ __loadMore: true });
    }

    loading.value = false;
  });
}

onMounted(() => load());

const { selectedObjectId } = useSelectedObject();

function onClick(id) {
  selectedObjectId.value = id;
}

const contextmenu = ref();

function nodeContextmenuCallback(event, data) {
  if (contextmenu.value) {
    contextmenu.value.show(event, data);
  } else {
    event.preventDefault();
  }
}
</script>
<template>
  <CommonContextMenu :menu="menu" ref="contextmenu" />

  <el-scrollbar v-loading="loading">
    <el-tree
      :data="tree"
      node-key="objectId"
      default-expand-all
      :expand-on-click-node="false"
      @node-contextmenu="nodeContextmenuCallback"
    >
      <template #default="{ node, data }">
        <div style="font-size: 12px; flex-grow: 1" @click="load" v-if="data.__loadMore">
          {{ t('common.clickToLoadMore') }}
        </div>
        <div class="ej-tree-node" @click="onClick(data.objectId)" v-else>
          <div style="display: flex; align-items: center">
            <img
              :src="
                data.origin
                  ? getIcon(data.gCRoot, data.objectType)
                  : getInboundIcon(data.gCRoot, data.objectType)
              "
              style="margin-right: 5px"
            />
            <strong style="padding-right: 5px">{{ data.prefix }}</strong>
            {{ data.label }}
            <span
              style="font-weight: bold; color: var(--el-text-color-secondary); padding-left: 5px"
            >
              {{ data.suffix }}
            </span>
          </div>
          <span style="padding-right: 5px">
            {{ prettySize(data.shallowSize) }} / {{ prettySize(data.retainedSize) }}
          </span>
        </div>
      </template>
    </el-tree>
  </el-scrollbar>
</template>
<style scoped>
.ej-tree-node {
  flex: 1;
  display: flex;
  font-size: 12px;
  padding-right: 8px;
  justify-content: space-between;
}
</style>
