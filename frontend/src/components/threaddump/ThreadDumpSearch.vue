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
   <el-container style="height: 100%">
     <el-container>
       <el-header>
         <view-menu subject="analysisResult" :file="file" :analysisState="analysisState" type="THREAD_DUMP_SEARCH" />
       </el-header>

       <el-main style="padding-top: 0" v-loading="loading" :element-loading-text="$t('jifa.threadDumpSearch.loadingPlaceholder')" element-loading-background="rgba(0, 0, 0, 0.8)">

         <el-container v-if="analysisState === 'SUCCESS'" style="height: 100%"  >
           <el-aside width="250px">
             <el-menu class="el-menu-vertical">
               <template #title>
               </template>
               <el-menu-item class="thread-menu-item" @click="scrollToRef('search')"><i class="el-icon-search"></i>{{ $t('jifa.threadDumpSearch.searchTitle') }}</el-menu-item>
               <el-menu-item-group :title="$t('jifa.threadDumpSearch.menuThreadsTitle')">
                <el-tooltip v-for="(thread, index) in searchResult" :key="thread.filename + thread.id" :disabled="thread.name.length < 25" :content="thread.name" placement="right">
                 <el-menu-item class="thread-menu-item" @click="scrollToThread(thread, index)">{{ truncate(thread.name, 25)}}</el-menu-item>
                </el-tooltip>
               </el-menu-item-group>
             </el-menu>
           </el-aside>
           <el-main style="padding: 20px; height: 100%;">
              <thread-dump-search-form :ref="'search'" style="width: 100%;" @submit="search => searchSubmitted(search)"></thread-dump-search-form>

             <el-container style="width: 100%;">
               <el-row :gutter="20" style="width: 100%;">
                 <el-col :span="12">
                   <el-card class="mt-3" bg-variant="dark" text-variant="white"
                     :header="$tc('jifa.threadDumpSearch.resultsHeader', searchResult.length, { count: searchResult.length })">
                     <doughnut-chart :chart-data="chartHitsData" :options="chartHitsOptions" :width="150" :height="150" />
                   </el-card>
                 </el-col>
                 <el-col :span="12">
                   <el-card class="mt-3" bg-variant="dark" text-variant="white"
                     :header="$t('jifa.threadDumpSearch.threadStatesChartTitle')">
                     <doughnut-chart :chart-data="chartThreadStatesData" :options="chartHitsOptions" :width="150" :height="150" />
                   </el-card>
                 </el-col>
               </el-row>
             </el-container>

             <el-container v-for="(thread, index) in searchResult" :key="thread.filename + thread.id" :ref="index">
                 <el-card class="mt-3" bg-variant="dark" text-variant="white" style="width: 100%;">
                   <template #header>

                     <el-row :gutter="20">
                       <el-col :span="14">
                         <span>{{ thread.name }}</span>
                       </el-col>
                       <el-col
                         :span="2"><!-- percentage is times 10, to boost the bar width a bit. Typically elapsed is a lot larger than cpu-->
                         <el-progress :text-inside="true" :stroke-width="24"
                           :percentage=" Math.min(100,(thread.cpu / thread.elapsed) * 1000)" :format="progressFormat" status="success">
                           <span>CPU</span>
                         </el-progress>
                       </el-col>
                       <el-col :span="8">
                         <el-tag type="info" class="mx-1" effect="dark" round size="small">{{ thread.filename }}</el-tag>
                         <el-tag type="info" class="mx-1" effect="dark" round size="small"
                           :color="color[getThreadState(thread).length % color.length]">{{ getThreadState(thread)}}</el-tag>
                       </el-col>
                     </el-row>
                   </template>
                   <b-card-text class="thread-content" v-html="renderThreadContent(thread)"></b-card-text>
                 </el-card>
          </el-container>
        </el-main>
      </el-container>
    </el-main>
  </el-container>
