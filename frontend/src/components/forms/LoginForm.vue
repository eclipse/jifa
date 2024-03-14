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
import { Lock, Message, Postcard, User } from '@element-plus/icons-vue';
import axios from 'axios';
import { useCipher } from '@/composables/cipher';
import Github from 'vue-material-design-icons/Github.vue';
import Google from 'vue-material-design-icons/Google.vue';
import Microsoft from 'vue-material-design-icons/Microsoft.vue';
import Login from 'vue-material-design-icons/Login.vue';
import { useEnv } from '@/stores/env';
import { t } from '@/i18n/i18n';

const env = useEnv();

const { encrypt } = useCipher();

const form = ref();

const params = reactive({
  name: '',
  username: '',
  password: '',
  email: ''
});

const rules = {
  name: [
    {
      required: true,
      message: () => t('form.requiredMessage', [t('loginForm.fullNameLabel')]),
      trigger: 'blur'
    },
    {
      min: 3,
      max: 64,
      message: () => t('form.invalidLengthMessage', [t('loginForm.fullNameLabel'), 3, 64]),
      trigger: 'blur'
    }
  ],
  username: [
    {
      required: true,
      message: () => t('form.requiredMessage', [t('loginForm.usernameLabel')]),
      trigger: 'blur'
    },
    {
      min: 3,
      max: 32,
      message: t('form.invalidLengthMessage', [t('loginForm.usernameLabel'), 3, 32]),
      trigger: 'blur'
    }
  ],
  password: [
    {
      required: true,
      message: () => t('form.requiredMessage', [t('loginForm.passwordLabel')]),
      trigger: 'blur'
    },
    {
      min: 8,
      max: 32,
      message: t('form.invalidLengthMessage', [t('loginForm.passwordLabel'), 8, 32]),
      trigger: 'blur'
    }
  ],
  email: [
    {
      required: true,
      message: () => t('form.requiredMessage', [t('loginForm.emailLabel')]),
      trigger: 'blur'
    },
    { type: 'email', message: () => t('loginForm.invalidEmailMessage'), trigger: 'blur' }
  ]
};

const doLogin = ref(true);
const processing = ref(false);
const message = ref('');

const submit = async (form) => {
  await form.validate((valid) => {
    if (valid) {
      processing.value = true;
      if (doLogin.value) {
        login();
      } else {
        signup();
      }
    }
  });
};
const reset = (form) => {
  form.resetFields();
};

watch(doLogin, () => {
  reset(form.value);
  message.value = '';
});

const iconMap = new Map();
iconMap['github'] = Github;
iconMap['google'] = Google;
iconMap['microsoft'] = Microsoft;

function icon(name: string) {
  let r = iconMap[name.toLowerCase()];
  return r ? r : Login;
}

function login() {
  let p = {
    username: params.username,
    password: encrypt(params.password)
  };
  axios
    .post('/jifa-api/login', p)
    .then((resp) => env.handleLoginOrSignupResponse(resp))
    .catch((e) => handleError(e));
}

function signup() {
  let p = {
    name: params.name,
    username: params.email,
    password: encrypt(params.password)
  };
  axios
    .post('/jifa-api/user', p)
    .then((resp) => {
      env.handleLoginOrSignupResponse(resp);
    })
    .catch((e) => handleError(e));
}

function oauth2Login(link) {
  processing.value = true;
  window.location.href = link;
}

function handleError(e) {
  message.value = e.response.data.message;
  processing.value = false;
}

function close(done) {
  if (processing.value) {
    return;
  }
  done();
}
</script>

