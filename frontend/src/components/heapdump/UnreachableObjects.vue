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
  <div style="height: 100%">
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
      <el-table-column label="Class Name">
        <template slot-scope="scope">
          <span v-if="scope.row.isRecord" @click="$emit('setSelectedObjectId', scope.row.objectId)"
                style="cursor: pointer">
            <img :src="classIcon"/> {{scope.row.className}}
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
      <el-table-column/>
      <el-table-column/>

      <el-table-column label="Objects" prop="objects" :formatter="toReadableCountFormatter">
      </el-table-column>

      <el-table-column label="Shallow Heap" prop="shallowSize" :formatter="toReadableSizeWithUnitFormatter">
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
  import axios from 'axios'
  import {heapDumpService, toReadableCount, toReadableCountFormatter, toReadableSizeWithUnitFormatter} from '../../util'

  let rowKey = 1
  export default {
    props: ['file'],
    methods: {
      toReadableCount,
      toReadableCountFormatter,
      toReadableSizeWithUnitFormatter,
      spanMethod(row) {
        let index = row.columnIndex
        if (index === 0) {
          return [1, 6]
        } else if (index >= 1 && index <= 5) {
          return [0, 0]
        }
        return [1, 1]
      },
      fetchSummary() {
        this.loading = true
        axios.get(heapDumpService(this.file, 'unreachableObjects/summary')).then(resp => {
          this.totalSize = resp.data.totalSize
          this.objects = resp.data.objects
          this.shallowSize = resp.data.shallowSize
          if (this.totalSize > 0) {
            this.fetchRecords()
          } else {
            this.loading = false
          }
        })
      },
      fetchRecords() {
        this.loading = true
        axios.get(heapDumpService(this.file, 'unreachableObjects/records'), {
          params: {
            page: this.nextPage,
            pageSize: this.pageSize,
          }
        }).then(resp => {
          let records = resp.data.data
          records.forEach(record => this.records.push({
            rowKey: rowKey++,

            objectId: record.objectId,
            className: record.className,
            objects: record.objects,
            shallowSize: record.shallowSize,

            isRecord: true,
          }))

          this.tableData = this.records.concat({
            rowKey: rowKey++,
            objects: this.objects,
            shallowSize: this.shallowSize,
            isSummary: true
          })

          this.nextPage++
          this.loading = false
        })
      },
    },
    data() {
      return {
        classIcon: require('../../assets/heap/objects/class.gif'),
        sumIcon: require('../../assets/heap/misc/sum.gif'),
        sumPlusIcon: require('../../assets/heap/misc/sum_plus.gif'),
        cellStyle: {padding: '4px', fontSize: '12px'},
        headerCellStyle: {padding: 0, 'font-size': '12px', 'font-weight': 'normal'},

        loading: false,

        objects: 0,
        shallowSize: 0,

        totalSize: 0,
        nextPage: 1,
        pageSize: 25,
        records: [],
        tableData: [],
      }
    },
    created() {
      this.fetchSummary()
    }
  }
</script>