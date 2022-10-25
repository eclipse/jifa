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
  <el-table
      ref='table'
      row-key="rowKey"

      :cell-style='cellStyle'
      :data="tableData"
      :show-header="false"
      :span-method="spanMethod"
      stripe
      v-loading="loading"

      lazy
      :load="loadChildrenData"
  >
    <el-table-column>
      <template slot-scope="scope">
        <span v-if="scope.row.dataRow">
          <i class="el-icon-s-operation" style="color: cornflowerblue"/>
          <span style="margin-left: 8px; margin-right: 2px">{{ scope.row.frame }}</span>
          <el-popover
              v-if="scope.row.monitors && scope.row.monitors.length > 0"
              placement="right"
              trigger="hover">
            <el-alert
                v-for="(monitor, index) in scope.row.monitors"
                v-bind:key="monitor"
                :title="monitor"
                :type="alertType(monitor)"
                :closable="false"
                :style="index > 0 ? 'margin-top: 5px' : ''"
            >
            </el-alert>
            <i slot="reference" :class="monitorIcon(scope.row.monitors)" style="color: #ff7d00"/>
          </el-popover>
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
    <el-table-column v-for="i in 8" v-bind:key="i"/>

    <el-table-column prop="weight">
    </el-table-column>
  </el-table>
</template>

<script>
import axios from 'axios'
import {threadDumpService} from '@/util'
import {frameToString, monitorsToStrings} from "@/components/threaddump/util"

let rowKey = 1

export default {
  props: ['file'],
  data() {
    return {
      cellStyle: {padding: '8px'},
      loading: false,

      pageSize: 25,
      tableData: [],

      rowAutoExpandLimit: 0,
      nextAutoExpandRow: null,
    }
  },
  methods: {
    spanMethod(row) {
      let index = row.columnIndex
      if (index === 0) {
        return [1, 9]
      } else if (index >= 1 && index <= 8) {
        return [0, 0]
      }
      return [1, 1]
    },

    resetAutoExpand() {
      this.rowAutoExpandLimit = 8
      this.nextAutoExpandRow = null
    },

    setNextAutoExpandRow(row) {
      if (this.rowAutoExpandLimit > 0) {
        this.nextAutoExpandRow = row
        this.rowAutoExpandLimit--
      } else {
        this.resetAutoExpand()
      }
    },

    alertType(monitor) {
      if (monitor.indexOf("waiting") !== -1 || monitor.indexOf("parking") !== -1) {
        return "warning"
      }
      return "info"
    },

    monitorIcon(monitors) {
      for (let i = 0; i < monitors.length; i++) {
        if (monitors[i].indexOf("waiting") !== -1 || monitors[i].indexOf("parking") !== -1) {
          return "el-icon-video-pause"
        }
      }
      return "el-icon-lock"
    },

    buildData(frame) {
      return {
        dataRow: true,
        rowKey: rowKey++,
        id: frame.id,
        frame: frameToString(frame),
        monitors: frame.monitors? monitorsToStrings(frame.monitors) : null,
        weight: frame.weight,
        hasChildren: !frame.end,
      }
    },

    buildSummary(id, size, totalSize, dblclick) {
      return {
        summaryRow: true,
        rowKey: rowKey++,
        id,
        size,
        totalSize,
        dblclick
      }
    },

    loadChildrenData(tree, treeNode, resolve) {
      this.loadData(tree.id, 1, tree.rowKey, resolve)
    },

    loadData(id, page, parentRowKey, resolve) {
      this.loading = true
      axios.get(threadDumpService(this.file, "callSiteTree"), {
        params: {
          parentId: id,
          page: page,
          pageSize: this.pageSize
        }
      }).then(resp => {
        let data = resp.data
        let loaded = null
        if (parentRowKey > 0) {
          loaded = this.$refs['table'].store.states.lazyTreeNodeMap[parentRowKey]
          if (!loaded) {
            loaded = []
          }
        } else {
          loaded = this.tableData
        }
        if (loaded.length > 0) {
          // the last is summary row
          loaded.splice(loaded.length - 1, 1)
        }

        let frames = data.data
        frames.forEach(frame => {
          loaded.push(this.buildData(frame))
        })

        if (data.totalSize > 1) {
          loaded.push(this.buildSummary(id, loaded.length, data.totalSize, () => {
            if (loaded.length - 1 /* summary */ < data.totalSize) {
              this.loadData(id, page + 1, parentRowKey, null);
            }
          }))
        }

        if (parentRowKey > 0) {
          if (resolve) {
            resolve(loaded)
          }
        } else {
          this.tableData = loaded
        }

        if (loaded.length === 1 && loaded[0].hasChildren) {
          this.setNextAutoExpandRow(loaded[0])
        } else {
          this.resetAutoExpand()
        }

        this.loading = false
      })
    },
  },
  mounted() {
    this.resetAutoExpand()
    this.loadData(0, 1, 0, null)
  },
  watch: {
    nextAutoExpandRow: function (val) {
      if (val) {
        this.$nextTick(() => this.$refs['table'].store.loadOrToggle(val))
      }
    }
  }
}
</script>