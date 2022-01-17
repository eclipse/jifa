<!--
    Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<template>
  <div>
    <el-row :gutter="10">
      <el-col :span="16">
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span>{{$t('jifa.gclog.gcCause')}}</span>
          </div>
          <el-table :data="cause"
                    :highlight-current-row="false"
                    :show-header="true"
                    stripe
                    v-loading="loading">
            <el-table-column width="300px" :label="$t('jifa.gclog.gcCause')">
              <template slot-scope="scope">
                {{scope.row.cause}}
                <el-tooltip
                    v-if="scope.row.hint !== undefined"
                    effect="dark"
                    :content="$t(scope.row.hint)"
                    placement="bottom">
                  <i class="el-icon-info"></i>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="count" :label="$t('jifa.gclog.count')">
            </el-table-column>
            <el-table-column prop="avgPause" :label="$t('jifa.gclog.avgPause')">
            </el-table-column>
            <el-table-column prop="maxPause" :label="$t('jifa.gclog.maxPause')">
            </el-table-column>
            <el-table-column prop="totalPause" :label="$t('jifa.gclog.totalPause')">
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span>{{$t('jifa.gclog.pausePercent')}}</span>
            <doughnut-chart :chart-data="chartData" :options="chartOptions" >
            </doughnut-chart>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-row><br/></el-row>
  </div>
</template>

<script>
  import DoughnutChart from '../charts/DoughnutChart'
  import axios from 'axios'
  import {formatTimePeriod, gclogService} from '@/util'
  import {setColorForDoughnutChart} from "@/components/gclog/ColorUtil";

  export default {
    props: ['file'],
    components: {
      DoughnutChart
    },
    data() {
      return {
        loading: false,
        cause: null,
        chartData: {},
        chartOptions: {
          legend: {
            fontsize: '12px',
            onClick: () => {
            },
          },
          tooltips: {
            callbacks: {
              label: (tooltipItem, data) => {
                const index = tooltipItem.index;
                const percent = (data.datasets[0].data[index] / this.totalPauseSum * 100).toFixed(2)
                return `${data.labels[index]}: ${percent}%`
              }
            }
          },
          responsive: true,
          maintainAspectRatio: false,
          pieceLabel: {
            mode: 'percentage',
            precision: 1
          },
        },
        totalPauseSum: 0,
      }
    },
    methods: {
      getHint(cause) {
        const mid = cause.indexOf(' - ')
        if (mid >= 0) {
          cause = cause.substr(mid + 3).trim()
        }
        switch (cause) {
          case "System.gc()": return 'jifa.gclog.cause.systemgc'
          case "JvmtiEnv ForceGarbageCollection": return 'jifa.gclog.cause.jvmti'
          case "GCLocker Initiated GC": return 'jifa.gclog.cause.gclocker'
          case "Heap Inspection Initiated GC": return 'jifa.gclog.cause.heapInspection'
          case "Heap Dump Initiated GC": return 'jifa.gclog.cause.heapDump'
          case "Allocation Failure": return 'jifa.gclog.cause.allocationFail'
          case "Metadata GC Threshold": return 'jifa.gclog.cause.metaspace'
          case "Ergonomics": return 'jifa.gclog.cause.ergonomics'
          case "G1 Evacuation Pause": return 'jifa.gclog.cause.g1Evacuation'
          case "G1 Humongous Allocation": return 'jifa.gclog.cause.humongous'
          case "Last ditch collection": return 'jifa.gclog.cause.lastDitch'
          case "Promotion failed": return 'jifa.gclog.cause.promotionFail'
          case "To-space Exhausted": return 'jifa.gclog.cause.toSpaceExhausted'
          case "Proactive": return 'jifa.gclog.cause.proactive'
          case "Allocation Rate": return 'jifa.gclog.cause.allocationRate'
          case "Timer": return 'jifa.gclog.cause.timer'
          case "Allocation Stall": return 'jifa.gclog.cause.allocationStall'
          case "High Usage": return 'jifa.gclog.cause.highUsage'
          case "Warmup": return 'jifa.gclog.cause.warmup'
          case "Metadata GC Clear Soft References":  return 'jifa.gclog.cause.metaspaceClearSoftRef'
          case "G1 Periodic Collection":  return 'jifa.gclog.cause.g1Periodic'
          case "Diagnostic Command": return 'jifa.gclog.cause.dcmd'
          case "G1 Compaction Pause": return 'jifa.gclog.cause.g1Compaction'
          case "G1 Preventive Collection": return 'jifa.gclog.cause.g1Preventive'

          // shenandoah
          case "Allocation Failure During Evacuation":
          case "Stopping VM":
          case "Concurrent GC":
          case "Upgrade To Full GC":
          case "FullGCAlot":
          case "ScavengeAlot":
          case "Allocation Profiler":
          case "WhiteBox Initiated Young GC":
          case "WhiteBox Initiated Concurrent Mark":
          case "Update Allocation Context Stats":
          case "No GC":
          case "Tenured Generation Full":
          case "CMS Generation Full":
          case "CMS Initial Mark":
          case "CMS Final Remark":
          case "CMS Concurrent Mark":
          case "Old Generation Expanded On Last Scavenge":
          case "Old Generation Too Full To Scavenge":
          case "ILLEGAL VALUE - last gc cause - ILLEGAL VALUE":
          case "WhiteBox Initiated Full GC":
            return undefined;
        }
        return undefined;
      }
    },
    created() {
      this.loading = true;
      axios.get(gclogService(this.file, 'gcCause')).then(resp => {
        this.totalPauseSum = 0
        this.cause = resp.data.map((causeItem => {
          this.totalPauseSum += causeItem.totalPause
          return {
            cause: causeItem.cause,
            count: causeItem.count,
            avgPause: formatTimePeriod(causeItem.avgPause),
            maxPause: formatTimePeriod(causeItem.maxPause),
            totalPause: formatTimePeriod(causeItem.totalPause),
            hint: this.getHint(causeItem.cause),
          }
        }));
        let labels = [];
        let data = [];
        resp.data.forEach(causeItem => {
          labels.push(causeItem.cause);
          data.push(causeItem.totalPause);
        })
        this.chartData = setColorForDoughnutChart({
          labels: labels,
          datasets: [
            {
              data: data,
            }
          ]
        })
        this.loading = false;
      });
    }
  }
</script>
