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
  <div style="height: 100%; display: flex; flex-direction: column;">
    <el-row>
      <el-col :span="15">
        <el-radio-group v-model="grouping" style="margin-top:10px; margin-left: 10px" @change="changeGrouping"
                        size="mini">
          <el-radio label="NONE">Object</el-radio>
          <el-radio label="BY_CLASS">Class</el-radio>
          <el-radio label="BY_CLASSLOADER">ClassLoader</el-radio>
          <el-radio label="BY_PACKAGE">Package</el-radio>
        </el-radio-group>
      </el-col>
      <el-col :span="9">
        <el-tooltip :content="$t('jifa.searchTip')" placement="bottom" effect="light">
          <el-input size="mini"
                    :placeholder="$t('jifa.searchPlaceholder')"
                    class="input-with-select"
                    v-model="searchText"
                    @keyup.enter.native="doSearch"
                    clearable>
            <el-select slot="prepend" style="width: 100px" v-model="searchType" default-first-option>
              <el-option label="By name" value="by_name"></el-option>
              <el-option label="By percent" value="by_percent"></el-option>
              <el-option label="By shallow heap size" value="by_shallow_size"></el-option>
              <el-option label="By retained heap size" value="by_retained_size"></el-option>
            </el-select>

            <el-button slot="append" :icon="inSearching ? 'el-icon-loading' : 'el-icon-search'"
                       :disabled="inSearching"
                       @click="doSearch"/>
          </el-input>
        </el-tooltip>
      </el-col>
    </el-row>

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
      <v-contextmenu-item v-if="grouping==='NONE'"
          @click="$emit('pathToGCRootsOfObj', contextMenuTargetObjectId, contextMenuTargetObjectLabel)">
        {{$t('jifa.heap.pathToGCRoots')}}
      </v-contextmenu-item>
      <v-contextmenu-item
          @click="$emit('mergePathToGCRootsFromDominatorTree', contextMenuTargetObjectIds == null ? [contextMenuTargetObjectId] : contextMenuTargetObjectIds, contextMenuTargetObjectLabel)">
        {{$t('jifa.heap.mergePathToGCRoots')}}
      </v-contextmenu-item>
    </v-contextmenu>

    <div style="flex-grow: 1; overflow: auto">
      <el-table
          ref='recordTable' :data="tableData"
          :highlight-current-row="true"
          stripe
          :header-cell-style="headerCellStyle"
          :cell-style='cellStyle'
          row-key="rowKey"
          :load="loadChildren"
          @sort-change="sortTable"
          :span-method="spanMethod"
          :indent=8
          lazy
          fit
          v-loading="loading"
      >
        <el-table-column prop="id" label="Class Name" show-overflow-tooltip sortable="custom">
          <template slot-scope="scope">
          <span v-if="scope.row.isResult"
                @click="scope.row.isObjType ? $emit('setSelectedObjectId', scope.row.objectId) : {}"
                style="cursor: pointer"
                @contextmenu="contextMenuTargetObjectId = scope.row.objectId; idPathInResultTree = scope.row.idPathInResultTree; contextMenuTargetObjectLabel = scope.row.label; contextMenuTargetObjectIds = scope.row.objectIds;"
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
            {{ toReadableCount(records.length) }} <strong> / </strong> {{ toReadableCount(totalSize) }}
          </span>

            <span v-if="scope.row.isChildrenSummary">
            <img :src="ICONS.misc.sumIcon" v-if="scope.row.currentSize >= scope.row.totalSize"/>
            <img :src="ICONS.misc.sumPlusIcon"
                 @dblclick="fetchChildren(scope.row.parentRowKey, scope.row.objectId, scope.row.nextPage, scope.row.idPathInResultTree, scope.row.resolve)"
                 style="cursor: pointer"
                 v-else/>
            {{ toReadableCount(scope.row.currentSize) }} <strong> / </strong> {{ toReadableCount(scope.row.totalSize) }}
          </span>
          </template>
        </el-table-column>
        <el-table-column/>
        <el-table-column/>
        <el-table-column/>
        <el-table-column/>
        <el-table-column/>

        <el-table-column v-if="grouping!=='NONE'" label="Objects" prop="Objects" sortable="custom">
          <template slot-scope="scope">
            {{ grouping !== "NONE" ? toReadableCount(scope.row.objects) : '' }}
          </template>
        </el-table-column>

        <el-table-column label="Shallow Heap" prop="shallowHeap" sortable="custom"
                         :formatter="toReadableSizeWithUnitFormatter">
        </el-table-column>
        <el-table-column label="Retained Heap" prop="retainedHeap" sortable="custom"
                         :formatter="toReadableSizeWithUnitFormatter">
        </el-table-column>
        <el-table-column label="Percentage" prop="percent" sortable="custom">
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
  import axios from 'axios'
  import {getIcon, ICONS} from "./IconHealper";
  import {heapDumpService, toReadableCount, toReadableSizeWithUnitFormatter} from "../../util";

  let rowKey = 1

  export default {
    props: ['file'],
    data() {
      return {
        ICONS,
        loading: false,
        cellStyle: {padding: '4px', fontSize: '12px'},
        headerCellStyle: {padding: 0, 'font-size': '12px', 'font-weight': 'normal'},

        nextPage: 1,
        pageSize: 25,
        totalSize: 0,
        records: [],
        tableData: [],
        contextMenuTargetObjectId: null,
        contextMenuTargetObjectLabel: null,
        contextMenuTargetObjectIds: null,

        // grouping support
        grouping: 'NONE',

        // sorting support
        ascendingOrder: false,
        sortBy: 'retainedHeap',

        // query support
        searchText: '',
        inSearching: false,
        searchType: 'by_name'
      }
    },
    methods: {
      toReadableCount,
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
      clear() {
        this.nextPage = 1
        this.totalSize = 0
        this.records = []
      },
      changeGrouping() {
        // sort by Objects is not valid for grouping by none
        if (this.grouping === 'NONE' && this.sortBy === 'Objects') {
          this.sortBy = 'retainedHeap'
        }
        this.clear()
        this.fetchNextPageData()
      },
      sortTable(val) {
        this.sortBy = val.prop;
        this.nextPage = 1
        this.totalSize = 0
        this.records = []
        this.ascendingOrder = val.order === 'ascending';
        this.fetchNextPageData();
      },
      getIconWrapper(gCRoot, objectType, isObj) {
        return getIcon(gCRoot, objectType, isObj)
      },
      loadChildren(tree, treeNode, resolve) {
        this.fetchChildren(tree.rowKey, tree.objectId, 1, tree.idPathInResultTree, resolve)
      },
      doSearch() {
        this.clear();
        this.fetchNextPageData();
      },
      fetchChildren(parentRowKey, objectId, page, idPathInResultTree, resolve) {
        this.loading = true
        axios.get(heapDumpService(this.file, 'dominatorTree/children'), {
          params: {
            parentObjectId: objectId,
            page: page,
            pageSize: this.pageSize,
            grouping: this.grouping,
            idPathInResultTree: JSON.stringify(idPathInResultTree),
            sortBy: this.sortBy,
            ascendingOrder: this.ascendingOrder,
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
              isObjType: d.objType,
              objectType: d.objectType,
              icon: this.getIconWrapper(d.gCRoot, d.objectType, d.objType),
              label: d.label,
              suffix: d.suffix,
              objects: d.objects,
              objectIds: d.objectIds,
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
            grouping: this.grouping,
            sortBy: this.sortBy,
            ascendingOrder: this.ascendingOrder,
            searchText: this.searchText,
            searchType: this.searchType,
          }
        }).then(resp => {
          this.totalSize = resp.data.totalSize
          let data = resp.data.data
          data.forEach(d => {
            this.records.push({
              rowKey: rowKey++,
              objectId: d.objectId,
              isObjType: d.objType,
              objectType: d.objectType,
              icon: this.getIconWrapper(d.gCRoot, d.objectType, d.objType),
              label: d.label,
              suffix: d.suffix,
              objects: d.objects,
              objectIds: d.objectIds,
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
