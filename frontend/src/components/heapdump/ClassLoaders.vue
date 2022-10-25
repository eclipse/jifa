<!--
    Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation

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
      <v-contextmenu-item divider></v-contextmenu-item>
      <v-contextmenu-item
              @click="$emit('pathToGCRootsOfObj', contextMenuTargetObjectId, contextMenuTargetObjectLabel)">
        {{$t('jifa.heap.pathToGCRoots')}}
      </v-contextmenu-item>
    </v-contextmenu>

    <el-table ref="recordTable"
              :data="tableData"
              :highlight-current-row="false"
              stripe
              :header-cell-style="headerCellStyle"
              :cell-style='cellStyle'
              row-key="rowKey"
              :load="loadChildren"
              lazy
              :span-method="spanMethod"
              height="100%"
              :indent=8
              v-loading="loading"
    >
      <el-table-column label="Class Name">
        <template slot-scope="scope">
          <span v-if="scope.row.isRecord" @click="$emit('setSelectedObjectId', scope.row.objectId)"
                style="cursor: pointer"
                @contextmenu="contextMenuTargetObjectId = scope.row.objectId; contextMenuTargetObjectLabel = scope.row.label"
                v-contextmenu:contextmenu>
            <img :src="scope.row.classLoader ? (scope.row.hasParent ? classLoaderOutboundIcon : classLoaderIcon) : classIcon"/>
            <strong>{{ scope.row.prefix }}</strong>
            {{scope.row.label}}
          </span>

          <span v-if="scope.row.isSummary">
            <img :src="sumIcon" v-if="records.length >= totalSize"/>
            <img :src="sumPlusIcon" @dblclick="fetchClassLoaders" style="cursor: pointer" v-else/>
            {{ toReadableCount(records.length) }} <strong> / </strong> {{ toReadableCount(totalSize) }}
          </span>

          <span v-if="scope.row.isChildrenSummary">
              <img :src="sumIcon" v-if="scope.row.currentSize >= scope.row.totalSize"/>
              <img :src="sumPlusIcon"
                   @dblclick="fetchChildren(scope.row.parentRowKey, scope.row.objectId, scope.row.nextPage, scope.row.resolve)"
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

      <el-table-column label="Defined Classes" prop="definedClasses" :formatter="toReadableCountFormatter">
      </el-table-column>

      <el-table-column label="No. of Instances" prop="numberOfInstances" :formatter="toReadableCountFormatter">
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
  import axios from 'axios'
  import {heapDumpService, toReadableCount, toReadableCountFormatter } from '../../util'
  import {ICONS} from "./IconHealper";

  let rowKey = 1
  export default {
    props: ['file'],
    methods: {
      toReadableCount,
      toReadableCountFormatter,
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
        axios.get(heapDumpService(this.file, 'classLoaderExplorer/summary')).then(resp => {
          this.definedClasses = resp.data.definedClasses
          this.numberOfInstances = resp.data.numberOfInstances
          this.totalSize = resp.data.totalSize
          if (this.totalSize > 0) {
            this.fetchClassLoaders()
          }
        })
      },
      fetchClassLoaders() {
        this.loading = true
        axios.get(heapDumpService(this.file, 'classLoaderExplorer/classLoader'), {
          params: {
            page: this.nextPage,
            pageSize: this.pageSize,
          }
        }).then(resp => {
          let records = resp.data.data
          records.forEach(item => this.records.push({
            rowKey: rowKey++,

            objectId: item.objectId,
            prefix: item.prefix,
            label: item.label,
            classLoader: item.classLoader,
            hasParent: item.hasParent,
            definedClasses: item.definedClasses,
            numberOfInstances: item.numberOfInstances,

            hasChildren: item.classLoader,
            isRecord: true,
          }))

          this.tableData = this.records.concat({
            rowKey: rowKey++,
            definedClasses: this.definedClasses,
            numberOfInstances: this.numberOfInstances,
            isSummary: true
          })

          this.nextPage++
          this.loading = false
        })
      },

      loadChildren(tree, treeNode, resolve) {
        this.fetchChildren(tree.rowKey, tree.objectId, 1, resolve)
      },

      fetchChildren(parentRowKey, objectId, page, resolve) {
        this.loading = true
        axios.get(heapDumpService(this.file, 'classLoaderExplorer/children'), {
          params: {
            classLoaderId: objectId,
            page: page,
            pageSize: this.pageSize,
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
          res.forEach(item => {
            loaded.push({
              rowKey: rowKey++,

              objectId: item.objectId,
              prefix: item.prefix,
              label: item.label,
              classLoader: item.classLoader,
              hasParent: item.hasParent,
              definedClasses: item.classLoader ? item.definedClasses : null,
              numberOfInstances: item.numberOfInstances,

              hasChildren: item.classLoader,

              isRecord: true,
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
    },
    data() {
      return {
        classIcon: ICONS.objects.class,
        classLoaderIcon: ICONS.objects.classloader_obj,
        classLoaderOutboundIcon: ICONS.objects.out.classloader_obj,

        sumIcon: ICONS.misc.sumIcon,
        sumPlusIcon: ICONS.misc.sumPlusIcon,

        cellStyle: {padding: '4px', fontSize: '12px'},
        headerCellStyle: {padding: 0, 'font-size': '12px', 'font-weight': 'normal'},

        loading: false,

        definedClasses: 0,
        numberOfInstances: 0,

        totalSize: 0,
        nextPage: 1,
        pageSize: 25,
        records: [],
        tableData: [],

        contextMenuTargetObjectId: null,
        contextMenuTargetObjectLabel: null,
      }
    },
    created() {
      this.fetchSummary()
    }
  }
</script>