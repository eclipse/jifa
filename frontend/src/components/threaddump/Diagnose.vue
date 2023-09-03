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
    <div>
      <el-dialog :visible.sync="threadTableVisible" width="60%" top="5vh">
        <thread :file="selectedFile" :idList="selectedThreadIds" />
      </el-dialog>
      <el-container>
        <el-table :data="diagnostics" style="width: 100%" v-loading="loading" :default-sort = "{prop: 'file', order: 'ascending'}">
          <el-table-column :label="$t('jifa.threadDump.diagnosis.messageColumn')" width="500" sortable prop="severity">
            <template slot-scope="scope">
              <i :style="computeColor(scope.row.severity)" :class="computeClass(scope.row.severity)" :title="scope.row.severity"></i> <span style="margin-left: 10px">{{ scope.row.message }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('jifa.threadDump.diagnosis.fileColumn')" width="300" v-if="Array.isArray(file) && file.length > 1" sortable prop="file">
            <template slot-scope="scope">
              <a :href='"../threadDump?file=" + scope.row.file' target="_blank" rel="noopener">{{truncate(scope.row.file, 40)}}</a>
            </template>
          </el-table-column>
          <el-table-column :label="$t('jifa.threadDump.diagnosis.suggestionColumn')" sortable prop="suggestion">
            <template slot-scope="scope">
              {{ scope.row.suggestion }}
              <span>  </span>
              <el-button v-if="Array.isArray(scope.row.threads)" @click="selectThreads(scope.row.threads, scope.row.file)" type="text">{{$t('jifa.threadDump.diagnosis.examine')}} <i class="el-icon-question el-icon-right"></i></el-button>
            </template>
          </el-table-column>
        </el-table>      
      </el-container>
    </div>
</template>

<script>
import axios from 'axios';
import {threadDumpService} from "@/util";
import Thread from "@/components/threaddump/Thread";

export default {
  props: ['file'],
  components: {
    Thread,
  },
  data() {
    return {
        loading: true,
        message: '',
        progress: 0,
        diagnostics: [],
        pollingInternal: 500,
        threadTableVisible: false,
        selectedThreadIds: null,
        selectedFile: null,
      }
  },
  methods: {
    loadData() {
      let files = []
      this.diagnostics = []
      if(Array.isArray(this.file)) {
        files = this.file;
      }
      else {
        files.push(this.file);
      }
      files.forEach(file => {
        axios.get(threadDumpService(file, "diagnoseInfo"), {
          params: {
          }
        }).then(resp => {
          
          if(resp.data == null || resp.data.length===0) {
            this.diagnostics.push({
              severity: "OK",
              message: "No issues found",
              file: file,
            })
          }
          else {
            resp.data.forEach(diagnostic => {
              diagnostic.file = file
              this.diagnostics.push(diagnostic)
            })
          }
          if(file===files[files.length-1]) {
            // last file
            this.loading = false
          }
        })
      })
    },
    computeColor(severity) {
      if(severity==='ERROR') return "color: #F56C6C"
      if(severity==='WARNING') return "color: #E6A23C"
      if(severity==='OK') return "color: #67C23A"
      return "color: #909399"
    },
    computeClass(severity) {
      if(severity==='ERROR') return "el-icon-error"
      if(severity==='WARNING') return "el-icon-warning"
      if(severity==='OK') return "el-icon-success"
      return "el-icon-info"
    },

    selectThreads(threads, file) {
      this.selectedThreadIds = threads.map(t => t.id)
      this.selectedFile = file
      this.threadTableVisible = true
    },
    truncate(str, n){
        return (str.length > n) ? str.slice(0, n-1) + '...' : str;
    },
  },
  mounted() {
    this.loadData()
  },

};
</script>
<style>
  
  .el-table .cell {
    word-break: break-word !important;
    font-size: larger;
  }
  .el-table .el-button {
    font-size: medium;
  }
</style>