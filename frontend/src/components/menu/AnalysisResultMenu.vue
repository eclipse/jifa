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
  <b-navbar-nav>
    <b-nav-item href="#" @click="doReanalyze" v-if="$jifa.fileManagement() && (analysisState === 'SUCCESS' || analysisState === 'ERROR')">
      <i class="el-icon-warning-outline" style="margin-right: 3px"/> {{$t("jifa.reanalyze")}}
    </b-nav-item>

    <b-nav-item href="#" @click="doRelease" v-if="$jifa.fileManagement() && analysisState === 'SUCCESS'">
      <i class="el-icon-s-release" style="margin-right: 3px"/> {{$t("jifa.release")}}
    </b-nav-item>

    <b-nav-item-dropdown right v-if="analysisState === 'SUCCESS' && type === 'HEAP_DUMP'">
      <template v-slot:button-content>
        <i class="el-icon-setting" style="margin-right: 3px"/> {{$t("jifa.setting")}}
      </template>
      <b-dropdown-item @click="triggerInspector" style="font-size: 14px">
        {{showInspector ? $t("jifa.hide") : $t("jifa.show")}} Inspector
      </b-dropdown-item>
      <b-dropdown-item @click="$emit('expandResultDivWidth')" style="font-size: 14px">
        {{$t("jifa.expandResultDivWidth")}}
      </b-dropdown-item>
      <b-dropdown-item @click="$emit('shrinkResultDivWidth')" style="font-size: 14px">
        {{$t("jifa.shrinkResultDivWidth")}}
      </b-dropdown-item>
      <b-dropdown-item @click="$emit('resetResultDivWidth')" style="font-size: 14px">
        {{$t("jifa.resetResultDivWidth")}}
      </b-dropdown-item>
    </b-nav-item-dropdown>
  </b-navbar-nav>
</template>
<script>
  import axios from 'axios'
  import {heapDumpService} from '../../util'

  export default {
    props: ['file', 'analysisState', 'type', 'showInspector'],

    methods: {

      doReanalyze() {
        if (this.$jifa.fileManagement()) {
          this.$confirm(this.$t('jifa.heap.reanalyzePrompt'), this.$t('jifa.prompt'), {
            confirmButtonText: this.$t('jifa.confirm'),
            cancelButtonText: this.$t('jifa.cancel'),
            type: 'warning'
          }).then(() => {
            axios.post(this.getUrlByType('clean')).then(() => {
              window.location.reload();
            })
          })
        }
      },

      doRelease() {
        if (this.$jifa.fileManagement()) {
          this.$confirm(this.$t('jifa.heap.releasePrompt'), this.$t('jifa.prompt'), {
            confirmButtonText: this.$t('jifa.confirm'),
            cancelButtonText: this.$t('jifa.cancel'),
            type: 'warning'
          }).then(() => {
            axios.post(this.getUrlByType('release')).then(() => {
              this.$router.push({name: 'finder'})
            })
          })
        }
      },

      triggerInspector() {
        if (this.type === "HEAP_DUMP") {
          this.$emit('setShowInspector', !this.showInspector)
        }
      },

      getUrlByType(uri) {
        switch (this.type) {
          case "HEAP_DUMP":
            return heapDumpService(this.file, uri);
          default:
            return ""
        }
      }
    }
  }
</script>
<style scoped>
  .navbar-light .navbar-nav .nav-link:focus {
    color: rgba(0, 0, 0, 0.5);
  }
</style>
