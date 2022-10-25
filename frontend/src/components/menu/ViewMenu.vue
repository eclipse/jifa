<!--
    Copyright (c) 2020 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<template>
  <b-navbar type="light" variant="faded" style="height: 100%; border-bottom: 1px solid #dcdfe6; font-size: 14px">
    <b-navbar-nav>
      <b-navbar-brand href="" to="/" v-if="$jifa.fileManagement">
        <i class="el-icon-s-platform"/>
        J I F A
      </b-navbar-brand>
      <b-navbar-brand v-if="!$jifa.fileManagement">
        <i class="el-icon-s-platform"/>
        J I F A
      </b-navbar-brand>
    </b-navbar-nav>
    <finder-menu v-if="subject==='finder'" v-bind="$attrs" v-on="$listeners"/>
    <analysis-result-menu v-else-if="subject==='analysisResult'" v-bind="$attrs" v-on="$listeners"/>

    <b-navbar-nav v-if="subject==='finder'" class="ml-auto">
      <Feedback/>
      <b-nav-item-dropdown right>
        <template v-slot:button-content>{{currentLanguage}}</template>
        <b-dropdown-item href="#" v-for="lang in supportLanguages" :key="lang.label"
                         @click="$jifa.set_locale($i18n, lang.value); currentLanguage=lang.label">
          {{lang.label}}
        </b-dropdown-item>
      </b-nav-item-dropdown>
    </b-navbar-nav>
  </b-navbar>
</template>

<script>
  import FinderMenu from "./FinderMenu"
  import AnalysisResultMenu from "./AnalysisResultMenu"
  import Feedback from "./Feedback";

  import {supportLanguages} from '../../i18n/i18n-setup'


  export default {
    props: ['subject'],

    components: {
      FinderMenu,
      AnalysisResultMenu,
      Feedback,
    },

    methods: {
      getCurrentLanguageLabel() {
        for (let i = 0; i < supportLanguages.length; i++) {
          let lang = supportLanguages[i]
          if (lang.value === this.$i18n.locale) {
            return lang.label
          }
        }
      }
    },

    data() {
      return {
        supportLanguages: supportLanguages,
        currentLanguage: this.getCurrentLanguageLabel(),
      }
    },
  }
</script>

<style scoped>
  .navbar-light .navbar-nav .nav-link.active {
    color: #1989FA;
  }
</style>
