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
          ref="table"
          :data="roots"
          :highlight-current-row="false"
          stripe
          :header-cell-style="headerCellStyle"
          :cell-style='cellStyle'
          :span-method="spanMethod"
          row-key="rowKey"
          lazy
          v-loading="loading"
          height="100%"
          :indent=8
          :load="load"
  >

    <el-table-column label="Class Name" show-overflow-tooltip>
      <template slot-scope="scope">
          <span v-if="scope.row.isRoot">
            <img :src="scope.row.icon" style="margin-right: 5px"/>
            {{ scope.row.label }}
          </span>

        <span v-if="scope.row.isClass" @click="$emit('setSelectedObjectId', scope.row.objectId)"
              style="cursor: pointer">
            <img :src="scope.row.icon" style="margin-right: 5px"/>
            {{ scope.row.label }}
          </span>

        <span v-if="scope.row.isObject" @click="$emit('setSelectedObjectId', scope.row.objectId)"
              style="cursor: pointer">
            <img :src="scope.row.icon" style="margin-right: 5px"/>
              {{ scope.row.label }}
          <!--<span style="font-weight: bold; color: #909399">-->
          <!--{{ scope.row.suffix }}-->
          <!--</span>-->
          </span>

        <span v-if="scope.row.isOutbound" @click="$emit('setSelectedObjectId', scope.row.objectId)"
              style="cursor: pointer">
            <img :src="scope.row.icon" style="margin-right: 5px"/>
            <strong>{{ scope.row.prefix }}</strong>
            {{ scope.row.label }}
            <span style="font-weight: bold; color: #909399">
              {{ scope.row.suffix }}
            </span>
          </span>


        <span v-if="scope.row.isClassSummary">
            <img :src="ICONS.misc.sumIcon" v-if="scope.row.currentSize >= scope.row.totalSize"/>
            <img :src="ICONS.misc.sumPlusIcon"
                 @dblclick="fetchClasses(scope.row.parentRowKey, scope.row.rootTypeIndex, scope.row.nextPage, scope.row.resolve)"
                 style="cursor: pointer" v-else/>
            {{ toReadableCount(scope.row.currentSize) }} <strong> / </strong> {{ toReadableCount(scope.row.totalSize) }}
           </span>

        <span v-if="scope.row.isObjectSummary">
            <img :src="ICONS.misc.sumIcon" v-if="scope.row.currentSize >= scope.row.totalSize"/>
            <img :src="ICONS.misc.sumPlusIcon"
                 @dblclick="fetchObjects(scope.row.parentRowKey, scope.row.rootTypeIndex,
                 scope.row.classIndex, scope.row.nextPage, scope.row.resolve)"
                 style="cursor: pointer" v-else/>
            {{ toReadableCount(scope.row.currentSize) }} <strong> / </strong> {{ toReadableCount(scope.row.totalSize) }}
           </span>

        <span v-if="scope.row.isOutboundsSummary">
            <img :src="ICONS.misc.sumIcon" v-if="scope.row.currentSize >= scope.row.totalSize"/>
            <img :src="ICONS.misc.sumPlusIcon"
                 @dblclick="fetchOutbounds(scope.row.parentRowKey, scope.row.objectId, scope.row.nextPage, scope.row.resolve)"
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
    <el-table-column/>

    <el-table-column label="Objects" prop="objects" :formatter="toReadableCountFormatter">
    </el-table-column>

    <el-table-column label="Shallow Heap" prop="shallowHeap" :formatter="toReadableSizeWithUnitFormatter">
    </el-table-column>

    <el-table-column label="Retained Heap" prop="retainedHeap" :formatter="toReadableSizeWithUnitFormatter">
    </el-table-column>
  </el-table>
</template>

<script>
  import axios from 'axios'
  import {getIcon, getOutboundIcon, ICONS} from "./IconHealper";
  import {heapDumpService, toReadableCount, toReadableCountFormatter, toReadableSizeWithUnitFormatter} from '../../util'

  let rowKey = 1
  export default {
    props: ['file'],
    data() {
      return {
        cellStyle: {padding: '4px', fontSize: '12px'},
        headerCellStyle: {padding: 0, 'font-size': '12px', 'font-weight': 'normal'},
        ICONS,
        pageSize: 25,
        loading: false,
        roots: []
      }
    },

    methods: {
      toReadableCount,
      toReadableCountFormatter,
      toReadableSizeWithUnitFormatter,
      spanMethod(row) {
        let index = row.columnIndex
        if (index === 0) {
          return [1, 7]
        } else if (index >= 1 && index <= 6) {
          return [0, 0]
        }
        return [1, 1]
      },
      fetchRoots() {
        this.loading = true
        axios.get(heapDumpService(this.file, 'GCRoots')).then(resp => {
          let rootIndex = 0
          resp.data.forEach(
              root => {
                this.roots.push({
                  rowKey: rowKey++,
                  isRoot: true,
                  rootTypeIndex: rootIndex++,
                  label: root.className,
                  objects: root.objects,
                  icon: ICONS.roots,
                  hasChildren: root.objects > 0,
                })
              },
              this.loading = false
          )
        })
      },
      fetchClasses(parentRowKey, rootTypeIndex, page, resolve) {
        this.loading = true
        axios.get(heapDumpService(this.file, 'GCRoots/classes'), {
          params: {
            rootTypeIndex: rootTypeIndex,
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
          let classIndex = loadedLen
          res.forEach(d => {
            loaded.push({
              rowKey: rowKey++,
              icon: ICONS.objects.class,
              label: d.className,
              objects: d.objects,
              objectId: d.objectId,
              rootTypeIndex: rootTypeIndex,
              classIndex: classIndex++,
              isClass: true,
              hasChildren: true,
            })
          })

          loaded.push({
            rowKey: rowKey++,
            parentRowKey: parentRowKey,
            isClassSummary: true,
            rootTypeIndex: rootTypeIndex,
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

      fetchObjects(parentRowKey, rootTypeIndex, classIndex, page, resolve) {
        this.loading = true
        axios.get(heapDumpService(this.file, 'GCRoots/class/objects'), {
          params: {
            rootTypeIndex: rootTypeIndex,
            classIndex: classIndex,
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
          res.forEach(d => {
            loaded.push({
              rowKey: rowKey++,
              icon: getIcon(d.gCRoot, d.objectType),
              label: d.label,
              objectId: d.objectId,
              suffix: d.suffix,
              shallowHeap: d.shallowSize,
              retainedHeap: d.retainedSize,
              isObject: true,
              hasChildren: true,
            })
          })

          loaded.push({
            rowKey: rowKey++,
            parentRowKey: parentRowKey,
            isObjectSummary: true,
            rootTypeIndex: rootTypeIndex,
            classIndex: classIndex,
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
              isOutbound: true
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

      load(tree, treeNode, resolve) {
        if (tree.isRoot) {
          this.fetchClasses(tree.rowKey, tree.rootTypeIndex, 1, resolve)
        } else if (tree.isClass) {
          this.fetchObjects(tree.rowKey, tree.rootTypeIndex, tree.classIndex, 1, resolve)
        } else {
          this.fetchOutbounds(tree.rowKey, tree.objectId, 1, resolve)
        }
      }
    },

    created() {
      this.fetchRoots()
    }
  }
</script>
