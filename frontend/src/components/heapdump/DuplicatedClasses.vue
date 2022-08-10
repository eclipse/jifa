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
              <el-option label="By classloader count" value="by_classloader_count"></el-option>
            </el-select>

            <el-button slot="append" :icon="inSearching ? 'el-icon-loading' : 'el-icon-search'"
                       :disabled="inSearching"
                       @click="doSearch"/>
          </el-input>
        </el-tooltip>
      </el-col>
    </el-row>

    <el-table
        ref="table"
        :data="records"
        :highlight-current-row="false"
        stripe
        :header-cell-style="headerCellStyle"
        :cell-style='cellStyle'
        row-key="rowKey"
        height="100%"
        :indent=8
        lazy
        v-loading="loading"
        :load="fetchClassLoaders"
        :span-method="tableSpanMethod">
      <el-table-column label="Class Name/Class Loader" show-overflow-tooltip>
        <template slot-scope="scope">
          <span v-if="scope.row.isClassItem">
            <img :src="ICONS.objects.class"/> {{scope.row.label}}
          </span>

          <span v-if="scope.row.isClassLoaderItem" @click="$emit('setSelectedObjectId', scope.row.objectId)"
                style="cursor: pointer">
            <img :src="scope.row.icon"/> {{scope.row.label}}
            <span style="font-weight: bold; color: #909399" v-if="scope.row.suffix">{{ " " + scope.row.suffix}}</span>
          </span>

          <span v-if="scope.row.isCldSummaryItem">
            <img :src="ICONS.misc.sumIcon" v-if="scope.row.currentSize >= scope.row.totalSize"/>

            <img :src="ICONS.misc.sumPlusIcon" @dblclick="fetchMoreClassLoaders(scope.row)" style="cursor: pointer"
                 v-else/>
            {{ toReadableCount(scope.row.currentSize) }} <strong> / </strong> {{ toReadableCount(scope.row.totalSize) }}
          </span>

          <span v-if="scope.row.isSummaryItem">
            <img :src="ICONS.misc.sumIcon" v-if="currentSize >= totalSize"/>
            <img :src="ICONS.misc.sumPlusIcon" @dblclick="fetchDuplicatedClasses" style="cursor: pointer" v-else/>
            {{ toReadableCount(currentSize) }} <strong> / </strong> {{ toReadableCount(totalSize) }}
          </span>
        </template>
      </el-table-column>

      <el-table-column/>
      <el-table-column/>
      <el-table-column/>

      <el-table-column label="ClassLoader Counts" prop="classLoaderCount" :formatter="toReadableCountFormatter">
      </el-table-column>

      <el-table-column label="Defined Classes" prop="definedClassesCount" :formatter="toReadableCountFormatter">
      </el-table-column>

      <el-table-column label="Num of Instances" prop="instantiatedObjectsCount" :formatter="toReadableCountFormatter">
      </el-table-column>
    </el-table>

  </div>
</template>

<script>

  import axios from 'axios'
  import {heapDumpService, toReadableCount, toReadableCountFormatter} from '../../util'
  import {getIcon, ICONS} from "./IconHealper";
  import {OBJECT_TYPE} from './CommonType'

  let rowKey = 1
  export default {
    props: ['file'],
    methods: {
      toReadableCount,
      toReadableCountFormatter,
      tableSpanMethod(row) {
        let index = row.columnIndex
        if (index === 0) {
          return [1, 4]
        } else if (index >= 1 && index <= 3) {
          return [0, 0]
        }
        return [1, 1]
      },
      doSearch() {
        this.currentSize = 0
        this.nextPage = 1
        this.totalSize = 0
        this.records = []
        this.fetchDuplicatedClasses()
      },
      doFetchClassLoaders(parentRowKey, index, page, resolve) {
        this.loading = true
        axios.get(heapDumpService(this.file, 'duplicatedClasses/classLoaders'), {
          params: {
            index: index,
            page: page,
            pageSize: this.pageSize,
          }
        }).then(resp => {
          let loadedLen = 0
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
              isClassLoaderItem: true,
              label: res[i].label,
              objectId: res[i].objectId,
              suffix: res[i].suffix,
              definedClassesCount: res[i].definedClassesCount,
              instantiatedObjectsCount: res[i].instantiatedObjectsCount,
              icon: getIcon(res[i].gCRoot, OBJECT_TYPE.CLASSLOADER)
            })
          }
          loaded.push({
            rowKey: rowKey++,
            parentRowKey: parentRowKey,
            isCldSummaryItem: true,
            index: index,
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
      fetchMoreClassLoaders(summary) {
        this.doFetchClassLoaders(summary.parentRowKey, summary.index, summary.nextPage, summary.resolve)
      },
      fetchClassLoaders(tree, treeNode, resolve) {
        this.doFetchClassLoaders(tree.rowKey, tree.index, 1, resolve)
      },
      fetchDuplicatedClasses() {
        this.loading = true
        if (this.nextPage > 1) {
          this.records.splice(this.records.length - 1, 1)
        }
        axios.get(heapDumpService(this.file, 'duplicatedClasses/classes'), {
          params: {
            page: this.nextPage,
            pageSize: this.pageSize,
            searchText: this.searchText,
            searchType: this.searchType,
          }
        }).then(resp => {
          this.totalSize = resp.data.totalSize
          let res = resp.data.data
          let index = this.currentSize
          let tmp = []
          for (let i = 0; i < res.length; i++) {
            tmp.push({
              rowKey: rowKey++,
              label: res[i].label,
              icon: this.classIcon,
              index: index++,
              classLoaderCount: res[i].count,
              currentSize: 0,
              hasChildren: res[i].count > 0,
              isClassItem: true,
            })
          }
          this.nextPage++
          this.currentSize += res.length
          tmp.forEach(t => this.records.push(t))
          if (this.records.length > 0) {
            this.records.push({
              rowKey: rowKey++,
              isSummaryItem: true
            })
          }
          this.loading = false
        })
      },

    },
    data() {
      return {
        ICONS,
        loading: true,
        records: [],
        nextPage: 1,
        pageSize: 25,
        currentSize: 0,
        totalSize: 0,
        cellStyle: {padding: '4px', fontSize: '12px'},
        headerCellStyle: {padding: 0, 'font-size': '12px', 'font-weight': 'normal'},

        // query support
        searchText: '',
        inSearching: false,
        searchType: 'by_name'
      }
    },
    created() {
      this.fetchDuplicatedClasses()
    }
  }
</script>