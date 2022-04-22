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
  <el-table :data="systemProperties.filter(data => !keyword || data.key.toLowerCase().includes(keyword.toLowerCase())
                                             || data.value.toLowerCase().includes(keyword.toLowerCase()))"
            :highlight-current-row="false"
            height="100%"
            stripe
            v-loading="loading"
            :header-cell-style="headerCellStyle"
            :cell-style='cellStyle'
            :span-method="tableSpanMethod">
    <el-table-column label="Key" prop="key" width="400px">
    </el-table-column>
    <el-table-column label="Value" prop="value">
    </el-table-column>
    <el-table-column align="right">
      <template slot="header" slot-scope="scope">
        <el-input size="mini" v-model="keyword" style="width: 50%;"
                  :placeholder="$t('jifa.typeKeyWord')" clearable/>
      </template>
    </el-table-column>
  </el-table>
</template>

<script>
  import axios from 'axios'
  import {heapDumpService} from '../../util'

  export default {
    props: ['file'],
    methods: {
      fetchSystemProperty() {
        if (this.systemProperties.length > 0) {
          return
        }
        this.loading = true
        axios.get(heapDumpService(this.file, 'systemProperties')).then(resp => {
          let sps = []
          for (let i in resp.data) {
            sps.push({
              key: i,
              value: resp.data[i]
            })
          }
          this.systemProperties = sps
          this.loading = false
        })
      },
      tableSpanMethod(i) {
        if (i.columnIndex === 0) {
          return [1, 1];
        } else if (i.columnIndex === 1) {
          return [1, 2];
        } else {
          return [0, 0]
        }
      },
    },
    data() {
      return {
        keyword: '',
        systemProperties: [],
        cellStyle: {padding: '0'},
        headerCellStyle: {'font-size': '12px', 'font-weight': 'normal'},
        loading: false,
      }
    },
    created() {
      this.fetchSystemProperty()
    }
  }
</script>
