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
                   @gclogCompareConfig="showCompareConfig"
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
            :visible.sync="analysisConfigVisible"
            :close-on-click-modal=false
            append-to-body>
          <el-form ref="configForm" :model="analysisConfigModel" :rules="analysisConfigRules" label-width="200px" size="medium" label-position="right"
                   style="margin-top: 10px" status-icon :show-message=false>
            <el-form-item :label="$t('jifa.gclog.analysisTimeRange')" prop="timeRange">
              <GCLogTimePicker v-model="analysisConfigModel.timeRange" :metadata="metadata"/>
            </el-form-item>
            <el-form-item :label="$t('jifa.gclog.longPauseThreshold')" prop="longPauseThreshold">
              <el-input-number v-model="analysisConfigModel.longPauseThreshold" :min="0" :controls="false" style="width: 30%"/>
            </el-form-item>
            <el-form-item v-if="metadata.generational" :label="$t('jifa.gclog.youngGCFrequentIntervalThreshold')" prop="youngGCFrequentIntervalThreshold">
              <el-input-number v-model="analysisConfigModel.youngGCFrequentIntervalThreshold" :min="0" :controls="false" style="width: 30%"/>
            </el-form-item>
            <el-form-item v-if="metadata.generational" :label="$t('jifa.gclog.oldGCFrequentIntervalThreshold')" prop="oldGCFrequentIntervalThreshold">
              <el-input-number v-model="analysisConfigModel.oldGCFrequentIntervalThreshold" :min="0" :controls="false" style="width: 30%"/>
            </el-form-item>
            <el-form-item :label="$t('jifa.gclog.fullGCFrequentIntervalThreshold')" prop="fullGCFrequentIntervalThreshold">
              <el-input-number v-model="analysisConfigModel.fullGCFrequentIntervalThreshold" :min="0" :controls="false" style="width: 30%"/>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="configChangeConfirm">{{$t('jifa.confirm')}}</el-button>
            </el-form-item>
          </el-form>
        </el-dialog>

        <el-dialog
            :title="$t('jifa.gclog.gclogCompare')"
            width="700px"
            :visible.sync="compareConfigVisible"
            :close-on-click-modal=false
            append-to-body>
          <el-form ref="compareForm" :model="compareConfigModel" label-width="200px" size="medium"
                   label-position="right"
                   style="margin-top: 10px" status-icon :show-message=false :rules="compareRule">
            <el-form-item v-for="i in 2" :label="$t('jifa.gclog.gclogFile') + i" :prop="'file'+i" :key="i">
              <el-input type="textarea" v-model="compareConfigModel['file' + (i-1)]" autosize resize="none"
                        style="width: 400px" :placeholder="$t('jifa.gclog.gclogFilePlaceholder')"/>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="doCompare()">{{ $t('jifa.confirm') }}</el-button>
            </el-form-item>
          </el-form>
        </el-dialog>

        <el-main style="padding: 10px 5px 5px; width: 1000px; margin: 0 auto">
          <GCPause :file="file" :metadata="metadata" :timeRange="analysisConfig.timeRange" :longPauseThreshold="analysisConfig.longPauseThreshold"/>
          <GCMemoryStats :file="file" :metadata="metadata" :analysisConfig="analysisConfig"/>
          <GCPhaseStats :file="file" :metadata="metadata" :analysisConfig="analysisConfig"/>
          <VmOptions :file="file" :metadata="metadata"/>
          <GCObjectStats :file="file" :metadata="metadata" :timeRange="analysisConfig.timeRange"/>

          <!--    for debug -->
          analysisconfig: <div>{{this.analysisConfig}}</div>
          compareConfig: <div>{{this.compareConfigModel}}</div>
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
  import GCPhaseStats from "@/components/gclog/GCPhaseStats";
  import VmOptions from "@/components/gclog/VmOptions";

  export default {
    props: ['file', 'start', 'end'],
    data() {
      return {
        type: 'GC_LOG',
        analysisState: 'NOT_STARTED',
        progressState: 'info',
        message: '',
        progress: 0,
        pollingInternal: 500,

        analysisConfigVisible: false,
        analysisConfigRules: {
          timeRange: [], // Do not validate it. The component will guarantee it is always valid.
          longPauseThreshold: [
            {required: true, trigger: 'blur'}
          ],
          youngGCFrequentIntervalThreshold: [
            {required: true, trigger: 'blur'}
          ],
          oldGCFrequentIntervalThreshold: [
            {required: true, trigger: 'blur'}
          ],
          fullGCFrequentIntervalThreshold: [
            {required: true, trigger: 'blur'}
          ],
        },
        analysisConfigModel: {
          timeRange: 0,
        },
        analysisConfig: null,

        compareConfigVisible: false,
        compareConfigModel: {
          file0: null,
          file1: null,
        },
        compareRule: {
          file0: [
            {required: true, trigger: 'blur'}
          ],
          file1: [
            {required: true, trigger: 'blur'}
          ],
        },

        metadata: null,
        detailVisible: false,
      }
    },

    components: {
      GCDetail,
      GCObjectStats,
      GCMemoryStats,
      GCPhaseStats,
      GCPause,
      VmOptions,
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
        this.analysisConfigModel = {
          timeRange: {
            start: typeof this.start !== 'undefined' ? Math.max(this.metadata.startTime, parseInt(this.start)) : this.metadata.startTime,
            end: typeof this.end !== 'undefined' ? Math.min(this.metadata.endTime, parseInt(this.end)) : this.metadata.endTime,
          },
          longPauseThreshold : this.metadata.pauseless ? 30 : 400,
          longConcurrentThreshold : 30000,
          youngGCFrequentIntervalThreshold: 1000,
          oldGCFrequentIntervalThreshold: 15000,
          fullGCFrequentIntervalThreshold: this.metadata.generational ? 60000 : 1000,
          highOldUsageThreshold: 80,
          highHeapUsageThreshold: 60,
          highMetaspaceUsageThreshold: 80,
        }
        this.analysisConfig = {...this.analysisConfigModel}
        this.updateUrl()
      },
      analyzeGCLog(params) {
        axios.post(gclogService(this.file, 'analyze'), new URLSearchParams(params)).then(() => {
          this.analysisState = "IN_PROGRESS";
          this.pollProgressOfAnalysis();
        })
      },
      updateUrl() {
        this.$router.push({
          query: {
            file: this.file,
            start: this.analysisConfig.timeRange.start,
            end: this.analysisConfig.timeRange.end
          }
        })
      },
      configChangeConfirm() {
        this.$refs['configForm'].validate((valid) => {
          if (valid) {
            this.analysisConfigVisible = false;
            this.analysisConfig = {...this.analysisConfigModel}
            this.updateUrl()
          }
        })
      },
      showDetail() {
        this.detailVisible = true;
      },
      showCompareConfig() {
        this.compareConfigModel = {
          file0: window.location.href,
          file1: null,
        }
        this.compareConfigVisible = true
      },
      showAnalysisConfig() {
        this.analysisConfigVisible = true;
      },
      getUrlParams(url) {
        let params = {}
        const question = url.indexOf('?')
        if (question >= 0) {
          let str = url.substr(question + 1);
          str.split("&").forEach(paramString => {
            const items = paramString.split("=");
            params[decodeURIComponent(items[0])] = decodeURIComponent(items[1]);
          })
        }
        return params;
      },
      doCompare() {
        this.$refs['compareForm'].validate((valid) => {
          if (valid) {
            const query = {}
            for (let i = 0; i <= 1; i++) {
              const file = this.compareConfigModel["file" + i]
              if (file.indexOf('/gcLog?') >= 0 && file.indexOf('file=') >= 0) {
                const params = this.getUrlParams(file);
                query["file" + i] = params.file
                query["start" + i] = params.start
                query["end" + i] = params.end
              } else {
                query["file" + i] = file
              }
            }
            const url = this.$router.resolve({
              name: 'gcLogCompare',
              query: query
            })
            window.open(url.href)
            this.compareConfigVisible = false
          }
        })
      }
    },
    mounted() {
      this.analyzeGCLog();
    }
  }
</script>

<style lang="scss" scoped>
.el-drawer.rtl {
  overflow: scroll
}
</style>

<style>
.bad-metric {
  color: #E74C3C;
}
</style>
