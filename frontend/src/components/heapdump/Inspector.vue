<!--
    Copyright (c) 2020 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<template xmlns:v-clipboard="http://www.w3.org/1999/xhtml">
  <el-container style="height: 100%">
    <el-main style="padding: 0; height: 100%">
      <div style="height: 100%; position: relative;">
        <el-tabs class="firstTabs" value="inspectorObjectView" style="position:relative; height: 38%;">
          <el-tab-pane name="inspectorObjectView">
          <span slot="label">
            <i class="el-icon-search"/>
            <span style="margin-left: 2px"> Inspector</span>
          </span>

            <div style="margin-bottom: 5px">
              <el-input size="mini"
                        width="60%"
                        placeholder="Object Address"
                        class="input-with-select"
                        v-model="objectAddress"
                        @keyup.enter.native="searchByAddress"
                        clearable>
                <el-button slot="append" :icon="searching ? 'el-icon-loading' : 'el-icon-search'"
                           :disabled="searching"
                           @click="searchByAddress"/>
              </el-input>
            </div>


            <el-table v-loading="viewLoading"
                      :data="objectOverview"
                      :show-header="false"
                      :highlight-current-row="true"
                      stripe
                      :cell-style="cellStyle"
                      empty-text=" "
                      height="80%"
            >
              <el-table-column align="left" show-overflow-tooltip>
                <template slot-scope="scope">
                  <p style="font-size: 12px; margin: 0 auto;">
                    <img :src="scope.row.icon"/> {{ scope.row.data}}
                    <span style="font-weight: bold; color: #909399"
                          v-if="scope.row.suffix">{{ " " + scope.row.suffix}}</span>
                    <i v-if="scope.row.copyable" class="el-icon-document-copy copy-icon"
                       style="padding-left: 5px"
                       v-clipboard:copy="scope.row.data"/>
                  </p>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>

        <el-tabs :value="activeName"
                 class="bottomTabs" style="position: absolute; top: 38%; left: 0; right: 0; bottom: 0;">
          <el-tab-pane label="Attributes" name="Attributes">
            <Fields :file="file" :object-id="objectId"/>
          </el-tab-pane>
          <el-tab-pane label="Statics" name="Statics">
            <div style="height: 100%">
              <Fields :file="file" :object-id="objectId" static/>
            </div>
          </el-tab-pane>
          <!--            <el-tab-pane label="Class Hierarchy" name="Class Hierarchy" disabled>-->
          <!--            </el-tab-pane>-->
          <el-tab-pane label="Value" name="Value">
            <div v-loading="valueLoading">
              <el-input
                      v-model="objectValue"
                      type="textarea"
                      style="font-size: 12px"
                      readonly
                      autosize>
              </el-input>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-main>
  </el-container>
</template>
<script>

  import axios from 'axios'
  import {heapDumpService, toReadableSizeWithUnit} from '../../util'
  import {getIcon, ICONS} from './IconHealper';
  import {OBJECT_TYPE} from './CommonType';
  import Fields from './Fields'

  export default {
    components: {Fields},
    props: ['file', 'objectId'],
    data() {
      return {
        objectOverview: [],
        activeName: 'Attributes',
        cellStyle: {padding: 0},
        objectValue: '',
        objectAddress: '',
        searching: false,
        viewLoading: false,
        valueLoading: false,
      }
    },
    methods: {
      locationDesc(locationType) {
        if (locationType === 1) {
          return "Young"
        }

        if (locationType === 2) {
          return "Old"
        }
        return ""
      },
      searchByAddress() {
        if (this.objectAddress) {
          this.searching = true
          let address = this.objectAddress
          if (address.startsWith('0x') || address.startsWith('0X')) {
            address = address.substring(2)
          }
          address = parseInt(address, 16);
          axios.get(heapDumpService(this.file, "inspector/addressToId"), {
            params: {
              objectAddress: address
            }
          }).then(resp => {
            this.$emit('setSelectedObjectId', resp.data)
            this.$emit('outgoingRefsOfObj', resp.data, this.objectAddress)
            this.searching = false
          }).catch(() => {
            this.objectAddress = null
            this.searching = false
          })
        }
      },
      inspect() {
        if (!this.objectId) {
          return
        }

        this.viewLoading = true
        axios.get(heapDumpService(this.file, "inspector/objectView"), {
          params: {
            objectId: this.objectId
          }
        }).then(resp => {
          let ov = resp.data

          let oa = '0x' + ov.objectAddress.toString(16)

          let tmpView = []
          tmpView.push({data: oa, icon: ICONS.id, suffix: this.locationDesc(ov.locationType), copyable: true})
          let name = ov.name
          let p = ''
          let dotPos = name.lastIndexOf(".")
          if (dotPos >= 0) {
            p = name.substring(0, dotPos)
            name = name.substring(dotPos + 1)
          }
          tmpView.push({data: name, icon: getIcon(ov.gCRoot, ov.objectType)})
          tmpView.push({data: p, icon: ICONS.objects.package})

          tmpView.push({
            data: ov.classLabel,
            icon: getIcon(ov.classGCRoot, OBJECT_TYPE.CLASS)
          })

          tmpView.push({data: ov.superClassName ? ov.superClassName : ' ', icon: ICONS.objects.superclass})

          tmpView.push({
            data: ov.classLoaderLabel,
            icon: getIcon(ov.classLoaderGCRoot, OBJECT_TYPE.CLASSLOADER)
          })
          tmpView.push({data: toReadableSizeWithUnit(ov.shallowSize) + ' (shallow size)', icon: ICONS.size})
          tmpView.push({data: toReadableSizeWithUnit(ov.retainedSize) + ' (retained size)', icon: ICONS.size})
          tmpView.push({data: ov.gcRootInfo, icon: ICONS.decorations.gc_root})
          this.objectOverview = tmpView
          let maxContent = ''
          tmpView.forEach(v => {
            if (v.data.length > maxContent.length) {
              maxContent = v.data
            }
          })
          if (ov.objectType === OBJECT_TYPE.CLASS) {
            this.activeName = 'Statics'
          } else {
            this.activeName = 'Attributes'
          }
          this.viewLoading = false
        }).catch(() => {
          this.objectOverview = []
          this.viewLoading = false
        })

        this.valueLoading = true
        axios.get(heapDumpService(this.file, "inspector/value"), {
          params: {
            objectId: this.objectId
          }
        }).then(resp => {
          this.objectValue = resp.data
          this.valueLoading = false
        }).catch(() => {
          this.valueLoading = false
        })
      },
    },
    watch: {
      objectId(id) {
        if (id >= 0) {
          this.inspect()
        } else {
          this.objectOverview = []
        }
      },
    },
    mounted() {
      this.inspect()
    }
  }
</script>

<style scoped>
  .copy-icon:hover {
    color: #409EFF;
    cursor: pointer;
  }

  .firstTabs /deep/ .el-tabs__content {
    position: absolute;
    top: 45px;
    left: 0;
    right: 0;
    bottom: 0;
    overflow: auto;
  }

  .firstTabs /deep/ .el-tab-pane {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    overflow: auto;
  }

  .bottomTabs /deep/ .el-tabs__content {
    position: absolute;
    top: 45px;
    left: 0;
    right: 0;
    bottom: 0;
    overflow: auto;
  }

  .bottomTabs /deep/ .el-tab-pane {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    overflow: auto;
  }
</style>
