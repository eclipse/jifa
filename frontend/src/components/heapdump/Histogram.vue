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
        <el-radio-group v-model="groupingBy" style="margin-top:10px; margin-left: 10px" @change="fetchNewHistogram" size="mini">
          <el-radio label="by_class">Class</el-radio>
          <el-radio label="by_superclass">SuperClass</el-radio>
          <el-radio label="by_classloader">ClassLoader</el-radio>
          <el-radio label="by_package">Package</el-radio>
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
            <el-select slot="prepend" style="width: 100px" v-model="searchType" placeholder="Choose" default-first-option>
              <el-option label="By name" value="by_name"></el-option>
              <el-option label="By object num" value="by_obj_num"></el-option>
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
                @click="groupingBy === 'by_class' ? $emit('outgoingRefsOfHistogramObjs', contextMenuTargetObjectId, contextMenuTargetObjectLabel)
                                                  : $emit('outgoingRefsOfObj', contextMenuTargetObjectId, contextMenuTargetObjectLabel)">
          {{$t('jifa.heap.ref.object.outgoing')}}
        </v-contextmenu-item>
        <v-contextmenu-item
                @click="groupingBy === 'by_class' ? $emit('incomingRefsOfHistogramObjs', contextMenuTargetObjectId, contextMenuTargetObjectLabel)
                                                  : $emit('incomingRefsOfObj', contextMenuTargetObjectId, contextMenuTargetObjectLabel)">
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

    <div style="flex-grow: 1; overflow: auto">
      <el-table ref='recordTable' :data="tableData"
                :highlight-current-row="false"
                stripe
                @sort-change="sortTable"
                :header-cell-style="headerCellStyle"
                :cell-style='cellStyle'
                row-key="rowKey"
                :load="loadChildren"
                lazy
                :span-method="spanMethod"
                :indent=8
                v-loading="loading"
      >
        <el-table-column prop="id" :label="label" sortable="custom">
          <template slot-scope="scope">
          <span v-if="scope.row.isRecord"
                @click="scope.row.type!==6?$emit('setSelectedObjectId', scope.row.objectId):{}"
                @contextmenu="contextMenuTargetObjectId = scope.row.objectId; contextMenuTargetObjectLabel = scope.row.label"
                v-contextmenu:contextmenu
                style="cursor: pointer">
            <img :src="scope.row.icon"/> {{ scope.row.label }}
          </span>

            <span v-if="scope.row.isSummary">
            <img :src="sumIcon" v-if="records.length >= totalSize"/>
            <img :src="sumPlusIcon" @dblclick="fetchHistogram" style="cursor: pointer" v-else/>
            {{ toReadableCount(records.length) }} <strong> / </strong> {{ toReadableCount(totalSize) }}
          </span>

            <span v-if="scope.row.isChildrenSummary">
              <img :src="ICONS.misc.sumIcon" v-if="scope.row.currentSize >= scope.row.totalSize"/>
              <img :src="ICONS.misc.sumPlusIcon"
                   @dblclick="fetchChildren(scope.row.parentRowKey, scope.row.objectId, scope.row.nextPage, scope.row.resolve)"
                   style="cursor: pointer"
                   v-else/>
              {{ toReadableCount(scope.row.currentSize) }} <strong> / </strong> {{
                toReadableCount(scope.row.totalSize)
              }}
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

        <el-table-column label="Objects" prop="numberOfObjects" sortable="custom" :formatter="toReadableCountFormatter">
        </el-table-column>

        <el-table-column label="Shallow Heap" prop="shallowSize" sortable="custom"
                         :formatter="toReadableSizeWithUnitFormatter">
        </el-table-column>

        <el-table-column label="Objects(Y)" prop="numberOfYoungObjects" v-if="generationInfoAvailable" sortable="custom"
                         :formatter="toReadableCountFormatter">
        </el-table-column>

        <el-table-column label="Shallow Heap(Y) " prop="shallowSizeOfYoung" v-if="generationInfoAvailable"
                         sortable="custom"
                         :formatter="toReadableSizeWithUnitFormatter">
        </el-table-column>

        <el-table-column label="Objects(O)" prop="numberOfOldObjects" v-if="generationInfoAvailable" sortable="custom"
                         :formatter="toReadableCountFormatter">
        </el-table-column>

        <el-table-column label="Shallow Heap(O)" prop="shallowSizeOfOld" v-if="generationInfoAvailable"
                         sortable="custom"
                         :formatter="toReadableSizeWithUnitFormatter">
        </el-table-column>

        <el-table-column label="Retained Heap" prop="retainedSize" sortable="custom"
                         v-if="this.groupingBy!=='by_package' && this.groupingBy!=='by_superclass'">
          <template slot-scope="scope">
          <span v-if="scope.row.retainedSize < 0">
            >= {{ toReadableSizeWithUnit(-scope.row.retainedSize) }}
          </span>

            <span v-else>
            {{ toReadableSizeWithUnit(scope.row.retainedSize) }}
          </span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>

  import axios from 'axios'
  import {ICONS,getIcon} from "./IconHealper";
  import {heapDumpService, toReadableCount, toReadableCountFormatter, toReadableSizeWithUnit, toReadableSizeWithUnitFormatter} from '../../util'
  import {OBJECT_TYPE} from "@/components/heapdump/CommonType";

  let rowKey = 1
  export default {
    props: ['file', 'generationInfoAvailable'],
    methods: {
      toReadableCount,
      toReadableCountFormatter,
      toReadableSizeWithUnit,
      toReadableSizeWithUnitFormatter,
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
            sortBy: this.sortBy,
            ascendingOrder: this.ascendingOrder,
            searchText: this.searchText,
            searchType: this.searchType,
          }
        }).then(resp => {
          let records = resp.data.data
          this.totalSize = resp.data.totalSize
          records.forEach(record =>
              this.records.push({
                rowKey: rowKey++,
                objectId: record.objectId,
                label: record.label,
                type : record.type,
                icon: getIcon(false,record.type,false),
                numberOfObjects: record.numberOfObjects,
                shallowSize: record.shallowSize,

                numberOfYoungObjects: record.numberOfYoungObjects,
                shallowSizeOfYoung: record.shallowSizeOfYoung,

                numberOfOldObjects: record.numberOfOldObjects,
                shallowSizeOfOld: record.shallowSizeOfOld,

                retainedSize: record.retainedSize,
                isRecord: true,
                hasChildren: record.type !== OBJECT_TYPE.CLASS
              }))

          this.tableData = this.records.concat({
            rowKey: rowKey++,
            isSummary: true
          })
          this.nextPage++
          this.loading = false
        })
      },
      fetchNewHistogram(){
        this.nextPage = 1
        this.totalSize = 0
        this.records = []
        switch (this.groupingBy) {
          case "by_class":
            this.label = "Class Name";break;
          case "by_classloader":
            this.label = "Class Loader";break;
          case "by_superclass":
            this.label = "Super Class";break;
          case "by_package":
            this.label = "Package";break;
          default:
            throw "should not reach here";
        }
        this.fetchHistogram();
      },
      sortTable(val){
        this.sortBy = val.prop;
        this.ascendingOrder = val.order === 'ascending';
        this.fetchNewHistogram();
      },
      loadChildren(tree, treeNode, resolve) {
        this.fetchChildren(tree.rowKey, tree.objectId, 1, resolve)
      },
      fetchChildren(parentRowKey, objectId, page, resolve) {
        this.loading = true
        axios.get(heapDumpService(this.file, 'histogram/children'), {
          params: {
            groupingBy: this.groupingBy,
            page: page,
            pageSize: this.pageSize,
            sortBy: this.sortBy,
            ascendingOrder: this.ascendingOrder,
            parentObjectId: objectId,
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
          res.forEach(record => {
            loaded.push({
              rowKey: rowKey++,
              objectId: record.objectId,
              label: record.label,
              type : record.type,
              icon: getIcon(false,record.type,false),
              numberOfObjects: record.numberOfObjects,
              shallowSize: record.shallowSize,

              numberOfYoungObjects: record.numberOfYoungObjects,
              shallowSizeOfYoung: record.shallowSizeOfYoung,

              numberOfOldObjects: record.numberOfOldObjects,
              shallowSizeOfOld: record.shallowSizeOfOld,

              retainedSize: record.retainedSize,
              isRecord: true,
              hasChildren: record.type !== OBJECT_TYPE.CLASS
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
          })

          if (callResolve) {
            resolve(loaded)
          }
          this.loading = false
        })
      },
      doSearch(){
        this.nextPage = 1
        this.totalSize = 0
        this.records = []
        this.fetchHistogram();
      }
    },
    data() {
      return {
        ICONS,
        loading: false,
        sumIcon: require('../../assets/heap/misc/sum.gif'),
        sumPlusIcon: require('../../assets/heap/misc/sum_plus.gif'),
        label: 'Class Name',
        cellStyle: {padding: '4px', fontSize: '12px'},
        headerCellStyle: {padding: 0, 'font-size': '12px', 'font-weight': 'normal'},

        records: [],
        tableData: [],
        nextPage: 1,
        pageSize: 25,
        totalSize: 0,

        contextMenuTargetObjectId: null,
        contextMenuTargetObjectLabel: null,

        // grouping support
        groupingBy: 'by_class',

        // query support
        searchText:'',
        inSearching:false,
        searchType:'by_name',

        // sorting support
        sortBy: 'retainedSize',
        ascendingOrder: true,
      }
    },
    created() {
      this.fetchHistogram()
    }
  }
</script>