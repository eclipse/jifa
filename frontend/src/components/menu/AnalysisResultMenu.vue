<!--
    Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<template>
  <b-navbar-nav>
    <b-nav-item href="#" @click="doReanalyze" v-if="$jifa.fileManagement && (analysisState === 'SUCCESS' || analysisState === 'ERROR')">
      <i class="el-icon-warning-outline" style="margin-right: 3px"/> {{$t("jifa.reanalyze")}}
    </b-nav-item>

    <b-nav-item href="#" @click="doRelease" v-if="$jifa.fileManagement && analysisState === 'SUCCESS'">
      <i class="el-icon-s-release" style="margin-right: 3px"/> {{$t("jifa.release")}}
    </b-nav-item>

    <b-nav-item :href="`/jifa-api/file/download?name=${file}&type=${type}`"
                download
                v-if="$jifa.fileManagement && showDownloadOpt">
      <i class="el-icon-download" style="margin-right: 3px"/> {{$t("jifa.download")}}
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

    <b-nav-item href="#" @click="$emit('gcLogAnalysisConfig')" v-if="analysisState === 'SUCCESS' && type === 'GC_LOG'">
      <i class="el-icon-s-tools" style="margin-right: 3px"/> {{$t("jifa.gclog.analysisConfig")}}
    </b-nav-item>

    <b-nav-item href="#" @click="$emit('gclogCompareConfig')" v-if="analysisState === 'SUCCESS' && type === 'GC_LOG'">
      <i class="el-icon-collection" style="margin-right: 3px"/> {{$t("jifa.gclog.gclogCompare")}}
    </b-nav-item>

    <b-nav-item href="#" @click="doUnlock" v-if="$jifa.fileManagement && !$jifa.workerOnly && showUnlockOpt">
      <i class="el-icon-folder-opened" style="margin-right: 3px"/> {{$t("jifa.unlockFile")}}
    </b-nav-item>
  </b-navbar-nav>
</template>
<script>
  import axios from 'axios'
  import {heapDumpService, gclogService, threadDumpService, service} from '../../util'

  export default {
    props: ['file', 'analysisState', 'type', 'showInspector'],

    data(){
      return {
        showUnlockOpt: false,
        showDownloadOpt: false,
      }
    },

    methods: {

      doReanalyze() {
        this.$confirm(this.$t('jifa.heap.reanalyzePrompt'), this.$t('jifa.prompt'), {
          confirmButtonText: this.$t('jifa.confirm'),
          cancelButtonText: this.$t('jifa.cancel'),
          type: 'warning'
        }).then(() => {
          axios.post(this.getUrlByType('clean')).then(() => {
            window.location.reload();
          })
        })
      },

      doUnlock() {
        // Okay, unlock it.
        this.$confirm(this.$t('jifa.unlockFilePrompt'), this.$t('jifa.prompt'), {
          confirmButtonText: this.$t('jifa.confirm'),
          cancelButtonText: this.$t('jifa.cancel'),
          type: 'warning'
        }).then(() => {
          let formData = new FormData()
          formData.append('name', this.file)
          axios.post(service("/file/setShared"), new URLSearchParams(formData)).then(() => {
            this.$notify({
              title: this.$t("jifa.unlockFileSuccessPrompt"),
              position: 'top-right',
              type: "success",
              offset: 300,
              duration: 1000,
              showClose: true,
              onClose: () => {
                this.analysisState = "SUCCESS";
              }
            })
            this.showUnlockOpt = false
          })
        })
      },

      doRelease() {
        this.$confirm(this.$t('jifa.heap.releasePrompt'), this.$t('jifa.prompt'), {
          confirmButtonText: this.$t('jifa.confirm'),
          cancelButtonText: this.$t('jifa.cancel'),
          type: 'warning'
        }).then(() => {
          axios.post(this.getUrlByType('release')).then(() => {
            this.$router.push({name: 'finder'})
          })
        })
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
          case "GC_LOG":
            return gclogService(this.file, uri);
          case "THREAD_DUMP":
            return threadDumpService(this.file, uri);
          default:
            return ""
        }
      }
    },
    mounted() {
      // workerOnly mode does not need this
      if (this.$jifa.workerOnly) {
        this.showDownloadOpt = true;
        return;
      }

      axios.get(service('/file'), {
        params: {
          name: this.file,
          type: this.type
        }
      }).then(resp => {
        let data = resp.data;

        // Already shared?
        if (data.shared) {
          this.showDownloadOpt = true;
          return;
        }

        // Granted accessibility?
        let fileOwnerId = data.userId;
        axios.get(service('/userInfo')).then(resp1 => {
          let userInfo = resp1.data;
          if (userInfo === undefined) {
            return;
          }
          let currentUserId = userInfo.id;
          if (userInfo.admin !== true && currentUserId !== fileOwnerId) {
            return;
          }

          // Finally...
          this.showUnlockOpt = true;
          this.showDownloadOpt = true;
        });
      })
    }
  }
</script>
<style scoped>
  .navbar-light .navbar-nav .nav-link:focus {
    color: rgba(0, 0, 0, 0.5);
  }
</style>
