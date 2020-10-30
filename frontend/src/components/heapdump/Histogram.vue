<!--
    Copyright (c) 2020 Contributors to the Eclipse Foundation

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
      <v-contextmenu-submenu :title="$t('jifa.heap.ref.object.label')">
        <v-contextmenu-item
                @click="$emit('outgoingRefsOfObj', contextMenuTargetObjectId, contextMenuTargetObjectLabel)">
          {{$t('jifa.heap.ref.object.outgoing')}}
        </v-contextmenu-item>
        <v-contextmenu-item
                @click="$emit('incomingRefsOfObj', contextMenuTargetObjectId, contextMenuTargetObjectLabel)">
          {{$t('jifa.heap.ref.object.incoming')}}
        </v-contextmenu-item>
      </v-contextmenu-submenu>
      <v-contextmenu-submenu :title="$t('jifa.heap.ref.type.label')">
        <v-contextmenu-item
                @click="$emit('outgoingRefsOfClass', contextMenuTargetObjectId, contextMenuTargetObjectLabel)">
          {{$t('jifa.heap.ref.type.outgoing')}}
        </v-contextmenu-item>
        <v-contextmenu-item
                @click="$emit('incomingRefsOfClass', contextMenuTargetObjectId, contextMenuTargetObjectLabel)">
          {{$t('jifa.heap.ref.type.incoming')}}
        </v-contextmenu-item>
      </v-contextmenu-submenu>
      <v-contextmenu-item divider/>
      <v-contextmenu-item
              @click="$emit('mergePathToGCRootsFromHistogram', contextMenuTargetObjectId, contextMenuTargetObjectLabel)">
        {{$t('jifa.heap.mergePathToGCRoots')}}
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
      <el-table-column :label="label">
        <template slot-scope="scope">
          <span v-if="scope.row.isRecord" @click="$emit('setSelectedObjectId', scope.row.objectId)"
                @contextmenu="contextMenuTargetObjectId = scope.row.objectId; contextMenuTargetObjectLabel = scope.row.label"
                v-contextmenu:contextmenu
                style="cursor: pointer">
            <img :src="scope.row.icon"/> {{scope.row.label}}
          </span>

          <span v-if="scope.row.isSummary">
            <img :src="sumIcon" v-if="records.length >= totalSize"/>
            <img :src="sumPlusIcon" @dblclick="fetchHistogram" style="cursor: pointer" v-else/>
            {{ records.length }} <strong> / </strong> {{totalSize}}
          </span>
        </template>
      </el-table-column>


      <el-table-column v-if="generationInfoAvailable"/>
      <el-table-column v-if="generationInfoAvailable"/>
      <el-table-column v-if="generationInfoAvailable"/>

      <el-table-column v-if="!generationInfoAvailable"/>
      <el-table-column v-if="!generationInfoAvailable"/>
      <el-table-column v-if="!generationInfoAvailable"/>
      <el-table-column v-if="!generationInfoAvailable"/>
      <el-table-column v-if="!generationInfoAvailable"/>

      <el-table-column label="Objects" prop="numberOfObjects">
      </el-table-column>

      <el-table-column label="Shallow Heap" prop="shallowSize">
      </el-table-column>

      <el-table-column label="Objects(Y)" prop="numberOfYoungObjects" v-if="generationInfoAvailable">
      </el-table-column>

      <el-table-column label="Shallow Heap(Y) " prop="shallowSizeOfYoung" v-if="generationInfoAvailable">
      </el-table-column>

      <el-table-column label="Objects(O)" prop="numberOfOldObjects" v-if="generationInfoAvailable">
      </el-table-column>

      <el-table-column label="Shallow Heap(O)" prop="shallowSizeOfOld" v-if="generationInfoAvailable">
      </el-table-column>

      <el-table-column label="Retained Heap">
        <template slot-scope="scope">
          <span v-if="scope.row.retainedSize < 0">
            >= {{ -scope.row.retainedSize }}
          </span>

          <span v-else>
            {{ scope.row.retainedSize }}
          </span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>

  import axios from 'axios'
  import {heapDumpService} from '../../util'

  let rowKey = 1
  export default {
    props: ['file', 'generationInfoAvailable'],
    methods: {
      spanMethod(row) {
        let index = row.columnIndex
        if (this.generationInfoAvailable) {
          if (index === 0) {
            return [1, 4]
          } else if (index >= 1 && index <= 3) {
            return [0, 0]
          }
          return [1, 1]
        } else {
          if (index === 0) {
            return [1, 6]
          } else if (index >= 1 && index <= 5) {
            return [0, 0]
          }
          return [1, 1]
        }
      },
      fetchHistogram() {
        this.loading = true
        axios.get(heapDumpService(this.file, 'histogram'), {
          params: {
            groupingBy: this.groupingBy,
            page: this.nextPage,
            pageSize: this.pageSize,
          }
        }).then(resp => {
          let records = resp.data.data
          this.totalSize = resp.data.totalSize
          records.forEach(record =>
              this.records.push({
                rowKey: rowKey++,
                objectId: record.objectId,
                label: record.label,
                icon: this.classIcon,
                numberOfObjects: record.numberOfObjects,
                shallowSize: record.shallowSize,

                numberOfYoungObjects: record.numberOfYoungObjects,
                shallowSizeOfYoung: record.shallowSizeOfYoung,

                numberOfOldObjects: record.numberOfOldObjects,
                shallowSizeOfOld: record.shallowSizeOfOld,

                retainedSize: record.retainedSize,
                isRecord: true
              }))

          this.tableData = this.records.concat({
            rowKey: rowKey++,
            isSummary: true
          })
          this.nextPage++
          this.loading = false
        })
      },
    },
    data() {
      return {
        loading: false,
        classIcon: require('../../assets/heap/objects/class.gif'),
        sumIcon: require('../../assets/heap/misc/sum.gif'),
        sumPlusIcon: require('../../assets/heap/misc/sum_plus.gif'),
        label: 'Class Name',
        groupingBy: 'by_class',
        cellStyle: {padding: '4px', fontSize: '12px'},
        headerCellStyle: {padding: 0, 'font-size': '12px', 'font-weight': 'normal'},

        records: [],
        tableData: [],
        nextPage: 1,
        pageSize: 25,
        totalSize: 0,

        contextMenuTargetObjectId: null,
        contextMenuTargetObjectLabel: null,
      }
    },
    created() {
      this.fetchHistogram()
    }
  }
</script>