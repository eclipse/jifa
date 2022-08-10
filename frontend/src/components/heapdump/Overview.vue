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
  <div>
    <v-contextmenu ref="contextmenu">
      <v-contextmenu-submenu :title="$t('jifa.heap.ref.object.label')"
                             v-if="selectedBigObject && selectedBigObject.objectId >=0">
        <v-contextmenu-item
                @click="$emit('outgoingRefsOfObj', selectedBigObject.objectId, selectedBigObject.label)">
          {{$t('jifa.heap.ref.object.outgoing')}}
        </v-contextmenu-item>
        <v-contextmenu-item
                @click="$emit('incomingRefsOfObj', selectedBigObject.objectId, selectedBigObject.label)">
          {{$t('jifa.heap.ref.object.incoming')}}
        </v-contextmenu-item>
      </v-contextmenu-submenu>

      <v-contextmenu-submenu :title="$t('jifa.heap.ref.type.label')"
                             v-if="this.selectedBigObject && this.selectedBigObject.objectId >=0">
        <v-contextmenu-submenu :title="$t('jifa.heap.ref.type.label')">
          <v-contextmenu-item
                  @click="$emit('outgoingRefsOfClass', selectedBigObject.objectId, selectedBigObject.label)">
            {{$t('jifa.heap.ref.type.outgoing')}}
          </v-contextmenu-item>
          <v-contextmenu-item
                  @click="$emit('incomingRefsOfClass', selectedBigObject.objectId, selectedBigObject.label)">
            {{$t('jifa.heap.ref.type.incoming')}}
          </v-contextmenu-item>
        </v-contextmenu-submenu>
      </v-contextmenu-submenu>

      <v-contextmenu-item divider v-if="selectedBigObject && selectedBigObject.objectId >=0"></v-contextmenu-item>

      <v-contextmenu-item
              v-if="selectedBigObject && selectedBigObject.objectId >=0"
              @click="$emit('pathToGCRootsOfObj', selectedBigObject.objectId, selectedBigObject.label)">
        {{$t('jifa.heap.pathToGCRoots')}}
      </v-contextmenu-item>
    </v-contextmenu>

    <el-collapse v-model="activeNames" style="width: 100%;">
      <el-collapse-item v-loading="loading" :title="$t('jifa.heap.basicInformation')" name="1">
        <el-table v-loading="loading"
                  :data="details"
                  :show-header="false"
                  :highlight-current-row="false"
                  stripe
                  :cell-style='cellStyle'
                  height="280px"
        >
          <el-table-column prop="key" width="300px">
          </el-table-column>
          <el-table-column prop="value">
          </el-table-column>
        </el-table>
      </el-collapse-item>

      <el-collapse-item v-loading="loading" title='Biggest Objects by Retained Size' name="2">
        <doughnut-chart :chart-data="chartData" :options="chartOptions" v-contextmenu:contextmenu></doughnut-chart>
        <p style='margin-top: 20px; margin-bottom: -20px; font-size: 13px; font-weight: 500; color: #606266;'>
          {{selectedBigObject ? buildSelectedBigObjectInfo() : $t("jifa.heap.usedHeapSize") + ': '
          +toReadableSizeWithUnit(totalSize)}}
        </p>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>
<script>
  import DoughnutChart from '../charts/DoughnutChart'
  import axios from 'axios'
  import {heapDumpService, toReadableCountWithUnit, toReadableSizeWithUnit} from '../../util'
  import {formatDate} from 'element-ui/src/utils/date-util'
  import {a2rgb, PIE_COLORS, REMAINDER_COLOR} from "./ColorHelper";

  export default {
    props: ['file'],
    components: {
      DoughnutChart
    },
    methods: {
      toReadableSizeWithUnit,
      selectBigObject(event, elements) {
        if (elements.length > 0) {
          let index = elements[0]._index
          this.selectedBigObject = this.bigObjects[index]
          this.$emit("setSelectedObjectId", this.selectedBigObject.objectId)
        } else {
          this.selectedBigObject = null
        }
      },
      buildSelectedBigObjectInfo() {
        let obj = this.selectedBigObject
        return obj.label + ' [' + toReadableSizeWithUnit(obj.value) + ', '
            + ((obj.value) / this.totalSize * 100).toPrecision(3) + '%]'
      }
    },
    data() {
      return {
        loading: false,
        activeNames: ['1', '2'],
        details: null,
        totalSize: 0,
        selectedBigObject: null,
        cellStyle: {padding: '8px'},

        bigObjects: [],
        chartData: {},
        chartOptions: {
          legend: false,
          responsive: true,
          maintainAspectRatio: false,
          pieceLabel: {
            mode: 'percentage',
            precision: 1
          },
          animation: {
            // animateScale: true,
            // easing: 'linear'
          },
          onClick: this.selectBigObject
        }
      };
    },
    created() {
      this.loading = true

      axios.get(heapDumpService(this.file, 'details')).then(resp => {

        this.$emit("setGenerationInfoAvailable", resp.data.generationInfoAvailable)

        this.totalSize = resp.data.usedHeapSize
        this.details = [
          {
            key: this.$t("jifa.heap.usedHeapSize"),
            value: toReadableSizeWithUnit(resp.data.usedHeapSize)
          },
          {
            key: this.$t("jifa.heap.numberOfClasses"),
            value: toReadableCountWithUnit(resp.data.numberOfClasses)
          },
          {
            key: this.$t("jifa.heap.numberOfObjects"),
            value: toReadableCountWithUnit(resp.data.numberOfObjects)
          },
          {
            key: this.$t("jifa.heap.numberOfClassLoaders"),
            value: toReadableCountWithUnit(resp.data.numberOfClassLoaders)
          },
          {
            key: this.$t("jifa.heap.numberOfGCRoots"),
            value: toReadableCountWithUnit(resp.data.numberOfGCRoots)
          },
          {
            key: this.$t("jifa.heap.heapCreationDate"),
            value: formatDate(new Date(resp.data.creationDate), 'yyyy-MM-dd HH:mm:ss')
          },
          {
            key: this.$t("jifa.heap.OSBit"),
            value: resp.data.identifierSize === 8 ? '64 bit' : '32 bit'
          },
        ]

        axios.get(heapDumpService(this.file, 'biggestObjects')).then(resp => {
          this.bigObjects = resp.data;
          let labels = []
          let data = []
          for (let i = 0; i < resp.data.length; i++) {
            labels.push(this.bigObjects[i].label)
            data.push(this.bigObjects[i].value)
          }

          let color = []
          for (let i = 0; i < resp.data.length - 1; i++) {
            color.push(a2rgb(PIE_COLORS[i % PIE_COLORS.length]))
          }

          if (this.bigObjects[this.bigObjects.length - 1].objectId === -1
              || this.bigObjects[this.bigObjects.length - 1].label === 'Remainder') {
            color.push(a2rgb(REMAINDER_COLOR))
          } else {
            color.push(a2rgb((this.bigObjects.length - 1) % PIE_COLORS.length))
          }

          this.chartData = {
            labels: labels,
            datasets: [
              {
                backgroundColor: color,
                data: data,
              }
            ]
          }
          this.loading = false
        }).catch(() => {
          this.chartData = {}
        })
      })
    }
  };
</script>