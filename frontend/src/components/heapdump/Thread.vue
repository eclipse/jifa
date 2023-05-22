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
    <el-row>
      <el-col :offset="15" :span="9">
        <el-tooltip :content="$t('jifa.searchTip')" placement="bottom" effect="light">
          <el-input size="mini"
                    :placeholder="$t('jifa.searchPlaceholder')"
                    class="input-with-select"
                    v-model="searchText"
                    @keyup.enter.native="doSearch"
                    clearable>
            <el-select slot="prepend" style="width: 100px" v-model="searchType" default-first-option>
              <el-option label="By name" value="by_name"></el-option>
              <el-option label="By context classloader name" value="by_context_classloader_name"></el-option>
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

    <el-table ref="table" :data="threads"
              :highlight-current-row="false"
              stripe
              :header-cell-style="headerCellStyle"
              :cell-style='cellStyle'
              row-key="rowKey"
              lazy
              @sort-change="sortTable"
              v-loading="loading"
              height="100%"
              :indent=8
              :load="loadStackInfo"
              :span-method="tableSpanMethod">
      <el-table-column label="Thread / Stack" width="450px" show-overflow-tooltip>
        <template slot-scope="scope">
          <span v-if="scope.row.isThread" @click="$emit('setSelectedObjectId', scope.row.objectId)"
                style="cursor: pointer">
            <img :src="threadIcon"/> {{scope.row.object}}
          </span>

          <span v-if="scope.row.isStack">
            <img :src="frameIcon"/> {{scope.row.stack}}
          </span>

          <span v-if="scope.row.isLocal" @click="$emit('setSelectedObjectId', scope.row.objectId)"
                style="cursor: pointer">
            <img :src="scope.row.icon">
            <strong>{{ " " +scope.row.prefix + " "}}</strong>
            {{scope.row.label}}
            <span style="font-weight: bold; color: #909399">{{ " " + scope.row.suffix}}</span>
          </span>

          <span v-if="scope.row.isOutbound" @click="$emit('setSelectedObjectId', scope.row.objectId)"
                style="cursor: pointer">
            <img :src="scope.row.icon">
            <strong>{{ " " +scope.row.prefix + " "}}</strong>
            {{scope.row.label}}
            <span style="font-weight: bold; color: #909399">{{ " " + scope.row.suffix}}</span>
          </span>

          <span v-if="scope.row.isOutboundsSummary">
            <img :src="ICONS.misc.sumIcon" v-if="scope.row.currentSize >= scope.row.totalSize"/>

            <img :src="ICONS.misc.sumPlusIcon"
                 @dblclick="fetchOutbounds(scope.row.parentRowKey, scope.row.objectId, scope.row.nextPage, scope.row.resolve)"
                 style="cursor: pointer"
                 v-else/>
            {{ toReadableCount(scope.row.currentSize) }} <strong> / </strong> {{ toReadableCount(scope.row.totalSize) }}
          </span>

          <span v-if="scope.row.isThreadSummaryItem">
            <img :src="sumIcon" v-if="currentSize >= totalSize"/>
            <img :src="sumPlusIcon" @dblclick="fetchThreadDetails" style="cursor: pointer" v-else/>
            {{ toReadableCount(currentSize) }} <strong> / </strong> {{ toReadableCount(totalSize) }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="Name" width="300px" prop="name"  show-overflow-tooltip sortable="custom">
      </el-table-column>
      <el-table-column label="Shallow Heap" prop="shallowHeap" sortable="custom" :formatter="toReadableSizeWithUnitFormatter">
      </el-table-column>
      <el-table-column label="Retained Heap" prop="retainedHeap" sortable="custom" :formatter="toReadableSizeWithUnitFormatter">
      </el-table-column>
      <el-table-column label="Max Locals' Retained Heap" prop="maxLocalsRetainedHeap" :formatter="toReadableSizeWithUnitFormatter">
      </el-table-column>
      <el-table-column label="Context Class Loader" prop="contextClassLoader" width="420px" show-overflow-tooltip
                       sortable="custom">
      </el-table-column>
      <el-table-column label="Is Daemon" prop="daemon" sortable="custom">
      </el-table-column>
    </el-table>

  </div>
</template>

<script>
  import axios from 'axios'
  import {heapDumpService, toReadableCount, toReadableSizeWithUnitFormatter} from '../../util'
  import {getOutboundIcon, ICONS} from './IconHealper'

  let rowKey = 1

  export default {
    props: ['file'],
    methods: {
      toReadableCount,
      toReadableSizeWithUnitFormatter,
      sortTable(val) {
        this.sortBy = val.prop
        this.nextPage = 1
        this.totalSize = 0
        this.currentSize = 0
        this.threads = []
        this.ascendingOrder = val.order === 'ascending'
        this.fetchThreadsData()
      },
      doSearch() {
        this.currentSize = 0
        this.nextPage = 1
        this.totalSize = 0
        this.records = []
        this.threads = []
        this.fetchThreadsData()
      },
      loadStackInfo(tree, treeNode, resolve) {
        if (tree.isThread) {
          this.fetchStackTrace(tree.objectId, resolve)
        } else if (tree.isStack) {
          this.fetchLocals(tree.threadObjectId, tree.depth, tree.firstNonNativeFrame, resolve)
        } else if (tree.isLocal || tree.isOutbound) {
          this.fetchOutbounds(tree.rowKey, tree.objectId, 1, resolve)
        } else if (tree.isOutboundsSummary) {
          this.fetchOutbounds(tree.parentRowKey, tree.objectId, tree.nextPage, resolve)
        }
      },
      tableSpanMethod(i) {
        if (i.row.isStack) {
          if (i.columnIndex === 0) {
            return [1, 2];
          } else if (i.columnIndex === 1) {
            return [0, 0];
          } else {
            return [1, 1]
          }
        }

        if (i.row.isLocal || i.row.isOutbound) {
          if (i.columnIndex === 0) {
            return [1, 2];
          } else if (i.columnIndex === 1) {
            return [0, 0];
          } else {
            return [1, 1]
          }
        }
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
          let loaded = this.$refs['table'].store.states.lazyTreeNodeMap[parentRowKey]
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
          for (let i = 0; i < res.length; i++) {
            loaded.push({
              rowKey: rowKey++,
              isOutbound: true,
              objectId: res[i].objectId,
              prefix: res[i].prefix,
              label: res[i].label,
              suffix: res[i].suffix,
              shallowHeap: res[i].shallowSize,
              retainedHeap: res[i].retainedSize,
              icon: getOutboundIcon(res[i].gCRoot, res[i].objectType),
              hasChildren: res[i].hasOutbound
            })
          }

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
      fetchLocals(objId, depth, firstNonNativeFrame, resolve) {
        this.loading = true
        axios.get(heapDumpService(this.file, 'locals'), {
          params: {
            objectId: objId,
            depth: depth,
            firstNonNativeFrame: firstNonNativeFrame
          }
        }).then(resp => {
          let res = resp.data
          let locals = []
          for (let i = 0; i < res.length; i++) {
            locals.push({
              rowKey: rowKey++,
              isLocal: true,
              objectId: res[i].objectId,
              prefix: res[i].prefix,
              label: res[i].label,
              suffix: res[i].suffix,
              shallowHeap: res[i].shallowSize,
              retainedHeap: res[i].retainedSize,
              icon: getOutboundIcon(res[i].gCRoot, res[i].objectType),
              hasChildren: res[i].hasOutbound
            })
          }
          resolve(locals)
          this.loading = false
        })
      },
      fetchStackTrace(objId, resolve) {
        this.loading = true
        axios.get(heapDumpService(this.file, 'stackTrace'), {
          params: {
            objectId: objId
          }
        }).then(resp => {
          let res = resp.data
          let stacks = []
          let depth = 1
          for (let i = 0; i < res.length; i++) {
            stacks.push({
              rowKey: rowKey++,
              isStack: true,
              threadObjectId: objId,
              stack: res[i].stack,
              depth: depth++,
              hasChildren: res[i].hasLocal,
              maxLocalsRetainedHeap: res[i].maxLocalsRetainedSize,
              firstNonNativeFrame: res[i].firstNonNativeFrame
            })
          }
          resolve(stacks)
          this.loading = false
        })
      },
      fetchThreadsData() {
        this.loading = true
        axios.get(heapDumpService(this.file, 'threadsSummary'), {
          params: {
            searchText: this.searchText,
            searchType: this.searchType,
          }
        }).then(resp => {
          this.shallowHeap = resp.data.shallowHeap
          this.retainedHeap = resp.data.retainedHeap
          this.totalSize = resp.data.totalSize
          this.fetchThreadDetails();
        })
      },
      fetchThreadDetails() {
        if (this.currentSize >= this.totalSize) {
          this.loading = false
          return
        }
        if (this.nextPage > 1) {
          this.threads.splice(this.threads.length - 1, 1)
        }
        axios.get(heapDumpService(this.file, 'threads'), {
          params: {
            page: this.nextPage,
            pageSize: this.pageSize,
            sortBy: this.sortBy,
            ascendingOrder: this.ascendingOrder,
            searchText: this.searchText,
            searchType: this.searchType,
          }
        }).then(resp => {
          let res = resp.data.data
          let tmp = []
          this.currentSize += res.length
          for (let i = 0; i < res.length; i++) {
            let tmp_obj = {
              rowKey: rowKey++,
              isThread: true,
              objectId: res[i].objectId,
              object: res[i].object,
              name: res[i].name,
              shallowHeap: res[i].shallowSize,
              retainedHeap: res[i].retainedSize,
              contextClassLoader: res[i].contextClassLoader,
              daemon: res[i].daemon + '',
              hasChildren: res[i].hasStack
            }
            tmp.push(tmp_obj)
          }
          this.nextPage++
          tmp.forEach(t => this.threads.push(t))
          this.threads.push({
            rowKey: rowKey++,
            isThreadSummaryItem: true,
            shallowHeap: this.shallowHeap,
            retainedHeap: this.retainedHeap
          })
          this.loading = false
        })
      }
    },
    data() {
      return {
        loading: false,
        threads: [],
        ICONS,
        threadIcon: require('../../assets/heap/thread.gif'),
        frameIcon: require('../../assets/heap/stack_frame.gif'),
        sumIcon: require('../../assets/heap/misc/sum.gif'),
        sumPlusIcon: require('../../assets/heap/misc/sum_plus.gif'),
        nextPage: 1,
        pageSize: 25,
        currentSize: 0,
        totalSize: 0,
        shallowHeap: 0,
        retainedHeap: 0,
        cellStyle: {padding: '4px', fontSize: '12px'},
        headerCellStyle: {padding: 0, 'font-size': '12px', 'font-weight': 'normal'},

        // sorting support
        ascendingOrder: true,
        sortBy: 'retainedHeap',

        // query support
        searchText: '',
        inSearching: false,
        searchType: 'by_name'
      }
    },
    created() {
      this.fetchThreadsData()
    }
  }
</script>