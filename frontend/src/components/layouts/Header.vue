<!--
    Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<script setup lang="ts">
import { availableLocales, currentLocale, setLocale } from '@/i18n/i18n';
import { isDark } from '@/composables/theme';
import Sun from '@/components/icons/Sun.vue';
import Moon from '@/components/icons/Moon.vue';
import Logout from 'vue-material-design-icons/Logout.vue';
import Translate from 'vue-material-design-icons/Translate.vue';
import Github from 'vue-material-design-icons/Github.vue';
import eclipseLogoUrl from '@/assets/eclipse_incubation_horizontal.svg';
import eclipseLogoUrlForDark from '@/assets/eclipse_incubation_horizontal_dark.svg';
import { t } from '@/i18n/i18n';
import { useEnv } from '@/stores/env';
import { useHeaderToolbar } from '@/composables/header-toolbar';
import { useDebouncedRef } from '@/composables/debounced-ref';
import { useRouter } from 'vue-router';

const logo = computed(() => (isDark.value ? eclipseLogoUrlForDark : eclipseLogoUrl));

const env = useEnv();

const localeOptions = computed(() => {
  let result = {};
  Object.assign(result, availableLocales);
  delete result[currentLocale.value];
  return result;
});

const themeSwitchColor = computed(() => (isDark.value ? '#1a1a1a' : '#ffffff'));

const router = useRouter();

const { toolbar } = useHeaderToolbar();

const width = useDebouncedRef(window.innerWidth);
const height = useDebouncedRef(window.innerHeight);

const bannerVisible = computed(() => width.value >= 1000);
const bannerClosed = ref(false);

function handleResize() {
  width.value = window.innerWidth;
}

onMounted(() => {
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});
</script>
<template>
  <header class="header">
    <div class="container">
      <div class="left-side">
        <img
          style="height: calc(var(--ej-header-height) - 10px); cursor: pointer"
          alt=""
          :src="logo"
          @click="router.push({ name: 'Files' })"
        />
        <span class="project-name" @click="router.push({ name: 'Files' })">Jifa</span>

        <template v-if="toolbar">
          <component :is="toolbar" />
        </template>
      </div>

      <div class="right-side">
        <el-popover
          placement="bottom"
          :show-arrow="false"
          :popper-style="{ 'min-width': '100px', width: '100px', padding: '5px 0' }"
        >
          <template #reference>
            <Translate
              :size="20"
              style="height: 20px; color: var(--el-text-color-regular); cursor: pointer"
            />
          </template>
          <template #default>
            <div class="popover-item" v-for="(label, key) in localeOptions" @click="setLocale(key)">
              {{ label }}
            </div>
          </template>
        </el-popover>

        <el-divider direction="vertical" />

        <el-switch
          class="theme-switch"
          v-model="isDark"
          :active-action-icon="Moon"
          :inactive-action-icon="Sun"
        />

        <el-divider direction="vertical" />

        <a href="https://github.com/eclipse/jifa" target="_blank" style="height: 24px">
          <Github class="github-icon" title="Github" :size="24" />
        </a>
      </div>
    </div>
  </header>
  <div class="banner" v-if="bannerVisible && !bannerClosed">
    <el-alert :type="isDark ? 'success' : 'info'" center @close="bannerClosed = true">
      <template #title>
        <i18n-t keypath="jifa.banner" tag="span">
          <template v-slot:GitHub>
            <a
              style="text-decoration: underline"
              href="https://github.com/eclipse/jifa"
              target="_blank"
              >GitHub</a
            >
          </template>
        </i18n-t>
        ⭐
      </template>
    </el-alert>
  </div>
</template>

<style scoped>
.header {
  flex-shrink: 0;
  width: 100%;
  height: var(--ej-header-height);
  overflow: hidden;
  background-color: var(--el-bg-color);
  border-radius: var(--el-border-radius-base);
}

.banner {
  position: fixed;
  top: 12px;
  left: calc((100vw - 420px) / 2);
  right: calc((100vw - 420px) / 2);
  width: 420px;
  height: var(--ej-header-height);
  display: flex;
  justify-content: center;
  align-items: center;
}

.container {
  height: 100%;
  display: flex;
  justify-content: space-between;
}

.left-side {
  display: flex;
  align-items: center;
}

.project-name {
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 3px;
  cursor: pointer;
}

.right-side {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-right: 14px;
}

.popover-item {
  padding: 5px;
  text-align: center;
  color: var(--el-text-color-regular);
  cursor: pointer;
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
}

:deep(.el-divider--vertical) {
  margin: 0 14px !important;
}

.popover-item:hover {
  color: var(--el-menu-hover-text-color);
  background-color: var(--el-menu-hover-bg-color);
}

.theme-switch {
  --el-switch-on-color: #323238;
}

.theme-switch :deep(.el-switch__action) {
  background-color: v-bind(themeSwitchColor);
}

.github-icon {
  color: var(--el-text-color-regular);
}

.github-icon:hover {
  color: var(--el-text-color-primary);
}
</style>
