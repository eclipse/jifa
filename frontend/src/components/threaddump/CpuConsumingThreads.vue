<!--
    Copyright (c) 2022 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<template>
    <div>
      <el-dialog :visible.sync="threadTableVisible" width="60%" top="5vh">
        <thread :file="file" :id="selectedThreadId" />
      </el-dialog>
    
      <el-container>
      <div>
          <span>{{ $t('jifa.threadDump.cpuConsumingThreads.title') }}</span>
        </div>
      </el-container>
      <el-container>
        <div>
          <el-col :span="20"><bar-chart :chart-data="threadChartData" :options="threadChartOptions" :width='1000' :height='400' /></el-col>
        </div>
      
        </el-container>
    </div>
</template>

<script>
import axios from 'axios';
import {threadDumpService, determineTimeUnit, formatTimeDuration } from "@/util";
import Thread from "@/components/threaddump/Thread";
import BarChart from '../charts/BarChart'

export default {
  props: ['file'],
  components: {
    Thread,
    BarChart,
  },
  data() {
    return {
        color: [
        '#003f5c',
        '#2f4b7c',
        '#665191',
        '#a05195',
        '#d45087',
        '#f95d6a',
        '#ff7c43',
        '#ffa600',
        '#488f31',
        '#8aa1b4'
        ],
        analysisState: 'NOT_STARTED',
        progressState: 'info',
        loading: true,
        message: '',
        progress: 0,
        pollingInternal: 500,
        files: [],
        threadTableVisible: false,
        selectedThreadId: null,

        threadChartData: {
        },
        threadChartOptions: {
          responsive: false,
          maintainAspectRatio: false,
        },
 
      }
  },
  methods: {
    loadData() {
      let self = this
      axios.get(threadDumpService(this.file, "cpuConsumingThreads"), {
        params: {
          max: 10,
        }
      }).then(resp => {
        let data = resp.data
        let datasets = []
        let unit = 'seconds'
        if(data.length > 0) {
            unit = determineTimeUnit(data[0].cpu)
        }
        data.forEach((thread, index) => {
          datasets.push({
            data: [{
              x: thread.name,
              y: formatTimeDuration(thread.cpu, unit),
              id: thread.id,
            }],
            backgroundColor: self.color[index%self.color.length],
            label: thread.name,
          })
        })
        this.threadChartData = {
          datasets: datasets,
          labels: [self.$t('jifa.threadDump.cpuConsumingThreads.cpuConsumptionLabel') + " ( " + self.$t('jifa.threadDump.cpuConsumingThreads.'+ unit ) + " )"]
        }

        this.threadChartOptions.onClick = function (e) {
            var node = this.getElementsAtEventForMode(e,"dataset",{ intersect: true }, true)
            if(node) {
              let datasetIndex = node[0]._datasetIndex
              self.onCickChart(datasetIndex)
            } 
          }
      })
    },

    // opens the thread at the given dataset index
    onCickChart(index) {
      let dataset = this.threadChartData.datasets[index]
      this.selectThreadId(dataset.data[0].id)
    },

    selectThreadId(id) {
      this.selectedThreadId = id
      this.threadTableVisible = true
    },
  },
  mounted() {
    this.loadData()
  },

};
</script>