<!--
    Copyright (c) 2020 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<template>
  <div style="height: 100%; position: relative">
    <div v-if="!selectedFile" style="height: 100%">
      <div style="height: 40px; margin-bottom: 5px; margin-top: 10px;">
        <el-input style="margin-bottom: 10px;" size="mini" v-model="searchInput"
                  @keyup.enter.native="doSearch"
                  :placeholder="$t('jifa.typeKeyWord')" clearable/>
      </div>
      <div style="position: absolute; top: 50px; left: 0; right: 0; bottom: 80px;">
        <el-table :data="candidateFiles"
                  :highlight-current-row="false"
                  stripe
                  :header-cell-style="headerCellStyle"
                  :cell-style='fileViewCellStyle'
                  row-key="rowKey"
                  lazy
                  height="100%"
                  v-loading="fileViewLoading"
        >
          <el-table-column label="File Name ( Please double click to choose a file as a heap baseline )" prop="name"
                           show-overflow-tooltip>
            <template slot-scope="scope">
            <span style="cursor: pointer" @dblclick="doCompare(scope.row.finalName)">
              <i class="el-icon-document"></i> {{scope.row.originalName}}
            </span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div style="position: absolute; left: 0; right: 0; bottom: 0; height: 80px">
        <el-row type="flex" justify="space-around" style="margin-top: 15px">
          <el-col :span="8">
            <el-pagination
                    background
                    layout="prev, pager, next"
                    :current-page="fileViewPage"
                    :page-size="pageSize"
                    :total="candidateFileTotalSize"
                    @current-change="handleFileViewCurrentChange"
                    :hide-on-single-page=false
                    align="center">
            </el-pagination>
          </el-col>
        </el-row>
      </div>
    </div>

    <div v-else style="height: 100%">
      <div style="margin-bottom: 6px; height: 50px">
        <el-button size="mini" icon="el-icon-back" @click="reselect" circle></el-button>
        <span style='margin-left: 10px; font-size: 15px; font-weight: normal;
                   font-family: "Helvetica Neue",Helvetica,"PingFang SC","Hiragino Sans GB","Microsoft YaHei","微软雅黑",Arial,sans-serif;
                   color: #606266;
                  '>
          <i class="el-icon-document"></i> {{selectedFile}}
        </span>
      </div>

      <div style="position: absolute; top: 50px; left: 0; right: 0; bottom: 0;">
        <el-table :data="tableData"
                  :highlight-current-row="false"
                  stripe
                  :header-cell-style="headerCellStyle"
                  :cell-style='cellStyle'
                  row-key="rowKey"
                  lazy
                  :indent=8
                  height="100%"
                  v-loading="loading"
        >
          <el-table-column label="Class Name" width="800px" show-overflow-tooltip="">
            <template slot-scope="scope">
          <span v-if="scope.row.isRecord">
            <img :src="classIcon"/> {{scope.row.className}}
          </span>
              <span v-if="scope.row.isSummary">
            <img :src="sumIcon" v-if="records.length >= summary.totalSize"/>
            <img :src="sumPlusIcon" @dblclick="fetchRecords" style="cursor: pointer" v-else/>
            {{ toReadableCount(records.length) }} <strong> / </strong> {{ toReadableCount(summary.totalSize) }}
          </span>
            </template>
          </el-table-column>

          <el-table-column label="Objects">
            <template slot-scope="scope">
              {{ scope.row.objects > 0 ? '+' : '' }} {{ toReadableCount(scope.row.objects)}}
            </template>
          </el-table-column>

          <el-table-column label="Shallow Heap">
            <template slot-scope="scope">
              {{ scope.row.shallowHeap > 0 ? '+' : '' }} {{ toReadableSizeWithUnit(scope.row.shallowHeap) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
  import axios from 'axios'
  import {heapDumpService, toReadableCount, toReadableSizeWithUnit} from '../../util'

  let rowKey = 1
  export default {
    props: ['file'],
    methods: {
      toReadableCount,
      toReadableSizeWithUnit,
      fetchCompareSummary() {
        this.loading = true
        axios.get(heapDumpService(this.file, 'compare/summary'), {
          params: {
            baseline: this.selectedFile
          }
        }).then(resp => {
          this.summary = resp.data
          if (this.summary.totalSize > 0) {
            this.fetchRecords()
          } else {
            this.loading = false
          }
        })
      },
      fetchRecords() {
        this.loading = true
        axios.get(heapDumpService(this.file, 'compare/records'), {
          params: {
            page: this.nextPage,
            pageSize: this.pageSize,
            baseline: this.selectedFile
          }
        }).then(resp => {
          let records = resp.data.data
          records.forEach(record => this.records.push({
            rowKey: rowKey++,
            className: record.className,
            objects: record.objects,
            shallowHeap: record.shallowSize,
            isRecord: true,
          }))

          this.tableData = this.records.concat({
            rowKey: rowKey++,
            objects: this.summary.objects,
            shallowHeap: this.summary.shallowSize,
            isSummary: true
          })

          this.nextPage++
          this.loading = false
        })
      },
      doCompare(fileName) {
        this.selectedFile = fileName
        this.fetchCompareSummary()
      },
      reselect() {
        this.selectedFile = null
        this.summary = null
        this.records = []
        this.tableData = []
        this.nextPage = 1
      },
      doSearch() {
        this.expectedFile = this.searchInput
        this.fileViewPage = 1
        this.fetchFiles(this.fileViewPage)
      },
      handleFileViewCurrentChange(page) {
        this.fetchFiles(page)
      },
      fetchFiles(page) {
        this.fileViewLoading = true
        axios.get(heapDumpService(this.file, 'compare/files'), {
          params: {
            page: page,
            pageSize: this.pageSize,
            expected: this.expectedFile,
          }
        }).then(resp => {
          this.candidateFileTotalSize = resp.data.totalSize
          this.candidateFiles = resp.data.data
          this.fileViewLoading = false
        })
      }
    },
    data() {
      return {
        fileViewCellStyle: {padding: '8px', fontSize: '15px'},
        cellStyle: {padding: '4px', fontSize: '12px'},
        headerCellStyle: {padding: 0, 'font-size': '12px', 'font-weight': 'normal'},
        pageSize: 25,

        fileViewPage: 1,
        fileViewLoading: false,
        searchInput: null,
        expectedFile: null,
        candidateFiles: [],
        candidateFileTotalSize: 0,


        selectedFile: null,
        classIcon: require('../../assets/heap/objects/class.gif'),
        sumIcon: require('../../assets/heap/misc/sum.gif'),
        sumPlusIcon: require('../../assets/heap/misc/sum_plus.gif'),
        loading: false,
        summary: null,
        nextPage: 1,
        records: [],
        tableData: [],
      }
    },
    created() {
      this.fetchFiles(this.fileViewPage)
    }
  }
</script>
