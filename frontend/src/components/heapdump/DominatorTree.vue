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
  <div style="height: 100%; position: relative">
    <div style="height: 40px; padding-top: 10px" align="center">
      <el-radio-group v-model="grouping" @change="changeGrouping">
        <el-radio label="NONE">Object</el-radio>
        <el-radio label="BY_CLASS">Class</el-radio>
        <!--<el-radio label="BY_CLASSLOADER">Class Loader</el-radio>-->
        <!--<el-radio label="BY_PACKAGE">Package</el-radio>-->
      </el-radio-group>
    </div>

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
      <v-contextmenu-item divider></v-contextmenu-item>
      <v-contextmenu-item
              @click="$emit('pathToGCRootsOfObj', contextMenuTargetObjectId, contextMenuTargetObjectLabel)">
        {{$t('jifa.heap.pathToGCRoots')}}
      </v-contextmenu-item>
    </v-contextmenu>

    <div v-loading="loading" style="position: absolute; top: 40px; left: 0; right: 0; bottom: 0;">
      <el-table
              ref='recordTable' :data="tableData"
              :highlight-current-row="true"
              stripe
              :header-cell-style="headerCellStyle"
              :cell-style='cellStyle'
              row-key="rowKey"
              :load="loadChildren"
              height="100%"
              :span-method="spanMethod"
              :indent=8
              lazy
              fit>
        <el-table-column label="Class Name" show-overflow-tooltip>
          <template slot-scope="scope">
            <span v-if="scope.row.isResult" @click="$emit('setSelectedObjectId', scope.row.objectId)"
                  style="cursor: pointer"
                  @contextmenu="contextMenuTargetObjectId = scope.row.objectId; idPathInResultTree = scope.row.idPathInResultTree; contextMenuTargetObjectLabel = scope.row.label"
                  v-contextmenu:contextmenu>
              <img :src="scope.row.icon" style="margin-right: 5px"/>
                {{ scope.row.label }}
              <span style="font-weight: bold; color: #909399">
                {{ scope.row.suffix }}
              </span>
            </span>

            <span v-if="scope.row.isSummaryItem">
              <img :src="ICONS.misc.sumIcon" v-if="records.length >= totalSize"/>
              <img :src="ICONS.misc.sumPlusIcon" @dblclick="fetchNextPageData" style="cursor: pointer" v-else/>
              {{ records.length }} <strong> / </strong> {{totalSize}}
            </span>

            <span v-if="scope.row.isChildrenSummary">
              <img :src="ICONS.misc.sumIcon" v-if="scope.row.currentSize >= scope.row.totalSize"/>
              <img :src="ICONS.misc.sumPlusIcon"
                   @dblclick="fetchChildren(scope.row.parentRowKey, scope.row.objectId, scope.row.nextPage, scope.row.idPathInResultTree, scope.row.resolve)"
                   style="cursor: pointer"
                   v-else/>
              {{ scope.row.currentSize }} <strong> / </strong> {{ scope.row.totalSize }}
            </span>
          </template>
        </el-table-column>
        <el-table-column/>
        <el-table-column/>
        <el-table-column/>
        <el-table-column/>
        <el-table-column/>

        <el-table-column v-if="grouping!=='NONE'" label="Objects">
          <template slot-scope="scope">
            {{ grouping !== "NONE" ? scope.row.objects : ''}}
          </template>
        </el-table-column>

        <el-table-column label="Shallow Heap" prop="shallowHeap">
        </el-table-column>
        <el-table-column label="Retained Heap" prop="retainedHeap">
        </el-table-column>
        <el-table-column label="Percentage" prop="percent">
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
  import axios from 'axios'
  import {getIcon, ICONS} from "./IconHealper";
  import {heapDumpService} from "../../util";

  let rowKey = 1

  export default {
    props: ['file'],
    data() {
      return {
        ICONS,
        loading: false,
        cellStyle: {padding: '4px', fontSize: '12px'},
        headerCellStyle: {padding: 0, 'font-size': '12px', 'font-weight': 'normal'},
        grouping: 'NONE',

        nextPage: 1,
        pageSize: 25,
        totalSize: 0,
        records: [],
        tableData: [],
        contextMenuTargetObjectId: null,
        contextMenuTargetObjectLabel: null,
      }
    },
    methods: {
      spanMethod(row) {
        let index = row.columnIndex
        if (index === 0) {
          return [1, 6]
        } else if (index >= 1 && index <= 5) {
          return [0, 0]
        }
        return [1, 1]
      },
      clear() {
        this.nextPage = 1
        this.totalSize = 0
        this.records = []
      },
      changeGrouping() {
        this.clear()
        this.fetchNextPageData()
      },
      getIconWrapper(gCRoot, objectType) {
        if (this.grouping === 'BY_CLASS') {
          return ICONS.objects.class
        }
        return getIcon(gCRoot, objectType)
      },
      loadChildren(tree, treeNode, resolve) {
        this.fetchChildren(tree.rowKey, tree.objectId, 1, tree.idPathInResultTree, resolve)
      },
      fetchChildren(parentRowKey, objectId, page, idPathInResultTree, resolve) {
        this.loading = true
        axios.get(heapDumpService(this.file, 'dominatorTree/children'), {
          params: {
            parentObjectId: objectId,
            page: page,
            pageSize: this.pageSize,
            grouping: this.grouping,
            idPathInResultTree: JSON.stringify(idPathInResultTree)
          }
        }).then(resp => {
          let loadedLen = 0;
          let loaded = this.$refs['recordTable'].store.states.lazyTreeNodeMap[parentRowKey]
          let callResolve = false
          if (loaded) {
            loadedLen = loaded.length
            if (loadedLen > 0) {
              loaded.splice(--loadedLen, 1)
            }
          } else {
            loaded = []
            callResolve = true;
          }

          let res = resp.data.data
          res.forEach(d => {
            let idPathInResultTreeCopy = Array.from(idPathInResultTree);
            idPathInResultTreeCopy.push(d.objectId);
            loaded.push({
              rowKey: rowKey++,
              objectId: d.objectId,
              icon: this.getIconWrapper(d.gCRoot, d.objectType),
              label: d.label,
              suffix: d.suffix,
              objects: d.objects,
              shallowHeap: d.shallowSize,
              retainedHeap: d.retainedSize,
              percent: (d.percent * 100).toFixed(2) + '%',
              hasChildren: true,
              isResult: true,
              idPathInResultTree: idPathInResultTreeCopy
            })
          })

          loaded.push({
            rowKey: rowKey++,
            objectId: objectId,
            parentRowKey: parentRowKey,
            isChildrenSummary: true,
            nextPage: page + 1,
            currentSize: loadedLen + res.length,
            totalSize: resp.data.totalSize,
            resolve: resolve,
            idPathInResultTree: idPathInResultTree
          })

          if (callResolve) {
            resolve(loaded)
          }
          this.loading = false
        })
      },
      fetchNextPageData() {
        this.loading = true
        axios.get(heapDumpService(this.file, 'dominatorTree/roots'), {
          params: {
            page: this.nextPage,
            pageSize: this.pageSize,
            grouping: this.grouping
          }
        }).then(resp => {
          this.totalSize = resp.data.totalSize
          let data = resp.data.data
          data.forEach(d => {
            this.records.push({
              rowKey: rowKey++,
              objectId: d.objectId,
              icon: this.getIconWrapper(d.gCRoot, d.objectType),
              label: d.label,
              suffix: d.suffix,
              objects: d.objects,
              shallowHeap: d.shallowSize,
              retainedHeap: d.retainedSize,
              percent: (d.percent * 100).toFixed(2) + '%',
              hasChildren: true,
              isResult: true,
              idPathInResultTree: [d.objectId]
            })
          })
          this.tableData = this.records.concat({
            rowKey: rowKey++,
            isSummaryItem: true
          })
          this.nextPage++
          this.loading = false
        })
      },
    },
    created() {
      this.fetchNextPageData()
    }
  }
</script>