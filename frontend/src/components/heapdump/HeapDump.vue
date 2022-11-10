<!--
    Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<!--suppress HtmlUnknownTag -->
<template>
  <el-container style="height: 100%">
    <el-header>
      <view-menu subject="analysisResult"
                 :file="file" :analysisState="analysisState" :type="type" :showInspector="showInspector"
                 @setShowInspector="setShowInspector"
                 @expandResultDivWidth="expandResultDivWidth"
                 @shrinkResultDivWidth="shrinkResultDivWidth"
                 @resetResultDivWidth="resetResultDivWidth"/>
    </el-header>

    <el-main style="padding-top: 0; padding-bottom: 0; height: 100%">

      <el-dialog
              :title="$t('jifa.options')"
              width="20%"
              :visible.sync="optionViewVisible"
              :close-on-press-escape=false :close-on-click-modal=false :show-close=false
              append-to-body
              modal>

        <el-form label-position="top" size="mini">
          <el-popover
              placement="left"
              trigger="hover"
              width="500">
            <el-alert
                type="info"
                style="word-break: keep-all"
                :closable="false"
                :description="$t('jifa.heap.descOfKeepUnreachableObjects')">
            </el-alert>
            <el-form-item label="Keep unreachable objects" slot="reference">
              <el-switch v-model="options.keepUnreachableObjects"></el-switch>
            </el-form-item>
          </el-popover>

          <el-popover
              placement="left"
              trigger="hover"
              width="500">
            <el-alert
                type="info"
                style="word-break: keep-all"
                :closable="false"
                :title="$t('jifa.heap.descOfStrictness')">
              <div slot="default">
                <br/>
                <span>stop - {{ $t('jifa.heap.descOfStopStrictness') }}</span>
                <br/>
                <br/>
                <span>warn - {{ $t('jifa.heap.descOfWarnStrictness') }}</span>
                <br/>
                <br/>
                <span>permissive - {{ $t('jifa.heap.descOfPermissiveStrictness') }}</span>
              </div>
            </el-alert>
            <el-form-item label="Strictness" slot="reference">
              <el-radio-group v-model="options.strictness">
                <el-radio label="stop">stop</el-radio>
                <el-radio label="warn">warn</el-radio>
                <el-radio label="permissive">permissive</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-popover>
        </el-form>

        <span slot="footer" class="dialog-footer">
          <el-button @click="analyzeHeapDump" round>{{$t('jifa.confirm')}}</el-button>
        </span>
      </el-dialog>

      <div style="padding-top: 20px; height: 100%; display: flex; flex-direction: column" v-if="analysisState === 'IN_PROGRESS' || analysisState === 'ERROR'">
        <div>
          <b-progress height="2rem" show-progress :precision="2"
                      :value="progress"
                      :variant="progressState"
                      striped
                      :animated="progress < 100"/>
        </div>
        <div style="flex-grow: 1; overflow: auto; margin-top: 20px">
          <b-card bg-variant="dark" text-variant="white" v-if="message">
            <b-card-text style="white-space: pre-line;">{{ message }}</b-card-text>
            <div class="d-flex justify-content-center mb-3" v-if="progressState === 'info'">
              <b-spinner/>
            </div>
          </b-card>
        </div>
      </div>

      <el-container v-if="analysisState === 'SUCCESS'" style="height: 100%">
        <el-main style="padding: 5px; height: 100%">
          <el-row :gutter="5" style="height: 100%">
            <el-col :span="showInspector ? 19 : 24" style="height: 100%">
              <el-tabs class="mainTabs" v-model="activeTab" tab-position="left" :before-leave="switchTab">
                <el-tab-pane name="overview">
                  <span slot="label">{{$t('jifa.heap.overview')}}</span>
                  <overview :file="file"
                            @setGenerationInfoAvailable="setGenerationInfoAvailable"
                            @outgoingRefsOfObj="outgoingRefsOfObj"
                            @incomingRefsOfObj="incomingRefsOfObj"
                            @outgoingRefsOfClass="outgoingRefsOfClass"
                            @incomingRefsOfClass="incomingRefsOfClass"
                            @pathToGCRootsOfObj="pathToGCRootsOfObj"
                            @setSelectedObjectId="setSelectedObjectId"
                  />
                </el-tab-pane>

                <el-tab-pane name="leakSuspects" lazy>
                  <span slot="label">{{$t('jifa.heap.leakSuspects')}}</span>
                  <leak-suspects :file="file"
                                 @outgoingRefsOfObj="outgoingRefsOfObj"
                                 @incomingRefsOfObj="incomingRefsOfObj"
                                 @setSelectedObjectId="setSelectedObjectId"/>
                </el-tab-pane>

                <el-tab-pane name="GCRoots" lazy>
                  <span slot="label">{{$t('jifa.heap.GCRoots')}}</span>
                  <div v-bind:style="{ 'height': '100%', 'width': resultDivWidth}">
                    <GCRoots :file="file"
                             @outgoingRefsOfObj="outgoingRefsOfObj"
                             @incomingRefsOfObj="incomingRefsOfObj"
                             @outgoingRefsOfClass="outgoingRefsOfClass"
                             @incomingRefsOfClass="incomingRefsOfClass"
                             @pathToGCRootsOfObj="pathToGCRootsOfObj"
                             @setSelectedObjectId="setSelectedObjectId"/>
                  </div>
                </el-tab-pane>

                <el-tab-pane name="dominatorTree" lazy>
                  <span slot="label"> {{$t('jifa.heap.dominatorTree')}}</span>
                  <div v-bind:style="{ 'height': '100%', 'width': resultDivWidth}">
                    <dominator-tree :file="file"
                                    @outgoingRefsOfObj="outgoingRefsOfObj"
                                    @incomingRefsOfObj="incomingRefsOfObj"
                                    @outgoingRefsOfClass="outgoingRefsOfClass"
                                    @incomingRefsOfClass="incomingRefsOfClass"
                                    @pathToGCRootsOfObj="pathToGCRootsOfObj"
                                    @mergePathToGCRootsFromDominatorTree="mergePathToGCRootsFromDominatorTree"
                                    @setSelectedObjectId="setSelectedObjectId"/>
                  </div>
                </el-tab-pane>

                <el-tab-pane name="histogram" lazy>
                  <span slot="label"> {{$t('jifa.heap.histogram')}}</span>
                  <div v-bind:style="{ 'height': '100%', 'width': resultDivWidth}">
                    <histogram :file="file" :generationInfoAvailable="generationInfoAvailable"
                               @outgoingRefsOfObj="outgoingRefsOfObj"
                               @incomingRefsOfObj="incomingRefsOfObj"
                               @outgoingRefsOfHistogramObjs="outgoingRefsOfHistogramObj"
                               @incomingRefsOfHistogramObjs="incomingRefsOfHistogramObj"
                               @outgoingRefsOfClass="outgoingRefsOfClass"
                               @incomingRefsOfClass="incomingRefsOfClass"
                               @pathToGCRootsOfObj="pathToGCRootsOfObj"
                               @mergePathToGCRootsFromHistogram="mergePathToGCRootsFromHistogram"
                               @setSelectedObjectId="setSelectedObjectId"/>
                  </div>
                </el-tab-pane>

                <el-tab-pane name="unreachableObjects" lazy>
                  <span slot="label"> {{$t('jifa.heap.unreachableObjects')}}</span>
                  <div v-bind:style="{ 'height': '100%', 'width': resultDivWidth}">
                    <unreachable-objects :file="file"
                                         @setSelectedObjectId="setSelectedObjectId"/>
                  </div>
                </el-tab-pane>


                <el-tab-pane name="duplicatedClasses" lazy>
                  <span slot="label"> {{$t('jifa.heap.duplicatedClasses')}}</span>
                  <div v-bind:style="{ 'height': '100%', 'width': resultDivWidth}">
                    <duplicated-classes :file="file"
                                        @setSelectedObjectId="setSelectedObjectId"/>
                  </div>
                </el-tab-pane>

                <el-tab-pane name="classLoaders" lazy>
                  <span slot="label"> {{$t('jifa.heap.classLoaders')}}</span>
                  <div v-bind:style="{ 'height': '100%', 'width': resultDivWidth}">
                    <class-loaders :file="file"
                                   @setSelectedObjectId="setSelectedObjectId"
                                   @outgoingRefsOfObj="outgoingRefsOfObj"
                                   @incomingRefsOfObj="incomingRefsOfObj"
                                   @outgoingRefsOfClass="outgoingRefsOfClass"
                                   @incomingRefsOfClass="incomingRefsOfClass"
                                   @pathToGCRootsOfObj="pathToGCRootsOfObj"/>
                  </div>
                </el-tab-pane>

                <el-tab-pane name="directByteBuffer" lazy>
                  <span slot="label"> {{$t('jifa.heap.directByteBuffer')}}</span>
                  <div v-bind:style="{ 'height': '100%', 'width': resultDivWidth}">
                    <direct-byte-buffer :file="file"
                                        @setSelectedObjectId="setSelectedObjectId"
                                        @pathToGCRootsOfObj="pathToGCRootsOfObj"/>
                  </div>
                </el-tab-pane>

                <el-tab-pane name="systemProperty" lazy>
                  <span slot="label"> {{$t('jifa.heap.systemProperty')}}</span>
                  <system-property :file="file"/>
                </el-tab-pane>

                <el-tab-pane name="thread" lazy>
                  <span slot="label"> {{$t('jifa.heap.threadInfo')}}</span>
                  <div v-bind:style="{ 'height': '100%', 'width': resultDivWidth}">
                    <thread :file="file" @setSelectedObjectId="setSelectedObjectId"/>
                  </div>
                </el-tab-pane>

                <el-tab-pane name="queryEngine" lazy>
                  <span slot="label"> Query (OQL/Calcite) </span>
                  <div v-bind:style="{ 'height': '100%', 'width': resultDivWidth}">
                    <Query :file="file"
                         queryType="oql"
                         @outgoingRefsOfObj="outgoingRefsOfObj"
                         @incomingRefsOfObj="incomingRefsOfObj"
                         @outgoingRefsOfClass="outgoingRefsOfClass"
                         @incomingRefsOfClass="incomingRefsOfClass"
                         @pathToGCRootsOfObj="pathToGCRootsOfObj"
                         @setSelectedObjectId="setSelectedObjectId"/>
                  </div>
                </el-tab-pane>

                <el-tab-pane name="HeapFileCompare" lazy v-if="$jifa.dev()">
                  <span slot="label">{{ $t("jifa.heap.compare") }}</span>
                  <heap-file-compare :file="file"/>
                </el-tab-pane>

                <el-tab-pane name="dynamicResultSlot" :disabled="!showDynamicResultSlot" class="dynamicTab">
                  <span slot="label"> <i class="el-icon-more-outline"/> </span>
                  <div v-bind:style="{ 'height': '100%', 'width': resultDivWidth}">
                    <dynamic-result-slot ref="dynamicResultSlot" :file="file"
                                         @disableShowDynamicResultSlot="disableShowDynamicResultSlot"
                                         @setSelectedObjectId="setSelectedObjectId"
                                         @outgoingRefsOfObj="outgoingRefsOfObj"
                                         @incomingRefsOfObj="incomingRefsOfObj"
                                         @outgoingRefsOfClass="outgoingRefsOfClass"
                                         @incomingRefsOfClass="incomingRefsOfClass"
                                         @pathToGCRootsOfObj="pathToGCRootsOfObj"
                                         @mergePathToGCRootsFromHistogram="mergePathToGCRootsFromHistogram"/>
                  </div>
                </el-tab-pane>
              </el-tabs>
            </el-col>

            <el-col :span="showInspector ? 5 : 0" style="height: 100%">
              <Inspector :file="file"
                         :objectId="selectedObjectId"
                         @outgoingRefsOfObj="outgoingRefsOfObj"
                         @setSelectedObjectId="setSelectedObjectId"
                         v-if="showInspector"/>
            </el-col>
          </el-row>
        </el-main>
      </el-container>
    </el-main>
  </el-container>
