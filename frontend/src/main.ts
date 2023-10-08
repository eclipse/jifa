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
import App from '@/App.vue';
import { i18n } from '@/i18n/i18n';
import router from '@/router';
import { useEnv } from '@/stores/env';
import { createPinia } from 'pinia';
import VContextmenu from 'v-contextmenu';

// css
import '@/styles/index.scss';
import 'element-plus/dist/index.css';
import 'v-contextmenu/dist/themes/default.css';

const app = createApp(App);
app.use(createPinia());
app.use(i18n);
app.use(router);
app.use(VContextmenu);
app.config.globalProperties.window = window;
useEnv().init();
app.mount('#app');
