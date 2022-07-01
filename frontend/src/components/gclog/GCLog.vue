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
                   @gcLogAnalysisConfig="showAnalysisConfig"
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
            style="right: 5%;top: 50%; position: fixed; z-index: 100"
            @click="showDetail">
          {{$t('jifa.gclog.showGCDetail')}}
        </button>
        <el-drawer
            title="GC详情"
            size="90%"
            :visible.sync="detailVisible"
            direction="rtl">
          <GCDetail :file="file"/>
        </el-drawer>
        <el-dialog
            :title="$t('jifa.gclog.analysisConfig')"
            width="700px"
            :visible.sync="configVisible"
            :close-on-click-modal=false
            append-to-body>
          <el-form ref="configForm" :model="configModel" :rules="configRules" label-width="150px" size="medium" label-position="right"
                   style="margin-top: 10px" status-icon :show-message=false>
            <el-form-item :label="$t('jifa.gclog.logTimeRange')" prop="timeRange">
              <GCLogTimePicker v-model="configModel.timeRange" :metadata="metadata"/>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="configChangeConfirm">{{$t('jifa.confirm')}}</el-button>
            </el-form-item>
          </el-form>
        </el-dialog>

        <el-main style="padding: 10px 5px 5px;">
<!--           todo: adjust style-->
          <GCObjectStats :file="file" :metadata="metadata" :timeRange="analysisConfig.timeRange"/>
          <GCMemoryStats :file="file" :metadata="metadata" :timeRange="analysisConfig.timeRange"/>
          <GCPause :file="file" :metadata="metadata" :timeRange="analysisConfig.timeRange" :longPauseThreshold="analysisConfig.longPauseThreshold"/>
          <!--    for debug -->
          analysisconfig: <div>{{this.analysisConfig}}</div>
          metadata: <div>{{this.metadata}}</div>
          <!--    for debug    -->
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
  import GCLogTimePicker from "@/components/gclog/GCLogTimePicker";
  import GCObjectStats from "@/components/gclog/GCObjectStats";
  import GCMemoryStats from "@/components/gclog/GCMemoryStats";
  import GCPause from "@/components/gclog/GCPause";

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

        configVisible: false,
        configRules: {
          timeRange: [], // Do not validate it. The component will guarantee it is always valid.
        },
        configModel: {
          timeRange: 0,
        },
        analysisConfig: null,

        metadata: null,
        detailVisible: false,
      }
    },

    components: {
      GCDetail,
      GCObjectStats,
      GCMemoryStats,
      GCPause,
      ViewMenu,
      GCLogTimePicker,
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
            this.progress = 99;
            this.message += 'loading gclog metadata\n'
            axios.get(gclogService(this.file, 'metadata')).then(resp => {
              this.metadata = resp.data;
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
                  this.initializePage()
                  this.analysisState = "SUCCESS"
                }
              })
            })
          } else {
            this.progressState = 'danger';
            this.analysisState = "ERROR";
            axios.post(gclogService(this.file, 'release'))
          }
        })
      },
      initializePage() {
        this.configModel = {
          timeRange: {
            start: this.metadata.startTime,
            end: this.metadata.endTime,
            longPauseThreshold : this.metadata.pauseless ? 50 : 500
          }
        }
        this.analysisConfig = {...this.configModel}
      },
      analyzeGCLog(params) {
        axios.post(gclogService(this.file, 'analyze'), new URLSearchParams(params)).then(() => {
          this.analysisState = "IN_PROGRESS";
          this.pollProgressOfAnalysis();
        })
      },
      configChangeConfirm() {
        this.configVisible = false;
        this.analysisConfig = {...this.configModel}
      },
      showDetail() {
        this.detailVisible = true;
      },
      showAnalysisConfig() {
        this.configVisible = true;
      }
    },
    mounted() {
      this.analyzeGCLog();
    }
  }
</script>

<style lang="scss">
.el-drawer.rtl {
  overflow: scroll
}
</style>
