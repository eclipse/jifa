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
  <el-table
          ref="fieldsTable"
          v-loading="loading"
          :data="tableData"
          :span-method="spanMethod"
          show-header
          highlight-current-row
          stripe
          :cell-style='cellStyle'
          :header-cell-style="headerCellStyle"
          height="100%"
          empty-text=" "
  >
    <el-table-column show-overflow-tooltip label="Type" width="70px">
      <template slot-scope="scope">
        <p style="font-size: 12px; margin: 0 auto;" v-if="!scope.row.isSummaryItem">
          {{scope.row.type}}
        </p>
        <span v-if="scope.row.isSummaryItem">
              <img :src="ICONS.misc.sumIcon" v-if="fields.length >= totalSize"/>
              <img :src="ICONS.misc.sumPlusIcon" @dblclick="fetchNextPage" style="cursor: pointer" v-else/>
              {{ fields.length }} <strong> / </strong> {{totalSize}}
            </span>
      </template>
    </el-table-column>
    <el-table-column show-overflow-tooltip label="Name" width="120px" align="center">
      <template slot-scope="scope">
        <p style="font-size: 12px; margin: 0 auto;">
          {{ scope.row.name}}
        </p>
      </template>
    </el-table-column>

    <el-table-column show-overflow-tooltip
                     label="Value" width="500px">
      <template slot-scope="scope">
        <p style="font-size: 12px; margin: 0 auto;">
          {{ scope.row.value}}
        </p>
      </template>
    </el-table-column>
  </el-table>
</template>

<script>
  import axios from 'axios'
  import {heapDumpService} from '../../util'
  import {ICONS} from "./IconHealper";

  export default {
    props: {
      file: String,
      objectId: Number,
      static: {type: Boolean, default: false},
    },
    data() {
      return {
        ICONS,
        loading: false,
        fields: [],
        tableData: [],
        pageSize: 25,
        nextPage: 0,
        totalSize: 0,
        cellStyle: {padding: '4px', fontSize: '12px'},
        headerCellStyle: {padding: 0, 'font-size': '12px', 'font-weight': 'normal'},
      }
    },
    methods: {
      spanMethod({row}) {
        if (row.isSummaryItem) {
          return [1, 3]
        }
        return [1, 1]
      },
      convert(fieldType) {
        switch (fieldType) {
          case 2 :
            return 'ref'
          case 4 :
            return 'boolean'
          case 5 :
            return 'char'
          case 6:
            return 'float'
          case 7:
            return 'double'
          case 8:
            return 'byte'
          case 9:
            return 'short'
          case 10:
            return 'int'
          case 11:
            return 'long'
        }
        return 'unknown'
      },
      fetchNextPage() {
        this.fetchFields()
      },
      fetchFields() {
        if (!this.objectId) {
          return
        }
        this.loading = true
        let url = heapDumpService(this.file, this.static ? 'inspector/staticFields' : 'inspector/fields')
        axios.get(url, {
          params: {
            objectId: this.objectId,
            page: this.nextPage,
            pageSize: this.pageSize,
          }
        }).then(resp => {
          this.totalSize = resp.data.totalSize
          if (this.totalSize > 0) {
            let res = resp.data.data
            res.forEach(f => {
                  this.fields.push({
                    type: this.convert(f.fieldType),
                    name: f.name,
                    value: f.fieldType === 2 && !f.value ? 'null' : f.value
                  })
                }
            )

            this.tableData = this.fields.concat({
              isSummaryItem: true
            })

            this.nextPage++;
            this.loading = false
          } else {
            this.clear()
            this.loading = false
          }
        })
      },
      clear() {
        this.fields = []
        this.tableData = []
        this.totalSize = 0
      },
    },
    watch: {
      objectId(id) {
        this.nextPage = 1
        this.clear()
        if (id >= 0) {
          this.fetchFields()
        }
      }
    },
    mounted() {
      this.nextPage = 1
      this.clear()
      this.fetchFields()
    }
  }
</script>
