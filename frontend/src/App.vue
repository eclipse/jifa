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
import { elementLocale, t } from '@/i18n/i18n';
import Header from '@/components/layouts/Header.vue';
import Side from '@/components/layouts/Side.vue';
import Main from '@/components/layouts/Main.vue';
import LoginForm from '@/components/forms/LoginForm.vue';
import axios from 'axios';
import { useEnv } from '@/stores/env';
import { ElLoading } from 'element-plus';

const env = useEnv();
const error = ref(false);

const shaken = ref(false);

onMounted(() => {
  const loadingService = ElLoading.service({
    lock: true,
    background: 'rgba(0, 0, 0, 0.7)'
  });
  axios
    .get('/jifa-api/handshake')
    .then((resp) => {
      env.handleHandshakeData(resp.data);
      shaken.value = true;
      loadingService.close();
    })
    .catch((e) => {
      if (e.response && e.response.status === 401) {
        env.resetToken();
        location.reload();
      } else {
        error.value = true;
        shaken.value = true;
        loadingService.close();
      }
    });
});
</script>

<template>
  <el-config-provider :locale="elementLocale">
    <LoginForm v-if="env.loginFormVisible" />
    <div class="ej-container">
      <Header />

      <div class="ej-body" v-if="shaken">
        <el-result
          class="ej-error"
          v-if="error"
          icon="error"
          :title="t('serviceUnavailable.title')"
          :sub-title="`${t('serviceUnavailable.subtitle')}`"
        />

        <template v-else-if="!env.loginRequired">
          <Side />
          <div class="ej-main">
            <Main />
          </div>
        </template>
      </div>
    </div>
  </el-config-provider>
</template>

<style scoped>
.ej-container {
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.ej-body {
  margin-top: var(--ej-header-height);
  height: calc(100vh - var(--ej-header-height));
  overflow: hidden;
}

.ej-main {
  margin-left: var(--ej-side-width);
  height: 100%;
  padding: var(--ej-main-padding);
  overflow: hidden;
}

.ej-error {
  height: 100%;
}
</style>
