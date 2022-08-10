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
  <div style="height: 100%; position: relative">
    <el-collapse v-if="useful" v-model="activeNames" style="width: 100%">
      <el-collapse-item v-loading="loading" :title='$t("jifa.heap.overview")' name="0">
        <div style="position: relative;">
          <div style="position: absolute; top: 30px; left: 30px;" v-html="descOfSelectedSlice"></div>
          <div>
            <doughnut-chart :chart-data="chartData" :options="chartOptions"></doughnut-chart>
          </div>
        </div>
      </el-collapse-item>
      <el-collapse-item v-for="(record) in records" v-bind:key="record.name" :title="record.name" :name="record.name">
        <el-tabs tab-position="left">
          <el-tab-pane :label="$t('jifa.heap.description')">
            <div v-html="record.desc"></div>
          </el-tab-pane>
          <el-tab-pane v-if="record.paths" :label="$t('jifa.heap.detail')">
            <el-tree
                    :data="record.paths"
                    node-key="objectId"
            >
              <span class="custom-tree-node" style="font-size: 12px" slot-scope="{ node, data }"
                    @click="$emit('setSelectedObjectId', data.objectId)">
                <span>
                  <img :src="getIcon(data.gCRoot, data.objectType)" style="margin-right: 5px"/>
                  {{ data.label }}
                </span>
              <span>
                    {{ toReadableSizeWithUnit(data.shallowSize) }} / {{ toReadableSizeWithUnit(data.retainedSize) }}
              </span>
            </span>

            </el-tree>
          </el-tab-pane>
        </el-tabs>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script>
  import axios from 'axios'
  import DoughnutChart from '../charts/DoughnutChart'
  import {heapDumpService, toReadableSizeWithUnit} from '../../util'
  import {a2rgb, PIE_COLORS, REMAINDER_COLOR} from "./ColorHelper";
  import {getIcon} from "./IconHealper"

  export default {
    props: ['file'],
    components: {
      DoughnutChart
    },
    methods: {
      toReadableSizeWithUnit,
      getIcon,
      clickPie(event, elements) {
        if (elements.length > 0) {
          let index = elements[0]._index
          this.descOfSelectedSlice = this.slices[index].desc
          this.$emit("setSelectedObjectId", this.slices[index].objectId)
        } else {
          this.descOfSelectedSlice = null
        }
      },
      fetchReport() {
        this.loading = true
        axios.get(heapDumpService(this.file, 'leak/report')).then(resp => {
          this.useful = resp.data.useful
          if (this.useful) {
            this.slices = resp.data.slices
            let labels = []
            let data = []
            for (let i = 0; i < this.slices.length; i++) {
              labels.push(this.slices[i].label)
              data.push(this.slices[i].value)
            }

            let color = []
            let len = this.slices.length
            for (let i = 0; i < len - 1; i++) {
              color.push(a2rgb(PIE_COLORS[i % PIE_COLORS.length]))
            }
            if (this.slices[len - 1].objectId === -1
                || this.slices[len - 1].label === 'Remainder') {
              color.push(a2rgb(REMAINDER_COLOR))
            } else {
              color.push(a2rgb((len - 1) % PIE_COLORS.length))
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

            this.records = resp.data.records
            this.records.forEach(
                r => {
                  this.activeNames.push(r.name)
                }
            )
          }
          this.loading = false
        })
      }

    },
    data() {
      return {
        activeNames: ['0'],
        loading: false,

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
          onClick: this.clickPie
        },

        useful: true,
        slices: null,
        descOfSelectedSlice: null,
        records: null,
      }
    },
    created() {
      this.fetchReport()
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