<!--
    Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<template xmlns:v-contextmenu="http://www.w3.org/1999/xhtml">
  <div style="height: 100%">
    <v-contextmenu ref="contextmenu">
      <v-contextmenu-item
              @click="$emit('pathToGCRootsOfObj', contextMenuTargetObjectId, contextMenuTargetObjectLabel)">
        {{$t('jifa.heap.pathToGCRoots')}}
      </v-contextmenu-item>
    </v-contextmenu>

    <el-table :data="tableData"
              :highlight-current-row="false"
              stripe
              :header-cell-style="headerCellStyle"
              :cell-style='cellStyle'
              row-key="rowKey"
              lazy
              :span-method="spanMethod"
              height="100%"
              :indent=8
              v-loading="loading"
    >
      <el-table-column label="Label">
        <template slot-scope="scope">
          <span v-if="scope.row.isRecord" @click="$emit('setSelectedObjectId', scope.row.objectId)"
                @contextmenu="contextMenuTargetObjectId = scope.row.objectId; contextMenuTargetObjectLabel = scope.row.label"
                v-contextmenu:contextmenu
                style="cursor: pointer">
            <img :src="instanceIcon"/> {{scope.row.label}}
          </span>

          <span v-if="scope.row.isSummary">
            <img :src="sumIcon" v-if="records.length >= totalSize"/>
            <img :src="sumPlusIcon" @dblclick="fetchRecords" style="cursor: pointer" v-else/>
            {{ toReadableCount(records.length) }} <strong> / </strong> {{ toReadableCount(totalSize) }}
          </span>
        </template>
      </el-table-column>

      <el-table-column/>
      <el-table-column/>
      <el-table-column/>

      <el-table-column label="position" prop="position" :formatter="toReadableSizeWithUnitFormatter">
      </el-table-column>

      <el-table-column label="limit" prop="limit" :formatter="toReadableSizeWithUnitFormatter">
      </el-table-column>

      <el-table-column label="capacity" prop="capacity" :formatter="toReadableSizeWithUnitFormatter">
      </el-table-column>

    </el-table>
  </div>
</template>

<script>
  import axios from 'axios'
  import {heapDumpService, toReadableCount, toReadableSizeWithUnitFormatter} from '../../util'

  let rowKey = 1
  export default {
    props: ['file'],
    methods: {
      toReadableCount,
      toReadableSizeWithUnitFormatter,
      spanMethod(row) {
        let index = row.columnIndex
        if (index === 0) {
          return [1, 4]
        } else if (index >= 1 && index <= 3) {
          return [0, 0]
        }
        return [1, 1]
      },
      fetchSummary() {
        this.loading = true
        axios.get(heapDumpService(this.file, 'directByteBuffer/summary')).then(resp => {
          this.totalSize = resp.data.totalSize
          this.position = null
          this.limit = resp.data.limit
          this.capacity = resp.data.capacity
          if (this.totalSize > 0) {
            this.fetchRecords()
          } else {
            this.loading = false
          }
        })
      },
      fetchRecords() {
        this.loading = true
        axios.get(heapDumpService(this.file, 'directByteBuffer/records'), {
          params: {
            page: this.nextPage,
            pageSize: this.pageSize,
          }
        }).then(resp => {
          let records = resp.data.data
          records.forEach(item => this.records.push({
            rowKey: rowKey++,

            objectId: item.objectId,
            label: item.label,
            position: item.position,
            limit: item.limit,
            capacity: item.capacity,

            isRecord: true,
          }))

          this.tableData = this.records.concat({
            rowKey: rowKey++,
            position: this.position,
            limit: this.limit,
            capacity: this.capacity,
            isSummary: true
          })

          this.nextPage++
          this.loading = false
        })
      },
    },
    data() {
      return {
        instanceIcon: require('../../assets/heap/objects/instance_obj.gif'),
        sumIcon: require('../../assets/heap/misc/sum.gif'),
        sumPlusIcon: require('../../assets/heap/misc/sum_plus.gif'),
        cellStyle: {padding: '4px', fontSize: '12px'},
        headerCellStyle: {padding: 0, 'font-size': '12px', 'font-weight': 'normal'},

        loading: false,

        position: 0,
        limit: 0,
        capacity: 0,

        totalSize: 0,
        nextPage: 1,
        pageSize: 25,
        records: [],
        tableData: [],

        contextMenuTargetObjectId: null,
        contextMenuTargetObjectLabel: null,
      }
    },
    created() {
      this.fetchSummary()
    }
  }
</script>