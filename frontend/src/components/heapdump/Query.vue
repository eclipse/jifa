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

    <div style="height: 45px; margin-top: 5px">
      <el-autocomplete
              v-model="query"
              :fetch-suggestions="queryHistory"
              :disabled="disabledInput"
              placeholder="Query ..."
              :trigger-on-focus="false"
              prefix-icon="el-icon-edit"
              @keyup.enter.native="search"
              clearable
              style="display: flex"
      >
        <el-select slot="prepend" v-model="queryType" style="width: 140px" default-first-option>
          <el-option label="OQL" value="oql" />
          <el-option label="Calcite SQL" value="sql" />
        </el-select>
        <template slot="append">
          <el-button :icon="searching ? 'el-icon-loading':'el-icon-search'" :disabled="searching" @click="search">
          </el-button>
        </template>
      </el-autocomplete>
    </div>

    <div align="left" style="height: 25px; margin-bottom: 5px" v-if="queryType === 'oql'">
      <a href="https://help.eclipse.org/oxygen/index.jsp?topic=%2Forg.eclipse.mat.ui.help%2Freference%2Foqlsyntax.html&cp=66_4_2"
         target="_blank" style="font-size: 12px; font-weight: bold; color: #909399; text-decoration: underline">
        > Click to get detailed OQL Help documents.
      </a>
    </div>

    <div align="left" style="height: 25px; margin-bottom: 5px" v-if="queryType === 'sql'">
      <a href="https://github.com/vlsi/mat-calcite-plugin#sample"
         target="_blank" style="font-size: 12px; font-weight: bold; color: #909399; text-decoration: underline">
        > Click to get detailed Calcite SQL Help.
      </a>
    </div>

    <div v-loading="loading" style="position: absolute; top: 80px; left: 0; right: 0; bottom: 0;">
      <el-table v-if="isTreeResult"
                ref='treeResultTable' :data="treeTableData"
                :highlight-current-row="false"
                stripe
                :header-cell-style="headerCellStyle"
                :cell-style='cellStyle'
                row-key="rowKey"
                lazy
                @sort-change="sortTree"
                :indent=8
                height="100%"
                :load="loadOutbounds">
        <el-table-column label="Class Name" width="800px" show-overflow-tooltip prop="label" sortable="custom">
          <template slot-scope="scope">
            <span v-if="scope.row.isResult" @click="$emit('setSelectedObjectId', scope.row.objectId)"
                  style="cursor: pointer"
                  @contextmenu="contextMenuTargetObjectId = scope.row.objectId; contextMenuTargetObjectLabel = scope.row.label"
                  v-contextmenu:contextmenu>
              <img :src="scope.row.icon" style="margin-right: 5px"/>
              <strong>{{ scope.row.prefix }}</strong>
              {{ scope.row.label }}
              <span style="font-weight: bold; color: #909399">
                {{ scope.row.suffix }}
              </span>
            </span>

            <span v-if="scope.row.isOutboundsSummary">
              <img :src="ICONS.misc.sumIcon" v-if="scope.row.currentSize >= scope.row.totalSize"/>
              <img :src="ICONS.misc.sumPlusIcon"
                   @dblclick="fetchOutbounds(scope.row.parentRowKey, scope.row.objectId, scope.row.nextPage, scope.row.resolve)"
                   style="cursor: pointer"
                   v-else/>
              {{ toReadableCount(scope.row.currentSize) }} <strong> / </strong> {{ toReadableCount(scope.row.totalSize) }}
            </span>

            <span v-if="scope.row.isSummaryItem">
              <img :src="ICONS.misc.sumIcon" v-if="treeResult.length >= totalSize"/>
              <img :src="ICONS.misc.sumPlusIcon" @dblclick="fetchResult(scope.row.query)" style="cursor: pointer" v-else/>
              {{ toReadableCount(treeResult.length) }} <strong> / </strong> {{ toReadableCount(totalSize) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="Shallow Heap" prop="shallowHeap" sortable="custom" :formatter="toReadableSizeWithUnitFormatter">
        </el-table-column>
        <el-table-column label="Retained Heap" prop="retainedHeap" sortable="custom" :formatter="toReadableSizeWithUnitFormatter">
        </el-table-column>
      </el-table>

      <el-table v-if="isTableResult"
                ref='tableResultTable' :data="tableTableData"
                :highlight-current-row="false"
                stripe
                :header-cell-style="headerCellStyle"
                :cell-style='cellStyle'
                row-key="rowKey"
                lazy
                :indent=8
                height="100%">

        <el-table-column v-for="(column, index) in tableResultColumns" :label="column" v-bind:key="column">
          <template slot-scope="scope">
          <span v-if="scope.row.isResult" @click="$emit('setSelectedObjectId', scope.row.objectId)"
                style="cursor: pointer"
                @contextmenu="contextMenuTargetObjectId = scope.row.objectId; contextMenuTargetObjectLabel = scope.row.values[index] ? scope.row.values[index] : 'null'"
                v-contextmenu:contextmenu>
            {{ scope.row.values[index] ? scope.row.values[index] : 'null' }}
          </span>

            <span v-if="scope.row.isSummaryItem && index === 0">
            <img :src="ICONS.misc.sumIcon" v-if="tableResult.length >= totalSize"/>
            <img :src="ICONS.misc.sumPlusIcon" @dblclick="fetchResult(scope.row.query)" style="cursor: pointer" v-else/>
            {{ toReadableCount(tableResult.length) }} <strong> / </strong> {{ toReadableCount(totalSize) }}
          </span>
          </template>
        </el-table-column>
      </el-table>


      <el-input v-if="isTextResult"
                v-model="textResult"
                type="textarea"
                readonly
                autosize>
      </el-input>
    </div>
  </div>
</template>

<script>
  import axios from 'axios'
  import {getOutboundIcon, ICONS} from "./IconHealper";
  import {heapDumpService, toReadableCount, toReadableSizeWithUnit, toReadableSizeWithUnitFormatter} from "../../util";

  let rowKey = 1

  // query result type
  const TREE = 1
  const TABLE = 2
  const TEXT = 3

  export default {
    props: ['file', 'preparedQuery', 'preparedQueryType'],
    data() {
      return {
        ICONS,
        cellStyle: {padding: '4px', fontSize: '12px'},
        headerCellStyle: {padding: 0, 'font-size': '12px', 'font-weight': 'normal'},
        searching: false,
        loading: false,
        query: '',
        nextPage: 1,
        pageSize: 25,
        totalSize: 0,
        resultType: 0,

        contextMenuTargetObjectId: null,
        contextMenuTargetObjectLabel: null,

        treeResult: [],
        treeTableData: [],

        tableResult: [],
        tableResultColumns: [],
        tableTableData: [],

        textResult: '',

        isTreeResult: false,
        isTableResult: false,
        isTextResult: false,

        historyQueries: [],
        treeSortBy:'retainedHeap',
        treeAscendingOrder:true,

        disabledInput: false,

        queryType: 'oql',
      }
    },
    methods: {
      toReadableCount,
      toReadableSizeWithUnit,
      toReadableSizeWithUnitFormatter,
      adjustDataByResultType(type) {
        this.isTreeResult = type === TREE
        this.isTableResult = type === TABLE
        this.isTextResult = type === TEXT
      },

      sortTree(val){
        if (this.query) {
          this.treeSortBy = val.prop;
          this.treeAscendingOrder = val.order === 'ascending';
          this.searching = true
          this.clear()
          this.fetchResult(this.query)
        }
      },

      fetchResult(query) {
        if (!query || query.length === 0) {
          return
        }
        this.loading = true
        axios.get(heapDumpService(this.file, this.queryType), {
          params: {
            oql: this.queryType === 'oql' ? query : undefined,
            sql: this.queryType === 'sql' ? query : undefined,
            page: this.nextPage,
            pageSize: this.pageSize,
            sortBy: this.treeSortBy,
            ascendingOrder: this.treeAscendingOrder
          }
        }).then(resp => {
          this.adjustDataByResultType(resp.data.type)
          if (this.isTreeResult) {
            this.putHistory(query)
            this.totalSize = resp.data.pv.totalSize
            let data = resp.data.pv.data
            data.forEach(d => {
              this.treeResult.push({
                rowKey: rowKey++,
                icon: getOutboundIcon(d.gCRoot, d.objectType),
                prefix: d.prefix,
                label: d.label,
                suffix: d.suffix,
                shallowHeap: d.shallowSize,
                retainedHeap: d.retainedSize,
                hasChildren: d.hasOutbound,
                objectId: d.objectId,
                isResult: true
              })
            })
            this.treeTableData = this.treeResult.concat({
              rowKey: rowKey++,
              query: query,
              isSummaryItem: true
            })
            this.nextPage++
          } else if (this.isTableResult) {
            this.putHistory(query)
            this.totalSize = resp.data.pv.totalSize
            this.tableResultColumns = resp.data.columns
            let data = resp.data.pv.data
            data.forEach(d => {
              this.tableResult.push({
                rowKey: rowKey++,
                objectId: d.objectId,
                values: d.values,
                hasChildren: false,
                isResult: true
              })
            })
            this.tableTableData = this.tableResult.concat({
              rowKey: rowKey++,
              query: query,
              isSummaryItem: true
            })
            this.nextPage++
          } else if (this.isTextResult) {
            this.textResult = resp.data.text
          }
          this.searching = false
          this.loading = false
        })
      },
      loadOutbounds(tree, treeNode, resolve) {
        this.fetchOutbounds(tree.rowKey, tree.objectId, 1, resolve)
      },
      fetchOutbounds(parentRowKey, objectId, page, resolve) {
        this.loading = true
        axios.get(heapDumpService(this.file, 'outbounds'), {
          params: {
            objectId: objectId,
            page: page,
            pageSize: this.pageSize,
          }
        }).then(resp => {
          let loadedLen = 0;
          let loaded = this.$refs['treeResultTable'].store.states.lazyTreeNodeMap[parentRowKey]
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
            loaded.push({
              rowKey: rowKey++,
              icon: getOutboundIcon(d.gCRoot, d.objectType),
              prefix: d.prefix,
              label: d.label,
              suffix: d.suffix,
              shallowHeap: d.shallowSize,
              retainedHeap: d.retainedSize,
              hasChildren: d.hasOutbound,
              objectId: d.objectId,
              isResult: true
            })
          })

          loaded.push({
            rowKey: rowKey++,
            objectId: objectId,
            parentRowKey: parentRowKey,
            isOutboundsSummary: true,
            nextPage: page + 1,
            currentSize: loadedLen + res.length,
            totalSize: resp.data.totalSize,
            resolve: resolve,
          })

          if (callResolve) {
            resolve(loaded)
          }
          this.loading = false
        })
      },
      clear() {
        this.nextPage = 1
        this.totalSize = 0

        this.treeResult = []
        this.treeTableData = []

        this.tableResult = []
        this.tableResultColumns = []
        this.tableTableData = []
        this.textResult = ''
      },

      search() {
        if (this.query && !this.searching) {
          this.searching = true
          this.clear()
          this.fetchResult(this.query)
        }
      },

      putHistory(query) {
        let target = query.trim()
        for (let i = 0; i < this.historyQueries.length; i++) {
          if (this.historyQueries[i].value === target) {
            return
          }
        }

        this.historyQueries.push({value: target})
        if (this.historyQueries.length > 11) {
          this.historyQueries.shift()
        }
      },

      queryHistory(queryString, cb) {
        let history = this.historyQueries;
        let results = queryString ? history.filter(this.createFilter(queryString)) : history;
        cb(results);
      },

      createFilter(queryString) {
        return (history) => {
          return (history.value.toLowerCase().indexOf(queryString.toLowerCase().trim()) === 0);
        };
      },
    },

    created() {
      if (this.preparedQuery && this.preparedQueryType) {
        this.query = this.preparedQuery
        this.queryType = this.preparedQueryType
        this.disabledInput = true
        this.search()
      } else {
        this.disabledInput = false
      }
    }
  }
</script>
