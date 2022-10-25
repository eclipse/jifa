/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
import Vue from 'vue'
import VueI18n from 'vue-i18n'
import messages from './messages'

import ElementLocale from 'element-ui/lib/locale'

Vue.use(VueI18n)

const i18n = new VueI18n({
  fallbackLocale: 'en',
  messages
})

ElementLocale.i18n((key, value) => i18n.t(key, value))

export default i18n

export const supportLanguages = [{
  label: 'English',
  value: 'en'
}, {
  label: '中文',
  value: 'cn'
},]