<template>
  <el-dialog
    v-model="env.loginFormVisible"
    :show-close="env.allowAnonymousAccess"
    :close-on-click-modal="env.allowAnonymousAccess"
    :close-on-press-escape="env.allowAnonymousAccess"
    width="480px"
    :before-close="close"
  >
    <div
      style="width: 100%; display: flex; flex-direction: column; align-items: center"
      v-loading="processing"
    >
      <h2 v-if="doLogin" style="cursor: default">{{ t('loginForm.loginTitle') }}</h2>
      <h2 v-else style="cursor: default">{{ t('loginForm.signupTitle') }}</h2>

      <el-space v-if="doLogin && env.allowRegistration">
        <el-text type="info">{{ t('loginForm.loginSubtitle') }}</el-text>
        <el-text
          :style="{ cursor: processing ? 'not-allowed' : 'pointer' }"
          type="primary"
          tag="b"
          @click="doLogin = processing ? doLogin : false"
        >
          {{ t('loginForm.signup') }}
        </el-text>
      </el-space>

      <el-space v-if="!doLogin">
        <el-text type="info">{{ t('loginForm.signupSubtitle') }}</el-text>
        <el-text
          :style="{ cursor: processing ? 'not-allowed' : 'pointer' }"
          type="primary"
          tag="b"
          @click="doLogin = processing ? doLogin : true"
        >
          {{ t('loginForm.login') }}
        </el-text>
      </el-space>

      <el-form
        style="margin-top: 10px; width: 350px"
        label-position="top"
        size="large"
        hide-required-asterisk
        :disabled="processing"
        :model="params"
        :rules="rules as any"
        ref="form"
      >
        <el-form-item :label="t('loginForm.fullNameLabel')" v-if="!doLogin" prop="name">
          <el-input
            v-model="params.name"
            :placeholder="t('loginForm.fullNamePlaceholder')"
            clearable
            :prefix-icon="Postcard as any"
          >
          </el-input>
        </el-form-item>

        <el-form-item :label="t('loginForm.usernameLabel')" v-if="doLogin" prop="username">
          <el-input
            v-model="params.username"
            :placeholder="t('loginForm.usernamePlaceholder')"
            clearable
            :prefix-icon="User as any"
          >
          </el-input>
        </el-form-item>

        <el-form-item
          :label="t('loginForm.emailLabel') + t('loginForm.usedAsUsername')"
          v-if="!doLogin"
          prop="email"
        >
          <el-input
            v-model="params.email"
            :placeholder="t('loginForm.emailPlaceholder')"
            clearable
            :prefix-icon="Message as any"
          >
          </el-input>
        </el-form-item>

        <el-form-item :label="t('loginForm.passwordLabel')" prop="password">
          <el-input
            v-model="params.password"
            :placeholder="t('loginForm.passwordPlaceholder')"
            type="password"
            :prefix-icon="Lock as any"
            clearable
            show-password
            @keydown.enter="submit(form)"
          >
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" style="width: 100%" @click="submit(form)">
            {{ doLogin ? t('loginForm.login') : t('loginForm.signup') }}
          </el-button>
        </el-form-item>

        <el-divider v-if="env.supportedOauth2Login">
          <el-text size="small">{{ t('loginForm.or') }}</el-text>
        </el-divider>

        <el-form-item v-for="(value, key) in env.oauth2LoginLinks" v-if="env.supportedOauth2Login">
          <el-button style="width: 100%" @click="oauth2Login(value)">
            <component :is="icon(key as string)" class="ej-oauth2-icon" />
            {{ t('loginForm.continueWith', [key]) }}
          </el-button>
        </el-form-item>
      </el-form>

      <div
        style="
          width: 350px;
          padding: 8px 16px;
          border-radius: var(--el-border-radius-small);
          font-size: 12px;
          word-break: break-all;
          background-color: var(--el-color-error-light-9);
          color: var(--el-color-error);
        "
        v-if="message"
      >
        {{ message }}
      </div>
    </div>
  </el-dialog>
</template>
<style scoped>
.el-text {
  cursor: default;
}

.ej-oauth2-icon {
  height: 24px;
  margin-right: 10px;
  color: var(--el-text-color-primary);
}
</style>
