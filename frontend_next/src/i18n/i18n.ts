/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
// @ts-ignore
import ElementEn from 'element-plus/dist/locale/en.min.js';
// @ts-ignore
import ElementZh from 'element-plus/dist/locale/zh-cn.min.js';
import { computed, ref } from 'vue';
import { createI18n } from 'vue-i18n';
import en from './en';
import zh from './zh';

const messages = {
  en: {
    ...en
  },

  zh: {
    ...zh
  }
};

const defaultLocale = 'en';

const elementLocales = new Map<string, unknown>();
elementLocales.set('en', ElementEn);
elementLocales.set('zh', ElementZh);

ElementZh.el.table.emptyText = '没有数据';

export const i18n = createI18n({
  legacy: false,
  allowComposition: true,
  locale: defaultLocale,
  fallbackLocale: defaultLocale,
  messages
});

export const elementLocale = ref(elementLocales.get(defaultLocale));

type LocalType = keyof typeof messages;

export function setLocale(locale: LocalType) {
  i18n.global.locale.value = locale;
  elementLocale.value = elementLocales.get(locale);
  window.localStorage.setItem('jifa-i18n', locale);
}

export const availableLocales = {
  en: 'English',
  zh: '简体中文'
};

export const currentLocale = computed(() => i18n.global.locale.value);

let saved = window.localStorage.getItem('jifa-i18n') as LocalType | null;

if (saved) {
  setLocale(saved);
}

export function t(key: string, args?: any) {
  if (key.startsWith('jifa.')) {
    return i18n.global.t(key, args);
  }
  return i18n.global.t('jifa.' + key, args);
}

export function gct(key: string, args?: any) {
  return i18n.global.t('jifa.gclog.' + key, args);
}
