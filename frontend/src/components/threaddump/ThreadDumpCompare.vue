<!--
    Copyright (c) 2023 Contributors to the Eclipse Foundation

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
                       @search="doSearch"
                       type="THREAD_DUMP_COMPARE"/>
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
            <el-dialog :visible.sync="threadTableVisible" width="60%" top="5vh">
              <thread :file="file[file.length-1]" :id="selectedThreadId" />
            </el-dialog>

            <el-container v-if="analysisState === 'SUCCESS'" style="height: 100%">
              <el-aside width="250px">
                <div style="font-size: 16px; height: 100%;">
                <el-card :header="$t('jifa.threadDumpCompare.navigation')" style="height: 100%;">
                  <div class="nav-item"><a href="#navTop">{{ $t('jifa.threadDumpCompare.navToTop') }}</a></div>
                  <div class="nav-item" v-for="(fileInfo) in comparison.fileInfos" :key="fileInfo.name"><a :href='"../threadDump?file=" + fileInfo.name' target="_blank" rel="noopener">{{computeFilename(fileInfo)}}</a></div>
                  <el-divider/>
                  <div class="nav-item"><a href="#diagnostic">{{ $t('jifa.threadDumpCompare.diagnosisTitle') }}</a></div>
                  <div class="nav-item"><a href="#stateCompare">{{ $t('jifa.threadDumpCompare.stateCompare') }}</a></div>
                  <div class="nav-item"><a href="#threadGroupCompare">{{ $t('jifa.threadDumpCompare.threadGroupCompare') }}</a></div>
                  <div class="nav-item"><a href="#cpuConsumption">{{ $t('jifa.threadDumpCompare.cpuConsumingTitle') }}</a></div>
                </el-card>
              </div>
             </el-aside>
              <el-main style="padding: 20px; height: 100%;" id="navTop">
                <el-container>
                  <el-card :header="$t('jifa.threadDumpCompare.diagnosisTitle')" style="width: 100%;" id="diagnostic">
                    <span> {{ $t('jifa.threadDumpCompare.diagnosisDescription') }}</span>
                    <div>
                      <el-row :gutter="10">
                        <el-col :span="24">
                          <diagnose :file="file"></diagnose>
                        </el-col>
                      </el-row>
                    </div>
                  </el-card>
                </el-container>
                <el-container>
                  <el-card :header="$t('jifa.threadDumpCompare.stateCompare')" style="width: 100%;" id="stateCompare">
                    <span> {{ $t('jifa.threadDumpCompare.stateCompareDescription') }}</span>
                    <div>
                      <el-row :gutter="10">
                        <el-col :span="10">
                          <table style="width: 100%;" class="thread-state-table">
                            <thead>
                              <th>Thread State</th>
                              <th v-for="(fileInfo) in comparison.fileInfos" :key="fileInfo.name"><a :href='"../threadDump?file=" + fileInfo.name' target="_blank" rel="noopener">{{computeFilename(fileInfo)}}</a></th>
                            </thead>
                            <tbody>
                                <tr v-for="(state, stateIndex) in comparison.overviews[0].javaStates" :key="stateIndex">
                                    <td>{{state}}</td>
                                    <td v-for="(overview, index) in comparison.overviews" style="text-align: right;" :key="index" >
                                      <span v-if="index>0 && computeThreadCountDiff(comparison, index, stateIndex) < 0" class="data-negative">( {{ computeThreadCountDiff(comparison, index, stateIndex) }} ) </span>
                                      <span v-if="index>0 && computeThreadCountDiff(comparison, index, stateIndex) > 0" class="data-positive">( +{{ computeThreadCountDiff(comparison, index, stateIndex) }} ) </span>                                      
                                      <span>{{overview.javaThreadStat.javaCounts[stateIndex]}}</span>
                                    </td>
                                </tr>
                            </tbody>
                          </table>
                        </el-col>
                        <el-col :span="12"><line-chart :chart-data="threadsChartData.data" :options="threadsChartOptions" :width='800' :height='400' /></el-col>
                      </el-row>
                    </div>
                  </el-card>
                </el-container>
                <el-container>
                  <el-card :header="$t('jifa.threadDumpCompare.threadGroupCompare')" style="width: 100%;" id="threadGroupCompare">
                    <span>{{ $t('jifa.threadDumpCompare.threadGroupCompareDescription') }}</span>
                    <div>
                        <el-col :span="20"><bar-chart :chart-data="threadGroupChartData" :options="threadGroupChartOptions" :width='1000' :height='400' /></el-col>
                    </div>
                  </el-card>
                </el-container>
                <el-container>
                  <el-card :header="$t('jifa.threadDumpCompare.cpuConsumingTitle')" style="width: 100%;" id="cpuConsumption">
                    <span>{{ $t('jifa.threadDumpCompare.cpuConsumingDescription') }}</span>
                    <div>
                        <el-col :span="20"><bar-chart :chart-data="cpuConsumptionChartData" :options="cpuConsumptionChartOptions" :width='1000' :height='400' /></el-col>
                    </div>
                  </el-card>
                </el-container>
              </el-main>
            </el-container>
          </el-main>
        </el-container>
    </el-container> 
  </template>
  
  <script>
  import ViewMenu from "../menu/ViewMenu";
  import axios from "axios";
  import {threadDumpBase, determineTimeUnit, formatTimeDuration } from "@/util";
  import Thread from "@/components/threaddump/Thread";
  import LineChart from '../charts/LineChart'
  import BarChart from '../charts/BarChart'
  import Diagnose from "./Diagnose.vue";
    

  export default {
    components: {
    LineChart,
    BarChart,
    ViewMenu,
    Thread,
    Diagnose,
},
    data() {
      return {
        color: [
        '#003f5c',
        '#2f4b7c',
        '#665191',
        '#a05195',
        '#d45087',
        '#f95d6a',
        '#ff7c43',
        '#ffa600',
        '#488f31',
        '#8aa1b4'
        ],
        analysisState: 'NOT_STARTED',
        progressState: 'info',
        loading: true,
        message: '',
        progress: 0,
        pollingInternal: 500,
        comparison: null,
        file: [],
        threadTableVisible: false,
        selectedThreadId: null,
        threadsChartData: {
          type: 'line',
          data: {
            datasets: {}
          }
        },
        threadsChartOptions: {
          responsive: false,
          maintainAspectRatio: false,
          plugins: {
            tooltip: {
              mode: 'index'
            },
          },
          legend: {
            position: 'bottom'
          },
          interaction: {
            mode: 'nearest',
            axis: 'x',
            intersect: false
          },
          scales: {
            yAxes: [{
              stacked: true,
            }]
          }
        },
        threadGroupStats: [],
        threadGroupChartData: {},
        threadGroupChartOptions: {
          responsive: false,
          maintainAspectRatio: false,
          parsing: true,
        },
        cpuConsumptionChartData: {},
        cpuConsumptionChartOptions: {
          responsive: false,
          maintainAspectRatio: false,
          parsing: true,
          onClick: (event,data) => {
            let thread = this.cpuConsumptionChartData.threads[data[0]._index];
            this.selectThreadId(thread.id);
          }
        },
 
      }
    },
    methods: {
      analyzeThreadDump() {
        let self = this;
        this.analysisState = "IN_PROGRESS";
        axios.all(
          [axios.get(threadDumpBase() + 'compare/summary', {
            params: {
              file: this.file,
            },
            paramsSerializer: {
              indexes: null, // no brackets for multi-value params
            } 
          }),
           axios.get(threadDumpBase() + 'compare/compareCPUConsumption', {
            params: {
              file: this.file,
              max: 10,
              type: "JAVA",
            },
            paramsSerializer: {
              indexes: null, // no brackets for multi-value params
            } 
           })]).then(axios.spread((resp1, resp2) => {
          let summary = resp1.data
          self.comparison = summary
        
          //create chart data
          this.createThreadStateChartData(summary)
          this.createThreadGroupStats(summary)

          //create the CPU consumption chart data
          this.createCpuConsumingStats(resp2.data)
          this.loading = false
          this.analysisState = "SUCCESS"
        }))        
      },
      createThreadGroupStats(summary) {
        //creates bar chart groups for the largest thread groups (based on the first dump)  
        let firstStats = summary.overviews[0].threadGroupStat
        let candidates = []

        for (let k in firstStats) {
          candidates.push({
            key: k,
            value: this.sum(summary.overviews[0].threadGroupStat[k].counts),
          })
        }
        candidates.sort((i, j) => j.value - i.value)
        candidates = candidates.slice(0,8) //only the top most interesting
       
        this.threadGroupChartData = {     
          //the labels are names of the thread groups we will. Each thread group will be a group of bar charts (one bar per dump)                     
          labels: candidates.map(candidate => candidate.key),
          datasets: []
        }

        //for each dump, we need to private a data[] containing as many int values, as there is thread groups
        summary.overviews.forEach( (overview, index) => {
          let groupData = []
          candidates.forEach(threadGroup => {
            let currentStat = overview.threadGroupStat[threadGroup.key]
            if(currentStat!=null) {
              groupData.push(this.sum(currentStat.counts))
            }
          });
          this.threadGroupChartData.datasets.push({
            backgroundColor: this.color[index],
            label: this.computeFilename(summary.fileInfos[index]),
            data: groupData
          })
        });

      },

      createThreadStateChartData(summary) {
        let self = this;
        self.threadsChartData.data.labels = []
        summary.fileInfos.forEach(fileInfo => {
          self.threadsChartData.data.labels.push(self.computeFilename(fileInfo))
        })
        self.threadsChartData.data.datasets = []
        //one dataset per thread state kind, one data point per dump
        let states = summary.overviews[0].javaStates
        for (let i = 0; i < states.length; i++) {
          let stateData = []
          summary.overviews.forEach(overview => {
            stateData.push({
              y: overview.javaThreadStat.javaCounts[i],
            })
          })
          self.threadsChartData.data.datasets.push({
            data: stateData,
            backgroundColor: self.color[i],
            label: states[i],
          })
        }
      },

      computeThreadCountDiff(comparison, overviewIndex, stateIndex) {
        let value = comparison.overviews[overviewIndex].javaThreadStat.javaCounts[stateIndex]
        if (overviewIndex==0)
          return value
        let baseValue = comparison.overviews[0].javaThreadStat.javaCounts[stateIndex]
        let diff = value - baseValue
        return diff
      },

      sum(arr) {
        let s = 0;
        for (let i = 0; i < arr.length; i++) {
          s += arr[i]
        }
        return s
      },
      createCpuConsumingStats(threads) {
        let self = this;
        if(threads.length>0) {
          let timeunit = determineTimeUnit(threads[0].cpu);
  
          this.cpuConsumptionChartData = {     
            //the labels are names of the thread groups we will. Each thread group will be a group of bar charts (one bar per dump)                     
            labels: threads.map(t => t.name),
            threads: threads,
            datasets: [{
              data: threads.map(t => formatTimeDuration(t.cpu, timeunit)),
              backgroundColor: self.color,
              label: self.$t('jifa.threadDumpCompare.cpuConsumingDatasetLabel',{unit: timeunit}),
            }]
          }

        }
      },

      selectThreadId(id) {
        this.selectedThreadId = id
        this.threadTableVisible = true
      },
      doSearch(searchText) {
        const query = {
          file: this.file,
          term: searchText,
        }
        const url = this.$router.resolve({
          name: 'threadDumpSearch',
          query: query
        })
        window.open(url.href)
      },
      computeFilename(fileInfo) {
        if(fileInfo.originalName != null && fileInfo.originalName.length<fileInfo.name) {
          return this.truncate(fileInfo.originalName, 40)
        }
        return this.truncate(fileInfo.name, 40)
      },
      truncate(str, n){
        return (str.length > n) ? str.slice(0, n-1) + '...' : str;
      },
    },

    
    mounted() {
      this.file = this.$route.query.file
      this.analyzeThreadDump();
    }
  }
  </script>
  <style>
  .data-positive {
    color: green;
    font-weight: bold;
  }

  .data-negative {
    color: red;
    font-weight: bold;
  }

  .thread-state-table {
    font-size: 0.9rem;
  }

  .thread-state-table tr{
    font-size: 0.9rem;
    border: 1px solid black;
    padding: 3px;
  }

  .thread-state-table th{
    font-size: 0.9rem;
    border: 1px solid black;
    padding: 3px;
    text-align: center;
  }

  .thread-state-table td{
    font-size: 0.9rem;
    border: 1px solid black;
    padding: 3px;
  }

  thead {
    background-color: #d2d7e2;
    font-size: 1rem;
  }

  </style>