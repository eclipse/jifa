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
  <div class="resultDiv">
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
      <v-contextmenu-item
              @click="$emit('mergePathToGCRootsFromHistogram', contextMenuTargetObjectId, contextMenuTargetObjectLabel)">
        {{$t('jifa.heap.mergePathToGCRoots')}}
      </v-contextmenu-item>
    </v-contextmenu>

    <el-tabs v-model="editableTabsValue" type="card" closable @tab-remove="removeTab" style="height: 100%">
      <el-tab-pane
              v-for="(item) in editableTabs"
              :key="item.name"
              :label="item.title"
              :name="item.name">
        <el-table v-if="item.isObjRefsTab"
                  ref="resultContainer"
                  v-loading="item.loading"
                  :data="item.topItems"
                  :highlight-current-row="false"
                  stripe
                  :header-cell-style="headerCellStyle"
                  :cell-style='cellStyle'
                  :load="item.load"
                  row-key="rowKey"
                  lazy
                  :indent=8
                  height="100%">
          <el-table-column label="Class Name" width="1000px" show-overflow-tooltip>
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

              <span v-if="scope.row.isBoundsSummary || scope.row.isHistogramObjsSummary">
                <img :src="ICONS.misc.sumIcon" v-if="scope.row.currentSize >= scope.row.totalSize"/>
                <img :src="ICONS.misc.sumPlusIcon"
                     @dblclick="scope.row.isBoundsSummary ? fetchObjBounds(scope.row.parentRowKey, scope.row.objectId, scope.row.nextPage, scope.row.resolve) : loadHistogramObjs()"
                     style="cursor: pointer"
                     v-else/>
                {{ scope.row.currentSize }} <strong> / </strong> {{ scope.row.totalSize }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="Shallow Heap" prop="shallowHeap">
          </el-table-column>
          <el-table-column label="Retained Heap" prop="retainedHeap">
          </el-table-column>
        </el-table>


        <el-table v-if="item.isClassRefsTab"
                  ref="resultContainer"
                  v-loading="item.loading"
                  :data="item.topItems"
                  :highlight-current-row="false"
                  stripe
                  :header-cell-style="headerCellStyle"
                  :cell-style='cellStyle'
                  :load="item.load"
                  row-key="rowKey"
                  lazy
                  :indent=8
                  height="100%">
          <el-table-column label="Class Name" width="1000px" show-overflow-tooltip>
            <template slot-scope="scope">
              <span v-if="scope.row.isResult" @click="$emit('setSelectedObjectId', scope.row.objectId)"
                    style="cursor: pointer"
                    @contextmenu="contextMenuTargetObjectId = scope.row.objectId; contextMenuTargetObjectLabel = scope.row.label"
                    v-contextmenu:contextmenu>
                <img :src="scope.row.icon" style="margin-right: 5px"/>
                {{ scope.row.label }}
              </span>

              <span v-if="scope.row.isBoundsSummary">
                <img :src="ICONS.misc.sumIcon" v-if="scope.row.currentSize >= scope.row.totalSize"/>
                <img :src="ICONS.misc.sumPlusIcon"
                     @dblclick="fetchClassBounds(scope.row.parentRowKey, scope.row.objectIds, scope.row.nextPage, scope.row.resolve)"
                     style="cursor: pointer"
                     v-else/>
                {{ scope.row.currentSize }} <strong> / </strong> {{ scope.row.totalSize }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="Objects" prop="objects">
          </el-table-column>
          <el-table-column label="Shallow Heap" prop="shallowHeap">
          </el-table-column>
        </el-table>

        <div v-if="item.isPathToGCRootsTab" style="height: 100%; overflow: auto">
          <el-tree ref="resultContainer"
                   v-loading="item.loading"
                   :data="item.treeData"
                   node-key="objectId"
                   :expand-on-click-node="false"
                   default-expand-all>
            <span class="custom-tree-node" style="font-size: 12px" slot-scope="{ node, data }"
                  @click="$emit('setSelectedObjectId', data.objectId)"
                  @contextmenu="contextMenuTargetObjectId = data.objectId; contextMenuTargetObjectLabel = data.label"
                  v-contextmenu:contextmenu>
              <span>
                <img :src="data.origin ? getIcon(data.gCRoot, data.objectType) : getInboundIcon(data.gCRoot, data.objectType)"
                     style="margin-right: 5px"/>
                <strong>{{ data.prefix }}</strong>
                {{ data.label }}
                <span style="font-weight: bold; color: #909399">
                  {{ data.suffix }}
                </span>
              </span>

              <span>
                    {{ data.shallowSize }} / {{ data.retainedSize }}
              </span>
            </span>
          </el-tree>
          <el-divider><i v-if="item.hasMore" :class="item.loading ? 'el-icon-loading':'el-icon-circle-plus-outline'"
                         style="font-size: 25px; cursor: pointer"
                         @dblclick="loadMorePathToGCRootsOfObj(item)"></i></el-divider>
        </div>

        <el-table v-if="item.isMergePathToGCRootsTab"
                  ref='resultContainer'
                  :data="item.tableData"
                  :highlight-current-row="true"
                  stripe
                  :header-cell-style="headerCellStyle"
                  :cell-style='cellStyle'
                  row-key="rowKey"
                  v-loading="item.loading"
                  :load="item.load"
                  height="100%"
                  :span-method="spanMethod"
                  :indent=8
                  lazy
                  fit>
          <el-table-column label="Class Name" show-overflow-tooltip>
            <template slot-scope="scope">
              <img :src="scope.row.icon" style="margin-right: 5px"/>
              {{ scope.row.label }}
              <span style="font-weight: bold; color: #909399">
                {{ scope.row.suffix }}
              </span>
              <span v-if="scope.row.isSummaryItem">
                <img :src="ICONS.misc.sumIcon" v-if="item.records.length >= item.totalSize"/>
                <img :src="ICONS.misc.sumPlusIcon" @dblclick="fetchMergePathToGCRootsNextPageData" style="cursor: pointer" v-else/>
                {{ item.records.length }} <strong> / </strong> {{item.totalSize}}
              </span>
              <span v-if="scope.row.isChildrenSummary">
                <img :src="ICONS.misc.sumIcon" v-if="scope.row.currentSize >= scope.row.totalSize"/>
                <img :src="ICONS.misc.sumPlusIcon" @dblclick="fetchMergePathToGCRootsChildren(scope.row, scope.row.nextPage, scope.row.parentRowKey, scope.row.resolve)" style="cursor: pointer" v-else/>
                        {{ scope.row.currentSize }} <strong> / </strong> {{ scope.row.totalSize }}
              </span>
            </template>
          </el-table-column>
          <el-table-column/>
          <el-table-column/>
          <el-table-column/>
          <el-table-column/>
          <el-table-column/>

          <el-table-column label="Ref. Objects" prop="refObjects"></el-table-column>
          <el-table-column label="Shallow Heap" prop="shallowHeap"></el-table-column>
          <el-table-column label="Ref. Shallow Heap" prop="refShallowHeap"></el-table-column>
          <el-table-column label="Retained Heap" prop="retainedHeap"></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
<script>
  import axios from 'axios'
  import {
    getClassRefInboundIcon,
    getClassRefOutboundIcon,
    getIcon,
    getInboundIcon,
    getOutboundIcon,
    ICONS
  } from "./IconHealper";
  import {heapDumpService} from "../../util";

  let rowKey = 1

  export default {
    props: ['file'],
    data() {
      return {
        cellStyle: {padding: '4px', fontSize: '12px'},
        headerCellStyle: {padding: 0, 'font-size': '12px', 'font-weight': 'normal'},
        tabIndex: 0,
        editableTabsValue: null,
        editableTabs: [],
        ICONS,
        pageSize: 25,

        contextMenuTargetObjectId: null,
        contextMenuTargetObjectLabel: null,
      }
    },
    methods: {
      getIcon,
      getInboundIcon,
      getOutboundIcon,
      currentIndex() {
        for (let i = 0; i < this.editableTabs.length; i++) {
          if (this.editableTabs[i].name === this.editableTabsValue) {
            return i;
          }
        }
      },
      buildTitle(prefix, label) {
        return '「' + prefix + '」' + label
      },

      loadObjBounds(tree, treeNode, resolve) {
        this.fetchObjBounds(tree.rowKey, tree.objectId, 1, resolve)
      },

      fetchObjBounds(parentRowKey, objectId, page, resolve) {
        let index = this.currentIndex()
        let tab = this.editableTabs[index]
        let outbound = tab.outbound
        let table = this.$refs.resultContainer[index]
        tab.loading = true
        axios.get(heapDumpService(this.file, outbound ? 'outbounds' : 'inbounds'), {
          params: {
            objectId: objectId,
            page: page,
            pageSize: this.pageSize,
          }
        }).then(resp => {
          let loadedLen = 0;
          let loaded = table.store.states.lazyTreeNodeMap[parentRowKey]
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
              icon: outbound ? getOutboundIcon(d.gCRoot, d.objectType) : getInboundIcon(d.gCRoot, d.objectType),
              prefix: d.prefix,
              label: d.label,
              suffix: d.suffix,
              shallowHeap: d.shallowSize,
              retainedHeap: d.retainedSize,
              hasChildren: true,
              objectId: d.objectId,
              isResult: true
            })
          })

          loaded.push({
            rowKey: rowKey++,
            objectId: objectId,
            parentRowKey: parentRowKey,
            isBoundsSummary: true,
            nextPage: page + 1,
            currentSize: loadedLen + res.length,
            totalSize: resp.data.totalSize,
            resolve: resolve,
          })

          if (callResolve) {
            resolve(loaded)
          }
          tab.loading = false
        })
      },

      outgoingRefsOfObj(objectId, label) {
        this.boundsOfObj(objectId, label, true)
      },

      incomingRefsOfObj(objectId, label) {
        this.boundsOfObj(objectId, label, false)
      },

      boundsOfObj(objectId, label, outbound) {
        let newTabName = ++this.tabIndex + '';
        let newTab = this.addObjRefsTab(this.buildTitle(this.$t(outbound ?
            'jifa.heap.ref.object.outgoing' : 'jifa.heap.ref.object.incoming'), label),
            newTabName, this.loadObjBounds, outbound)
        newTab.loading = true
        axios.get(heapDumpService(this.file, 'object'), {
          params: {
            objectId: objectId
          }
        }).then((resp) => {
              let topItems = [];
              let d = resp.data
              topItems.push(
                  {
                    rowKey: rowKey++,
                    icon: outbound ? getOutboundIcon(d.gCRoot, d.objectType) : getIcon(d.gCRoot, d.objectType),
                    label: d.label,
                    suffix: d.suffix,
                    shallowHeap: d.shallowSize,
                    retainedHeap: d.retainedSize,
                    hasChildren: true,
                    objectId: d.objectId,
                    isResult: true
                  }
              )
              newTab.topItems = topItems
              newTab.loading = false
            }
        )
      },

      outgoingRefsOfHistogramObjs(objectId, label) {
        this.boundsOfHistogramObjs(objectId, label, true)
      },

      incomingRefsOfHistogramObjs(objectId, label) {
        this.boundsOfHistogramObjs(objectId, label, false)
      },

      boundsOfHistogramObjs(objectId, label, outbound) {
        let newTabName = ++this.tabIndex + '';
        let newTab = this.addObjRefsTab(this.buildTitle(this.$t(outbound ?
                'jifa.heap.ref.object.outgoing' : 'jifa.heap.ref.object.incoming'), label),
            newTabName, this.loadObjBounds, outbound)
        newTab.nextPage = 1;
        newTab.records = [];
        newTab.classId = objectId;
        this.loadHistogramObjs();
      },

      loadHistogramObjs() {
        let index = this.currentIndex()
        let tab = this.editableTabs[index]
        tab.loading = true
        axios.get(heapDumpService(this.file, 'histogram/objects'), {
          params: {
            classId: tab.classId,
            page: tab.nextPage,
            pageSize: this.pageSize
          }
        }).then((resp) => {
              let records = resp.data.data
              records.forEach(item => tab.records.push({
                rowKey: rowKey++,
                icon: tab.outbound ? getOutboundIcon(item.gCRoot, item.objectType) : getIcon(item.gCRoot, item.objectType),
                label: item.label,
                suffix: item.suffix,
                shallowHeap: item.shallowSize,
                retainedHeap: item.retainedSize,
                hasChildren: true,
                objectId: item.objectId,
                isResult: true,
              }))

              tab.topItems = tab.records.concat({
                rowKey: rowKey++,
                currentSize: tab.records.length,
                totalSize: resp.data.totalSize,
                isHistogramObjsSummary: true
              })

              tab.nextPage++
              tab.loading = false
            }
        )
      },

      addObjRefsTab(title, name, load, outbound) {
        this.tabIndex++;
        let newTab = {
          title: title,
          name: name,
          loading: false,
          topItems: [],
          load: load,
          outbound: outbound,
          isObjRefsTab: true
        }
        this.editableTabs.push(newTab)
        this.editableTabsValue = name;
        return newTab
      },

      loadClassBounds(tree, treeNode, resolve) {
        this.fetchClassBounds(tree.rowKey, tree.objectIds, 1, resolve)
      },

      fetchClassBounds(parentRowKey, objectIds, page, resolve) {
        let index = this.currentIndex()
        let tab = this.editableTabs[index]
        let outbound = tab.outbound
        let table = this.$refs.resultContainer[index]
        tab.loading = true
        axios.get(heapDumpService(this.file, outbound ? 'classReference/outbounds/children' : 'classReference/inbounds/children'), {
          params: {
            objectIds: JSON.stringify(objectIds),
            page: page,
            pageSize: this.pageSize,
          }
        }).then(resp => {
          let loadedLen = 0;
          let loaded = table.store.states.lazyTreeNodeMap[parentRowKey]
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
              label: item.label,
              hasChildren: true,
              objectId: item.objectId,
              objectIds: item.objectIds,
              objects: item.objects,
              shallowHeap: item.shallowSize,
              icon: outbound ? getClassRefOutboundIcon(item.type) : getClassRefInboundIcon(item.type),
              isResult: true
            })
          })

          loaded.push({
            rowKey: rowKey++,
            objectIds: objectIds,
            parentRowKey: parentRowKey,
            isBoundsSummary: true,
            nextPage: page + 1,
            currentSize: loadedLen + res.length,
            totalSize: resp.data.totalSize,
            resolve: resolve,
          })

          if (callResolve) {
            resolve(loaded)
          }
          tab.loading = false
        })
      },

      outgoingRefsOfClass(objectId, label) {
        this.boundsOfClass(objectId, label, true)
      },

      incomingRefsOfClass(objectId, label) {
        this.boundsOfClass(objectId, label, false)
      },

      boundsOfClass(id, label, outbound) {
        let newTabName = ++this.tabIndex + '';
        let newTab = this.addClassRefsTab(this.buildTitle(this.$t(outbound ?
            'jifa.heap.ref.type.outgoing' : 'jifa.heap.ref.type.incoming'), label),
            newTabName, this.loadClassBounds, outbound)
        newTab.loading = true
        axios.get(heapDumpService(this.file, outbound ? 'classReference/outbounds/class' : 'classReference/inbounds/class'), {
          params: {
            objectId: id,
          }
        }).then((resp) => {
              let topItems = [];
              let item = resp.data
              topItems.push(
                  {
                    rowKey: rowKey++,
                    label: item.label,
                    hasChildren: true,
                    objectId: item.objectId,
                    objectIds: item.objectIds,
                    objects: item.objects,
                    shallowHeap: item.shallowSize,
                    icon: outbound ? getClassRefOutboundIcon(item.type) : ICONS.objects.class,
                    isResult: true
                  }
              )
              newTab.topItems = topItems
              newTab.loading = false
            }
        )
      },

      addClassRefsTab(title, name, load, outbound) {
        this.tabIndex++;
        let newTab = {
          title: title,
          name: name,
          loading: false,
          topItems: [],
          load: load,
          outbound: outbound,
          isClassRefsTab: true
        }
        this.editableTabs.push(newTab)
        this.editableTabsValue = name;
        return newTab
      },

      pathToGCRootsOfObj(objectId, label) {
        let newTabName = ++this.tabIndex + '';
        this.loadMorePathToGCRootsOfObj(this.addPathToGCRootsTab(this.buildTitle(this.$t('jifa.heap.pathToGCRoots'), label),
            newTabName, objectId))
      },

      loadMorePathToGCRootsOfObj(tab) {
        tab.loading = true
        axios.get(heapDumpService(this.file, 'pathToGCRoots'), {
          params: {
            skip: tab.count,
            origin: tab.origin,
            count: 10
          }
        }).then((resp) => {
          tab.count += resp.data.count
          tab.hasMore = resp.data.hasMore
          if (tab.treeData.length === 0) {
            // first
            tab.treeData.push(resp.data.tree)
          } else {
            this.mergePath(resp.data.tree.children, tab.treeData[0])
          }
          tab.loading = false
        })
      },

      mergePath(children, parent) {
        for (let i = 0; i < children.length; i++) {
          let found = false
          let child = children[i]
          for (let j = 0; j < parent.children.length; j++) {
            let oldChild = parent.children[j]
            if (oldChild.objectId === child.objectId) {
              found = true
              this.mergePath(child.children, oldChild)
              break;
            }
          }

          if (!found) {
            parent.children.push(child)
          }
        }
      },

      addPathToGCRootsTab(title, name, objectId) {
        this.tabIndex++;
        let newTab = {
          title: title,
          name: name,
          loading: false,
          isPathToGCRootsTab: true,
          origin: objectId,
          treeData: [],
          hasMore: false,
          count: 0,
        }
        this.editableTabs.push(newTab)
        this.editableTabsValue = name;
        return newTab
      },

      removeTab(targetName) {
        let tabs = this.editableTabs;
        let activeName = this.editableTabsValue;
        if (activeName === targetName) {
          tabs.forEach((tab, index) => {
            if (tab.name === targetName) {
              let nextTab = tabs[index + 1] || tabs[index - 1];
              if (nextTab) {
                activeName = nextTab.name;
              }
            }
          });
        }

        this.editableTabsValue = activeName;
        this.editableTabs = tabs.filter(tab => tab.name !== targetName);
      },

      //  ============ mergePathToGCRootS methods ====================
      spanMethod(row) {
        let index = row.columnIndex;
        if (index === 0) {
          return [1, 6]
        } else if (index >= 1 && index <= 5) {
          return [0, 0]
        }
        return [1, 1]
      },

      getIconWrapper(gCRoot, objectType) {
        if (this.grouping === 'BY_CLASS') {
          return ICONS.objects.class
        }
        return getIcon(gCRoot, objectType)
      },

      mergePathToGCRootsFromDominatorTree(objectIds, label) {
        let newTabName = ++this.tabIndex + '';
        let newTab = this.addPathToGCRootsFromDominatorTreeTab(
            this.buildTitle(this.$t('jifa.heap.mergePathToGCRoots'), label), newTabName, this.loadMergePathToGCChildren, objectIds);
        this.fetchMergePathToGCRootsNextPageData(newTab);
      },

      mergePathToGCRootsFromHistogram(classId, label) {
        let newTabName = ++this.tabIndex + '';
        let newTab = this.addPathToGCRootsFromHistogramTab(
                this.buildTitle(this.$t('jifa.heap.mergePathToGCRoots'), label), newTabName, this.loadMergePathToGCChildren, classId);
        this.fetchMergePathToGCRootsNextPageData(newTab);
      },

      addPathToGCRootsFromHistogramTab(title, name, load, classId) {
        return this.addPathToGCRoots({title: title, name: name, load: load, classId: classId, fromHistogram: true});
      },

      addPathToGCRootsFromDominatorTreeTab(title, name, load, objectIds) {
        return this.addPathToGCRoots({title: title, name: name, load: load, objectIds: objectIds, fromDominatorTree: true});
      },

      addPathToGCRoots({title, name, load, classId = null, objectIds = null, fromHistogram = false, fromDominatorTree = false} = { }) {
        this.tabIndex++;
        let newTab = {
          fromHistogram: fromHistogram,
          fromDominatorTree: fromDominatorTree,
          title: title,
          name: name,
          loading: false,
          isMergePathToGCRootsTab: true,
          classId: classId,
          objectIds: objectIds,
          tableData: [],
          load: load,
          grouping: 'FROM_GC_ROOTS',
          nextPage: 1,
          pageSize: 25,
          totalSize: 0,
          records: [],
        };
        this.editableTabs.push(newTab);
        this.editableTabsValue = name;
        return newTab;
      },

      fetchMergePathToGCRootsNextPageData(mergePathToGCRootsTab) {
        mergePathToGCRootsTab.loading = true;
        let api = "";
        let params = {};
        let method = "";
        let data = null;
        if (mergePathToGCRootsTab.fromHistogram) {
          api = 'mergePathToGCRoots/roots/byClassId';
          method = 'get';
          params = {
            classId: mergePathToGCRootsTab.classId,
            page: mergePathToGCRootsTab.nextPage,
            pageSize: mergePathToGCRootsTab.pageSize,
            grouping: mergePathToGCRootsTab.grouping
          }
        } else if (mergePathToGCRootsTab.fromDominatorTree) {
          api = 'mergePathToGCRoots/roots/byObjectIds';
          method = 'post';
          data = JSON.stringify(mergePathToGCRootsTab.objectIds);
          params = {
            page: mergePathToGCRootsTab.nextPage,
            pageSize: mergePathToGCRootsTab.pageSize,
            grouping: mergePathToGCRootsTab.grouping
          }
        }
        axios(heapDumpService(this.file, api), {method: method, data: data, params: params}).then(resp => {
          mergePathToGCRootsTab.totalSize = resp.data.totalSize;
          let data = resp.data.data;
          data.forEach(d => {
            mergePathToGCRootsTab.records.push({
              rowKey: rowKey++,
              objectId: d.objectId,
              icon: this.getIconWrapper(d.gCRoot, d.objectType),
              label: d.className,
              suffix: d.suffix,
              refObjects: d.refObjects,
              shallowHeap: d.shallowHeap,
              refShallowHeap: d.refShallowHeap,
              retainedHeap: d.retainedHeap,
              hasChildren: true,
              isResult: true,
              objectIdPathInGCPathTree: [d.objectId],
            })
          });
          mergePathToGCRootsTab.tableData = mergePathToGCRootsTab.records.concat({
            rowKey: rowKey++,
            isSummaryItem: true,
          });
          mergePathToGCRootsTab.nextPage++;
          mergePathToGCRootsTab.loading = false;
        })
      },

      loadMergePathToGCChildren(tree, treeNode, resolve) {
        this.fetchMergePathToGCRootsChildren(tree, 1, tree.rowKey, resolve);
      },

      fetchMergePathToGCRootsChildren(row, page, parentRowKey, resolve) {
        let index = this.currentIndex();
        let tab = this.editableTabs[index];
        let table = this.$refs.resultContainer[index];
        tab.loading = true;

        let api = "";
        let params = {};
        let method = "";
        let data = null;
        if (tab.fromHistogram) {
          api = 'mergePathToGCRoots/children/byClassId';
          method = 'get';
          params = {
            classId: tab.classId,
            objectIdPathInGCPathTree: JSON.stringify(row.objectIdPathInGCPathTree),
            page: page,
            pageSize: tab.pageSize,
            grouping: tab.grouping
          };
        } else if (tab.fromDominatorTree) {
          api = 'mergePathToGCRoots/children/byObjectIds';
          method = 'post';
          data = JSON.stringify(tab.objectIds);
          params = {
            objectIdPathInGCPathTree: JSON.stringify(row.objectIdPathInGCPathTree),
            page: page,
            pageSize: tab.pageSize,
            grouping: tab.grouping
          };
        }

        axios(heapDumpService(this.file, api), {method: method, data: data, params: params}).then(resp => {
          let loadedLen = 0;
          let loaded = table.store.states.lazyTreeNodeMap[parentRowKey];
          let callResolve = false;
          if (loaded) {
            loadedLen = loaded.length;
            if (loadedLen > 0) {
              loaded.splice(--loadedLen, 1)
            }
          } else {
            loaded = [];
            callResolve = true;
          }

          let res = resp.data.data;
          res.forEach(d => {
            let objectIdPathInGCPathTreeCopy = Array.from(row.objectIdPathInGCPathTree);
            objectIdPathInGCPathTreeCopy.push(d.objectId);
            loaded.push({
              rowKey: rowKey++,
              objectId: d.objectId,
              icon: this.getIconWrapper(d.gCRoot, d.objectType),
              label: d.className,
              suffix: d.suffix,
              refObjects: d.refObjects,
              shallowHeap: d.shallowHeap,
              refShallowHeap: d.refShallowHeap,
              retainedHeap: d.retainedHeap,
              hasChildren: true,
              isResult: true,
              objectIdPathInGCPathTree: objectIdPathInGCPathTreeCopy,
            });
          });

          loaded.push({
            rowKey: rowKey++,
            objectId: row.objectId,
            parentRowKey: parentRowKey,
            isChildrenSummary: true,
            nextPage: page + 1,
            currentSize: loadedLen + res.length,
            totalSize: resp.data.totalSize,
            resolve: resolve,
            objectIdPathInGCPathTree: row.objectIdPathInGCPathTree,
          });

          if (callResolve) {
            resolve(loaded)
          }

          tab.loading = false
        })
      },
    },

    watch: {
      editableTabs() {
        if (this.editableTabs.length === 0) {
          this.$emit('disableShowDynamicResultSlot')
        }
      }
    }
  }
</script>

<style scoped>
  .custom-tree-node {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 14px;
    padding-right: 8px;
  }
</style>

<style>
  .resultDiv {
    height: 100%;
    position: relative
  }
</style>
