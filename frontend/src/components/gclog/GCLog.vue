<!--
    Copyright (c) 2022 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<!--suppress HtmlUnknownTag -->
<template>
  <el-container>
    <el-header>
        <view-menu subject="analysisResult"
                   :file="file"
                   :analysisState="analysisState"
                   :type="type"/>
    </el-header>

    <el-main style="padding-top: 0">

      <div style="padding-top: 20px" v-if="analysisState === 'IN_PROGRESS' || analysisState === 'ERROR'">
        <b-progress height="2rem" show-progress :precision="2"
                    :value="progress"
                    :variant="progressState"
                    striped
                    :animated="progress < 100"></b-progress>
        <b-card class="mt-3" bg-variant="dark" text-variant="white" v-if="message">
          <b-card-text style="white-space: pre-line;">{{message}}</b-card-text>
          <div class="d-flex justify-content-center mb-3" v-if="progressState === 'info'">
            <b-spinner></b-spinner>
          </div>
        </b-card>
      </div>

      <div v-if="analysisState === 'SUCCESS'">
        <button
            style="right: 5%;top: 50%; position: fixed"
            @click="showDetail">
          显示GC详情
        </button>
        <el-drawer
            title="GC详情"
            size="90%"
            :visible.sync="detailVisible"
            direction="rtl">
          <GCDetail :file="file"/>
        </el-drawer>
        <el-main style="padding: 10px 5px 5px;">
          //todo  main
        </el-main>
      </div>
    </el-main>
  </el-container>
</template>

<script>
  import axios from 'axios'
  import {gclogService} from '../../util'
  import ViewMenu from "../menu/ViewMenu";
  import GCDetail from "@/components/gclog/GCDetail";

  export default {
    props: ['file'],
    data() {
      return {
        type: 'GC_LOG',
        analysisState: 'NOT_STARTED',
        progressState: 'info',
        message: '',
        progress: 0,
        pollingInternal: 500,

        detailVisible: false,
      }
    },

    components: {
      GCDetail,
      ViewMenu,
    },
    methods: {
      pollProgressOfAnalysis() {
        let self = this;
        if (!self || self._isDestroyed) {
          return
        }
        axios.get(gclogService(this.file, 'progressOfAnalysis')).then(resp => {
          let state = resp.data.state;
          let percent = resp.data.percent;
          if (resp.data.message) {
            this.message = resp.data.message.replace(/\\n/gm, "<b/>")
          }
          if (state === 'IN_PROGRESS') {
            if (percent >= 1) {
              this.progress = 99
            } else {
              this.progress = percent * 100
            }
            setTimeout(this.pollProgressOfAnalysis, this.pollingInternal)
          } else if (state === 'SUCCESS') {
            this.progress = 100;
            this.progressState = 'success';
            this.$notify({
              title: this.$t("jifa.goToOverViewPrompt"),
              position: 'top-right',
              type: "success",
              offset: 200, // changed
              duration: 1000,
              showClose: true,
              onClose: () => {
                this.analysisState = "SUCCESS"
              }
            })
          } else {
            this.progressState = 'danger';
            this.analysisState = "ERROR";
            axios.post(gclogService(this.file, 'release'))
          }
        })
      },
      analyzeGCLog(params) {
        axios.post(gclogService(this.file, 'analyze'), new URLSearchParams(params)).then(() => {
          this.analysisState = "IN_PROGRESS";
          this.pollProgressOfAnalysis();
        })
      },
      showDetail() {
        this.detailVisible = true;
      }
    },
    mounted() {
      this.analyzeGCLog();
    }
  }
</script>
