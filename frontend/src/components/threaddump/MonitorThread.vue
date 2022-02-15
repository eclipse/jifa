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
    <el-alert :title="title" type="success">
    </el-alert>
    <div v-for="state in states" v-bind:key="state" style="margin-top: 10px">
      <el-tag>{{ state }}</el-tag>
      <el-table
          :cell-style='cellStyle'
          :data="tableDataOfState[state].tableData"
          :show-header="false"
          stripe
          :v-loading="tableDataOfState[state].loading"

          style="margin-top: 10px"
      >
        <el-table-column>
          <template slot-scope="scope">
            <div v-if="scope.row.dataRow">
              <i class="el-icon-view" style="color: cornflowerblue"/>
              <span style="margin-left: 10px; color: #409eff; cursor: pointer" @click="loadThreadContent(scope.row)">
                {{ scope.row.name }}
              </span>
              <div v-if="scope.row.contentVisible && scope.row.contentLoaded" class="thread-content">
                {{ scope.row.content }}
              </div>
            </div>
            <span v-if="scope.row.summaryRow" @dblclick="scope.row.dblclick()"
                  :style="scope.row.size < scope.row.totalSize ? 'cursor: pointer': ''">
            <i v-if="scope.row.size < scope.row.totalSize" class="el-icon-circle-plus"
               style="color: cornflowerblue; margin-left: 1px;"/>
            <i v-else class="el-icon-remove" style="color: #909399; margin-left: 1px;"/>
            <span style="margin-left: 8px">
              {{ scope.row.size }} <strong> / </strong>  {{ scope.row.totalSize }}
            </span>
          </span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import axios from "axios";
import {threadDumpService} from "@/util";

export default {
  props: ['file', 'id', 'title'],
  data() {
    return {
      cellStyle: {padding: '8px'},

      pageSize: 15,

      states: [],
      tableDataOfState: {}
    }
  },
  methods: {
    loadThreadContent(row) {
      if (row.contentLoaded) {
        row.contentVisible = !row.contentVisible
        return
      }
      this.loading = true
      axios.get(threadDumpService(this.file, "rawContentOfThread"), {params: {id: row.id}})
          .then(resp => {
            let content = ''
            for (let i = 0; i < resp.data.length; i++) {
              content += resp.data[i]
              if (i !== resp.data.length - 1) {
                content += "\n"
              }
            }
            row.content = content
            row.contentVisible = true
            row.contentLoaded = true
            this.loading = false
          })
    },

    loadThreadData(stateData, state, page) {
      stateData.loading = true
      axios.get(threadDumpService(this.file, "threadsByMonitor"), {
        params: {
          id: this.id,
          state,
          page,
          pageSize: this.pageSize
        }
      }).then(resp => {
        let data = resp.data
        let loaded = stateData.tableData
        if (loaded.length > 0) {
          // the last is summary row
          loaded.splice(loaded.length - 1, 1)
        }

        let threads = data.data
        threads.forEach(thread => {
          loaded.push({
            dataRow: true,
            id: thread.id,
            name: thread.name,
            content: '',
            contentLoaded: false,
            contentVisible: false,
          })
        })

        if (data.totalSize > 1) {
          loaded.push({
            summaryRow: true,
            size: loaded.length,
            totalSize: data.totalSize,
            dblclick: () => {
              if (loaded.length - 1 /* summary */ < data.totalSize) {
                this.loadThreadData(stateData, state, page + 1);
              }
            }
          })
        }
        this.loading = false
      })
    },

    loadStateData() {
      this.states = []
      this.tableDataOfState = {}
      if (this.id <= 0) {
        return
      }
      axios.get(threadDumpService(this.file, "threadCountsByMonitor"), {params: {id: this.id}})
          .then(resp => {
            let data = resp.data
            for (let k in data) {
              if (data[k] > 0) {
                this.states.push(k)
                this.tableDataOfState[k] = {
                  totalSize: data[k],
                  tableData: [],
                  loading: false
                }
              }
            }

            for (let k in this.tableDataOfState) {
              this.loadThreadData(this.tableDataOfState[k], k, 1)
            }
          })
    }
  },

  mounted() {
    this.loadStateData()
  },

  watch: {
    id: function () {
      this.loadStateData()
    }
  }
}
</script>

<style scoped>
.thread-content {
  margin-top: 15px;
  background-color: #343a40;
  color: #fff;
  overflow: auto;
  white-space: pre;
}
</style>