</el-container></template>
  
  <script>
  import ViewMenu from "../menu/ViewMenu";
  import axios from "axios";
  import {threadDumpBase} from "@/util";
  import DoughnutChart from '../charts/DoughnutChart'
  import ThreadDumpSearchForm from "./ThreadDumpSearchForm.vue";    
   

  export default {
    components: {
      DoughnutChart,
      ViewMenu,
      ThreadDumpSearchForm
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
        search: {
          //search form model
          term: null,
        },
        file: null,
        searchResult: null,
        progressFormat: (percentage) =>  (percentage.toFixed(1) + ' ‰ CPU'),
        chartHitsData: null,
        chartHitsOptions: {
          responsive: true,
          maintainAspectRatio: false,
      },

      }
    },
    methods: {

      getThreadState(thread) {
        return thread.javaState != null ? thread.javaState : thread.osState;
      },
      selectThreadId(id) {
        this.selectedThreadId = id
        this.threadTableVisible = true
      },
      searchSubmitted(search) {
        this.search = search
        search.file = this.file,
        this.$router.replace({ query: search })
        this.doSearch(search)
      },
      doSearch(search) {
        this.analysisState = "IN_PROGRESS";
        this.loading = true
        if(search.term == null) {
          return
        }
        if(!Array.isArray(search.term)) {
          search.term = search.term.split(" ")
        }
        search.file = this.file
        axios.get(threadDumpBase() + 'searchThreads', {
          params: search,
          paramsSerializer: {
            indexes: null, // no brackets for multi-value params
          }
        }).then(resp => {
          this.searchResult = resp.data

          //search hits chart
          let hitsPerFile = []
          let allFiles = Array.isArray(this.file) ? this.file : [this.file]
          allFiles.forEach(currentFile => {
            let result = resp.data.filter(hit => hit.filename === currentFile)
            hitsPerFile.push(result.length)
          })
          this.chartHitsData = {
            labels: allFiles,
            datasets: [
              {
                data: hitsPerFile,
                backgroundColor: this.color,
              }
            ]
          }

          //thread states
          let allStates = Array.from(new Set(resp.data.filter(hit => hit.javaState != null).map(hit => hit.javaState)))
          let hitsPerState = []
          allStates.forEach(currentState => {
            let result = resp.data.filter(hit => hit.javaState === currentState)
            hitsPerState.push(result.length)
          })
          this.chartThreadStatesData = {
            labels: allStates,
            datasets: [
              {
                data: hitsPerState,
                backgroundColor: this.color,
              }
            ]
          }
          this.loading = false
          this.analysisState = "SUCCESS"
        })  
      },

      truncate(str, n){
        return (str.length > n) ? str.slice(0, n-1) + '...' : str;
      },
      scrollToThread(thread, index) {
        let element = this.$refs[index][0]
        if(element != null) {
          element.$el.scrollIntoView({ behavior: 'smooth' });
        }
      },
      scrollToRef(ref) {
        let element = this.$refs[ref]
        if(element != null) {
          element.$el.scrollIntoView({ behavior: 'smooth' });
        }
      },
      renderThreadContent(thread) {
        
        let content = "";
        let terms = Array.isArray(this.search.term) ? this.search.term : this.search.term.split(" ")
        let patterns = []
        terms.forEach(t => {
          if(!this.search.regex) {
            //if it's not a regex, special characters must be escaped
            t = t.replace(/[-[\]{}()*+?.,\\^$|]/g, "\\$&")
          }

          let flags = "g"
          if(!this.search.matchCase) {
            flags = flags + "i"
          }
          patterns.push(new RegExp('('+t+')',flags))
        })
        
        thread.lines.slice(1).forEach(line => {
          let modified = line.replace("<","&lt;").replace(">","&gt;")+"\n" //sanitize
          patterns.forEach(pattern => {
            modified = modified.replaceAll(pattern,'<span class="search-hit">$1</span>')
          })
          content +=modified
        })
        return content
      }
    },

    
    mounted() {
      this.file = this.$route.query.file
      this.search = this.$route.query
      this.doSearch(this.search);
    }
  }
  </script>
  <style>
  .thread-content {
    margin-top: 15px;
    background-color: #343a40;
    color: #fff;
    overflow: auto;
    white-space: pre;
    font-size: x-small;
    font-family: monospace;
  }

  .el-progress-bar__innerText {
    font-weight: bold;
    color: #343a40 !important;
  }

  .search-hit {
    color: red;
    background-color: white;
    font-weight: bold;
  }

  </style>