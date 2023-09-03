<!--
    Copyright (c) 2022 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->

<template>
  <el-container  style="height: 100%">
      <el-container>
        <el-header>
          <view-menu subject="analysisResult"
                     :file="file"
                     :analysisState="analysisState"
                     @threadDumpCompareConfig="showCompareConfig"
                     @search="doSearch"
                     type="THREAD_DUMP"/>
        </el-header>
    
        <el-main style="padding-top: 0">
    
          <div style="padding-top: 20px" v-if="analysisState === 'IN_PROGRESS' || analysisState === 'ERROR'">
            <b-progress height="2rem" show-progress :precision="2"
                        :value="progress"
                        :variant="progressState"
                        striped
                        :animated="progress < 100"></b-progress>
            <b-card class="mt-3" bg-variant="dark" text-variant="white" v-if="message">
              <b-card-text style="white-space: pre-line;">{{ message }}</b-card-text>
              <div class="d-flex justify-content-center mb-3" v-if="progressState === 'info'">
                <b-spinner></b-spinner>
              </div>
            </b-card>
          </div>
    
          <el-dialog
            :title="$t('jifa.threadDump.threadDumpCompare')"
            width="700px"
            :visible.sync="compareConfigVisible"
            :close-on-click-modal=false
            append-to-body>
          <el-form ref="compareForm" :model="compareConfigModel" label-width="200px" size="medium"
                   label-position="right"
                   style="margin-top: 10px" status-icon :show-message=false :rules="compareRule">
            <el-form-item v-for="i in 3" :label="'Dump '+i" :prop="'file'+(i-1)" :key="i">
              <el-input type="textarea" v-model="compareConfigModel['file' + (i-1)]" autosize resize="none"
                        style="width: 400px" :placeholder="$t('jifa.threadDump.threadDumpFilePlaceholder')"/>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="doCompare()">{{ $t('jifa.confirm') }}</el-button>
            </el-form-item>
          </el-form>
        </el-dialog>


          <el-container v-if="analysisState === 'SUCCESS'" style="height: 100%">
            <el-aside width="200px">
              <div style="font-size: 16px; height: 100%;">
              <el-card :header="$t('jifa.threadDump.navigation')" style="height: 100%;">
                <div class="nav-item"><a href="#navTop">{{ $t('jifa.threadDump.navToTop') }}</a></div>
                <el-divider/>
                <div class="nav-item"><a href="#overview">{{ $t('jifa.threadDump.basicInfo') }}</a></div>
                <div class="nav-item"><a href="#diagnosis">{{ $t('jifa.threadDump.diagnosis.title') }}</a></div>
                <div class="nav-item"><a href="#blockedThreads">{{ $t('jifa.threadDump.blockedThreadsLabel') }}</a></div>
                <div class="nav-item"><a href="#cpuConsumingThreads">{{ $t('jifa.threadDump.cpuConsumingThreadsLabel') }}</a></div>
                <div class="nav-item"><a href="#threadSummary">{{ $t('jifa.threadDump.threadSummary') }}</a></div>
                <div class="nav-item"><a href="#threadGroupSummary">{{ $t('jifa.threadDump.threadGroupSummary') }}</a></div>
                <div class="nav-item"><a href="#monitors">{{ $t('jifa.threadDump.monitors') }}</a></div>
                <div class="nav-item"><a href="#callSiteTree">{{ $t('jifa.threadDump.callSiteTree') }}</a></div>
                <el-divider/>
                <div class="nav-item"><a href="#fileContent" @click="showDetail('fileContent')">{{ $t('jifa.threadDump.fileContent') }}</a></div>
              </el-card>
            </div>
           </el-aside>
            <el-main style="padding: 5px; height: 100%;">
              <main-page :file="file" style="width: 90%; left: 10%;" id="navTop"/>
            </el-main>
          </el-container>
        </el-main>
      </el-container>
  </el-container> 
</template>

<script>
import ViewMenu from "../menu/ViewMenu";
import MainPage from "./Main";
import axios from "axios";
import {threadDumpService} from "@/util";
import {getUrlParams} from "@/components/gclog/GCLogUtil";

export default {
  props: ['file'],
  components: {
    ViewMenu, MainPage
  },
  data() {
    return {
      analysisState: 'NOT_STARTED',
      progressState: 'info',
      message: '',
      progress: 0,
      pollingInternal: 500,
      compareConfigVisible: false,
      compareConfigModel: {},
      compareRule: {
          file0: [
            {required: true, trigger: 'blur'}
          ],
          file1: [
            {required: true, trigger: 'blur'}
          ],
          file2: [
            {required: false, trigger: 'blur'}
          ],
        },
    }
  },
  methods: {
    pollProgressOfAnalysis() {
      let self = this;
      if (!self || self._isDestroyed) {
        return
      }
      axios.get(threadDumpService(this.file, 'progressOfAnalysis')).then(resp => {
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
          axios.post(threadDumpService(this.file, 'release'))
        }
      })
    },
    analyzeThreadDump() {
      axios.post(threadDumpService(this.file, 'analyze')).then(() => {
        this.analysisState = "IN_PROGRESS";
        this.pollProgressOfAnalysis();
      })
    },
    showCompareConfig() {
        this.compareConfigModel = {
          file0: this.file,
          file1: null,
          file2: null,
        }
        this.compareConfigVisible = true
      },
      doCompare() {
        this.$refs['compareForm'].validate((valid) => {
          if (valid) {
            const query = {
              file: []
            }
            for (let i = 0; i <= 2; i++) {
              const file = this.compareConfigModel["file" + i]
              if(file==null || file=='') {
                continue
              }
              if (file.indexOf('/threadDump?') >= 0 && file.indexOf('file=') >= 0) {
                const params = getUrlParams(file);
                query.file.push(params.file)
              } else {
                query.file.push(file)
              }
            }
            const url = this.$router.resolve({
              name: 'threadDumpCompare',
              query: query
            })
            window.open(url.href)
            this.compareConfigVisible = false
          }
        })
      },
      doSearch(searchText) {
        const query = {
          file: [this.file],
          term: searchText,
        }
        const url = this.$router.resolve({
          name: 'threadDumpSearch',
          query: query
        })
        window.open(url.href)
      },
  },
  mounted() {
    this.analyzeThreadDump();
  }
}
</script>