</template>

<script>
import axios from 'axios'
import {heapDumpService} from '../../util'

import Overview from './Overview'
import Inspector from './Inspector'
import LeakSuspects from './LeakSuspects'
import ViewMenu from "../menu/ViewMenu"
import SystemProperty from "./SystemProperty"
import Thread from "./Thread";
import Histogram from "./Histogram"
import DuplicatedClasses from "./DuplicatedClasses"
import Query from "./Query"
import DynamicResultSlot from "./DynamicResultSlot"
import DominatorTree from "./DominatorTree"
import GCRoots from "./GCRoots"
import UnreachableObjects from './UnreachableObjects'
import ClassLoaders from './ClassLoaders'
import DirectByteBuffer from './DirectByteBuffer'
import HeapFileCompare from './HeapFileCompare'
import {Loading} from "element-ui";

export default {
    props: ['file'],
    data() {
      return {
        type: 'HEAP_DUMP',
        optionViewVisible: false,
        options: {
          keepUnreachableObjects: true,
          strictness: 'stop'
        },
        progress: 0,
        progressState: 'info',
        message: '',

        pollingInternal: 1000,
        analysisState: 'NOT_STARTED',

        activeTab: 'overview',
        oriActiveTab: null,
        generationInfoAvailable: false,
        selectedObjectId: null,
        showDynamicResultSlot: false,

        showInspector: true,

        resultDivWidth: '100%'
      }
    },
    components: {
      ViewMenu,
      Inspector,
      Overview,
      LeakSuspects,
      GCRoots,
      DominatorTree,
      DuplicatedClasses,
      Histogram,
      Thread,
      SystemProperty,
      Query,
      DynamicResultSlot,
      UnreachableObjects,
      ClassLoaders,
      DirectByteBuffer,
      HeapFileCompare,
    },
    methods: {
      expandResultDivWidth() {
        let width = parseInt(this.resultDivWidth.substr(0, this.resultDivWidth.length - 1));
        width += 7;
        this.resultDivWidth = width + "%";
      },

      shrinkResultDivWidth() {
        let width = parseInt(this.resultDivWidth.substr(0, this.resultDivWidth.length - 1));
        if (width === 100) {
          return;
        }
        width -= 7;
        this.resultDivWidth = width + "%";
      },

      resetResultDivWidth() {
        this.resultDivWidth = '100%';
      },

      switchTab(active, old) {
        if (old !== 'dynamicResultSlot') {
          this.oriActiveTab = old;
        }
      },

      setGenerationInfoAvailable(ava) {
        this.generationInfoAvailable = ava;
      },

      setShowDynamicResultSlot(val) {
        this.showDynamicResultSlot = val;
        if (val) {
          this.oriActiveTab = this.activeTab;
          this.activeTab = 'dynamicResultSlot';
        } else if (this.activeTab === 'dynamicResultSlot') {
          if (!this.oriActiveTab) {
            this.oriActiveTab = 'overview';
          }
          this.activeTab = this.oriActiveTab;
        }
      },

      enableShowDynamicResultSlot() {
        this.setShowDynamicResultSlot(true);
      },

      disableShowDynamicResultSlot() {
        this.setShowDynamicResultSlot(false);
      },

      setSelectedObjectId(id) {
        this.selectedObjectId = id;
      },

      setShowInspector(val) {
        this.showInspector = val;
      },

      outgoingRefsOfObj(id, label) {
        this.$refs['dynamicResultSlot'].outgoingRefsOfObj(id, label);
        this.enableShowDynamicResultSlot();
      },

      incomingRefsOfObj(id, label) {
        this.$refs['dynamicResultSlot'].incomingRefsOfObj(id, label);
        this.enableShowDynamicResultSlot();
      },

      outgoingRefsOfHistogramObj(id, label) {
        this.$refs['dynamicResultSlot'].outgoingRefsOfHistogramObjs(id, label);
        this.enableShowDynamicResultSlot();
      },

      incomingRefsOfHistogramObj(id, label) {
        this.$refs['dynamicResultSlot'].incomingRefsOfHistogramObjs(id, label);
        this.enableShowDynamicResultSlot();
      },

      outgoingRefsOfClass(id, label) {
        this.$refs['dynamicResultSlot'].outgoingRefsOfClass(id, label);
        this.enableShowDynamicResultSlot();
      },

      incomingRefsOfClass(id, label) {
        this.$refs['dynamicResultSlot'].incomingRefsOfClass(id, label);
        this.enableShowDynamicResultSlot();
      },

      pathToGCRootsOfObj(id, label) {
        this.$refs['dynamicResultSlot'].pathToGCRootsOfObj(id, label);
        this.enableShowDynamicResultSlot();
      },

      mergePathToGCRootsFromDominatorTree(ids, label) {
        this.$refs['dynamicResultSlot'].mergePathToGCRootsFromDominatorTree(ids, label);
        this.enableShowDynamicResultSlot();
      },

      mergePathToGCRootsFromHistogram(id, label) {
        this.$refs['dynamicResultSlot'].mergePathToGCRootsFromHistogram(id, label);
        this.enableShowDynamicResultSlot();
      },

      pollProgressOfAnalysis() {
        let self = this;
        if (!self || self._isDestroyed) {
          return;
        }
        axios.get(heapDumpService(this.file, 'progressOfAnalysis')).then(resp => {
          let state = resp.data.state;
          let percent = resp.data.percent;
          if (resp.data.message) {
            this.message = resp.data.message.replace(/\\n/gm, "<b/>");
          }
          if (state === 'IN_PROGRESS') {
            if (percent >= 1) {
              this.progress = 99;
            } else {
              this.progress = percent * 100;
            }
            setTimeout(this.pollProgressOfAnalysis, this.pollingInternal)
          } else if (state === 'SUCCESS') {
            this.progress = 100;
            this.progressState = 'success';
            this.$notify({
              title: this.$t("jifa.goToOverViewPrompt"),
              position: 'top-right',
              type: "success",
              offset: 300,
              duration: 1000,
              showClose: true,
              onClose: () => {
                this.analysisState = "SUCCESS";
              }
            })
          } else {
            this.progressState = 'danger';
            this.analysisState = "ERROR";
            axios.post(heapDumpService(this.file, 'release'));
          }
        })
      },

      analyzeHeapDump() {
        this.optionViewVisible = false;
        let params = new FormData();
        params.append('keep_unreachable_objects', this.options.keepUnreachableObjects);
        params.append('strictness', this.options.strictness);
        this.doAnalyzeHeapDump(params);
      },

      doAnalyzeHeapDump(params) {
        axios.post(heapDumpService(this.file, 'analyze'), new URLSearchParams(params)).then(() => {
          this.analysisState = "IN_PROGRESS";
          this.pollProgressOfAnalysis();
        })
      },
    },
    mounted() {
      let loadingInstance = Loading.service({fullscreen: true})
      axios.get(heapDumpService(this.file, 'isFirstAnalysis')).then(resp => {
        if (resp.data.result) {
          this.optionViewVisible = true
        } else {
          this.doAnalyzeHeapDump(new FormData())
        }
        loadingInstance.close()
      })
    }
  }
</script>

<style scoped>
  .mainTabs {
    height: 100%;
  }

  .mainTabs /deep/ .el-tabs__content {
    height: 100%;
    overflow: auto;
  }

  .mainTabs /deep/ .el-tab-pane {
    height: 100%;
    overflow: auto;
  }

  .mainTabs .dynamicTab /deep/ .el-tabs__content {
    height: unset;
    position: absolute;
    top: 60px;
    left: 0;
    right: 0;
    bottom: 0;
    overflow: auto;
  }

  .mainTabs .dynamicTab /deep/ .el-tab-pane {
    height: unset;
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    overflow: auto;
  }
</style>
