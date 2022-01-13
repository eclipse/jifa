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

      <el-container v-if="analysisState === 'SUCCESS'">
        <el-main style="padding: 10px 5px 5px;">
          <el-tabs v-model="activeTab">

            <el-tab-pane name="gcOverview" :lazy="true">
              <span slot="label">{{$t('jifa.gclog.overview')}}</span>
              <GCOverview :file="file"/>
            </el-tab-pane>

            <el-tab-pane name="monitor" :lazy="true">
              <span slot="label">{{$t('jifa.gclog.monitor')}}</span>
              <Monitor :file="file"/>
            </el-tab-pane>

            <el-tab-pane name="gcCauseAndPhase" :lazy="true">
              <span slot="label">{{$t('jifa.gclog.gcCauseAndPhase')}}</span>
              <GCCause :file="file"/>
              <GCPhase :file="file"/>
            </el-tab-pane>

            <el-tab-pane name="gcDetail" :lazy="true">
              <span slot="label">{{$t('jifa.gclog.gcDetail')}}</span>
              <GGCDetail :file="file"/>
            </el-tab-pane>

          </el-tabs>
        </el-main>
      </el-container>
    </el-main>
  </el-container>
</template>

<script>
  import axios from 'axios'
  import {gclogService} from '../../util'
  import ViewMenu from "../menu/ViewMenu";
  import GCOverview from "./GCOverview";
  import GCCause from "./GCCause";
  import GCPhase from "./GCPhase";
  import Monitor from "./Monitor";
  import GGCDetail from "./GCDetail";

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
        activeTab: 'gcOverview',
      }
    },

    components: {
      GCOverview,
      GCCause,
      GCPhase,
      ViewMenu,
      Monitor,
      GGCDetail,
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
      }
    },
    mounted() {
      this.analyzeGCLog();
    }
  }
</script>
