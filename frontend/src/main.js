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

import Jifa from './Jifa.vue'
import JifaGlobal, {USER} from './Jifa'

import i18n from './i18n/i18n-setup'
import router from './router'
import config from './config'

import ElementUI from 'element-ui'
import BootstrapVue from 'bootstrap-vue'
import VueCharts from 'vue-chartjs'
import VueClipboard from 'vue-clipboard2'
import contentmenu from 'v-contextmenu'

import VueCookies from 'vue-cookies'

import 'v-contextmenu/dist/index.css'
import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'
import 'element-ui/lib/theme-chalk/index.css'
// put our css file in the end
import './Jifa.css'

Vue.use(ElementUI)
Vue.use(BootstrapVue)
Vue.use(VueCharts)
Vue.use(VueClipboard)
Vue.use(contentmenu)
Vue.use(VueCookies)
Vue.prototype.$jifa = Object.assign(JifaGlobal, config)
Vue.prototype.$jifa_user = USER

export default new Vue({
  router,
  i18n,
  render: h => h(Jifa),
  created() {
    this.$jifa.init_authorization_header()
    this.$jifa.init_locale(this.$i18n, 'en')
    // eslint-disable-next-line no-console
    console.log(this.$t("jifa.consoleMsg"))
  }
}).$mount('#app')