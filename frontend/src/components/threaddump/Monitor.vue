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
    <el-dialog :visible.sync="monitorThreadTableVisible" width="60%" top="5vh">
      <monitor-thread :file="file" :id="selectedMonitorId" :title="title"/>
    </el-dialog>

    <el-table
        ref='table'
        row-key="rowKey"

        :cell-style='cellStyle'
        :data="tableData"
        :show-header="false"
        stripe
        v-loading="loading"
    >
      <el-table-column>
        <template slot-scope="scope">
          <span v-if="scope.row.dataRow">
            <i class="el-icon-lock" style="color: cornflowerblue"/>
            <span style="margin-left: 8px; margin-right: 2px; cursor: pointer; color: #409eff;"
                  @click="selectedMonitorId = scope.row.id; title = scope.row.content; monitorThreadTableVisible=true">
              {{ scope.row.content }}</span>
          </span>
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
</template>

<script>
import axios from "axios";
import {threadDumpService} from "@/util";
import {rawMonitorToString} from "@/components/threaddump/util";
import MonitorThread from "@/components/threaddump/MonitorThread";

export default {
  props: ['file'],
  components: {
    MonitorThread
  },
  data() {
    return {
      cellStyle: {padding: '8px'},
      loading: false,

      pageSize: 5,
      tableData: [],

      selectedMonitorId: -1,
      title: '',
      monitorThreadTableVisible: false
    }
  },
  methods: {
    loadData(page) {
      this.loading = true

      axios.get(threadDumpService(this.file, "monitors"), {
        params: {
          page,
          pageSize: this.pageSize
        }
      }).then(resp => {
        let data = resp.data
        let loaded = this.tableData
        if (loaded.length > 0) {
          // the last is summary row
          loaded.splice(loaded.length - 1, 1)
        }

        let monitors = data.data
        monitors.forEach(monitor => {
          loaded.push({
            dataRow: true,
            id: monitor.id,
            content: rawMonitorToString(monitor)
          })
        })

        if (data.totalSize > 1) {
          loaded.push({
            summaryRow: true,
            size: loaded.length,
            totalSize: data.totalSize,
            dblclick: () => {
              if (loaded.length - 1 /* summary */ < data.totalSize) {
                this.loadData(page + 1);
              }
            }
          })
        }
        this.loading = false
      })
    },
  },
  mounted() {
    this.loadData(1)
  },
}
</script>