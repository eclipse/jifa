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
  <div style="height: 100%; position: relative">
    <el-dialog :visible.sync="conceptRefVisible">
      <reference :content="refContent"/>
    </el-dialog>

    <el-dialog :visible.sync="threadTableVisible" width="60%" top="5vh">
      <thread :file="file" :group-name="selectedGroupName" :type="selectedThreadType"/>
    </el-dialog>

    <el-alert
        v-if="errorCount > 0"
        :title="$t('jifa.threadDump.errorPrompt') + errorCount"
        type="error"
        :closable="false">
    </el-alert>
    <el-collapse v-model="activeNames" style="width: 100%">
      <!-- basic info -->
      <el-collapse-item v-loading="loading" :title="$t('jifa.threadDump.basicInfo')" name="basicInfo">
        <el-table v-loading="loading"
                  :data="basicInfo"
                  :show-header="false"
                  stripe
                  :cell-style='cellStyle'>
          <el-table-column>
            <template slot-scope="scope">
              <i v-if="scope.row.icon" :class="scope.row.icon"/>
              <span v-if="!scope.row.ref" style="margin-left: 10px">{{ scope.row.key }}</span>
              <span v-else class="key-span" @click="refContent = scope.row.ref; conceptRefVisible = true">
                {{ scope.row.key }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="value">
          </el-table-column>
        </el-table>
      </el-collapse-item>

      <!-- threads -->
      <el-collapse-item v-loading="loading" :title="$t('jifa.threadDump.threadSummary')" name="threadSummary">
        <el-table v-loading="loading"
                  :data="threadStats"
                  :show-header="false"
                  stripe
                  :cell-style='cellStyle'>
          <el-table-column type="expand">
            <template slot-scope="scope">
              <doughnut-chart :chart-data="scope.row.chartData" :options="chartOptions"/>
            </template>
          </el-table-column>

          <el-table-column>
            <template slot-scope="scope">
              <i v-if="scope.row.icon" :class="scope.row.icon"/>
              <span class="key-span" @click="selectThreadType(scope.row.threadType)">
                {{ scope.row.key }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="value">
          </el-table-column>
        </el-table>
      </el-collapse-item>

      <!-- thread groups -->
      <el-collapse-item v-loading="loading" v-if="threadGroupStats && threadGroupStats.length > 0"
                        :title="$t('jifa.threadDump.threadGroupSummary')" name="threadGroupSummary">
        <el-table v-loading="loading"
                  :data="threadGroupStats"
                  :show-header="false"
                  stripe
                  :cell-style='cellStyle'>
          <el-table-column type="expand">
            <template slot-scope="scope">
              <el-tag v-for="(i) in vIndices(scope.row.counts)" :label="scope.row.states[i]" v-bind:key="i"
                      style="margin-right: 10px">
                {{ scope.row.states[i] }} : {{ scope.row.counts[i] }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column>
            <template slot-scope="scope">
              <i class="el-icon-notebook-2"/>
              <span class="key-span" @click="selectThreadGroupName(scope.row.key)">
                {{ scope.row.key }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="value">
          </el-table-column>
        </el-table>
      </el-collapse-item>

      <!-- call site tree -->
      <el-collapse-item title="Java Monitors" name="monitors">
        <monitor :file="file"/>
      </el-collapse-item>

      <!-- call site tree -->
      <el-collapse-item :title="$t('jifa.threadDump.callSiteTree')" name="callSiteTree">
        <call-site-tree :file="file"/>
      </el-collapse-item>

      <el-collapse-item :title="$t('jifa.threadDump.fileContent')" name="fileContent">
        <file-content :file="file"/>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script>
import axios from 'axios'
import DoughnutChart from '../charts/DoughnutChart'
import Reference from "@/components/reference";
import Thread from "@/components/threaddump/Thread";
import Monitor from "@/components/threaddump/Monitor";
import CallSiteTree from "@/components/threaddump/CallSiteTree";
import FileContent from "@/components/threaddump/Content";

import {formatTime, threadDumpService} from '@/util'

export default {
  props: ['file'],
  components: {
    DoughnutChart,
    Reference,
    Thread,
    Monitor,
    CallSiteTree,
    FileContent,
  },
  data() {
    return {
      cellStyle: {padding: '8px'},
      loading: false,

      conceptRefVisible: false,
      refContent: '',

      selectedGroupName: null,
      selectedThreadType: null,
      threadTableVisible: false,

      basicInfo: null,
      threadStats: null,
      threadGroupStats: null,
      activeNames: ['basicInfo', 'threadSummary', 'threadGroupSummary', 'monitors', 'callSiteTree'],
      deadLockCount: 0,
      errorCount: 0,

      chartOptions: {
        legend: {
          position: 'right'
        },
        responsive: true,
        maintainAspectRatio: false,
        pieceLabel: {
          mode: 'percentage',
          precision: 1
        },
      },

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
    }
  },
  methods: {
    sum(arr) {
      let s = 0;
      for (let i = 0; i < arr.length; i++) {
        s += arr[i]
      }
      return s
    },

    vIndices(counts) {
      let vi = []
      for (let i = 0; i < counts.length; i++) {
        if (counts[i] > 0) {
          vi.push(i);
        }
      }
      return vi.sort((i, j) => counts[j] - counts[i])
    },

    sort() {
      this.threadStats.sort((i, j) => j.value - i.value)
      this.threadGroupStats.sort((i, j) => j.value - i.value)
    },

    selectThreadType(type) {
      this.selectedGroupName = null
      this.selectedThreadType = type
      this.threadTableVisible = true
    },

    selectThreadGroupName(name) {
      this.selectedThreadType = null
      this.selectedGroupName = name
      this.threadTableVisible = true
    }
  },
  mounted() {
    this.loading = true
    axios.get(threadDumpService(this.file, 'overview')).then(resp => {
      let overview = resp.data
      this.deadLockCount = overview.deadLockCount
      this.errorCount = overview.errorCount;
      this.basicInfo = [
        {
          key: this.$t("jifa.threadDump.time"),
          value: overview.timestamp > 0 ? formatTime(overview.timestamp, 'Y-M-D h:m:s') : '-',
          icon: 'el-icon-time',
        },
        {
          key: this.$t("jifa.threadDump.vmInfo"),
          value: overview.vmInfo,
          icon: 'el-icon-s-platform',
        },
        {
          key: this.$t("jifa.threadDump.jniRefs"),
          value: overview.jniRefs >= 0 && overview.jniWeakRefs >= 0
              ? (overview.jniRefs + ' (' + overview.jniWeakRefs + ' weak refs)')
              : (overview.jniRefs >= 0 ? overview.jniRefs : '-1'),
          icon: 'el-icon-connection',
          // ref: '# JNI Reference'
        },
      ]
      this.threadStats = [
        {
          key: this.$t("jifa.threadDump.javaThread"),
          value: this.sum(overview.javaThreadStat.javaCounts),
          states: overview.javaStates,
          counts: overview.javaThreadStat.javaCounts,
          icon: 'el-icon-coffee-cup',
          chartData: {
            labels: overview.javaStates,
            datasets: [
              {
                data: overview.javaThreadStat.javaCounts,
                backgroundColor: this.color,
              }
            ]
          },
          threadType: "JAVA",
        },
        {
          key: this.$t("jifa.threadDump.jitThread"),
          value: this.sum(overview.jitThreadStat.counts),
          states: overview.states,
          counts: overview.jitThreadStat.counts,
          icon: 'el-icon-s-promotion',
          chartData: {
            labels: overview.states,
            datasets: [
              {
                data: overview.jitThreadStat.counts,
                backgroundColor: this.color,
              }
            ]
          },
          threadType: "JIT",
        },
        {
          key: this.$t("jifa.threadDump.gcThread"),
          value: this.sum(overview.gcThreadStat.counts),
          states: overview.states,
          counts: overview.gcThreadStat.counts,
          icon: 'el-icon-delete',
          chartData: {
            labels: overview.states,
            datasets: [
              {
                data: overview.gcThreadStat.counts,
                backgroundColor: this.color,
              }
            ]
          },
          threadType: "GC",
        },
        {
          key: this.$t("jifa.threadDump.otherThread"),
          value: this.sum(overview.otherThreadStat.counts),
          states: overview.states,
          counts: overview.otherThreadStat.counts,
          icon: 'el-icon-s-operation',
          chartData: {
            labels: overview.states,
            datasets: [
              {
                data: overview.otherThreadStat.counts,
                backgroundColor: this.color,
              }
            ]
          },
          threadType: "VM",
        },
      ]
      this.threadGroupStats = []
      for (let k in overview.threadGroupStat) {
        this.threadGroupStats.push({
          key: k,
          value: this.sum(overview.threadGroupStat[k].counts),
          states: overview.states,
          counts: overview.threadGroupStat[k].counts,
        })
      }
      this.sort();
      this.threadStats.push({
        key: this.$t("jifa.threadDump.total"),
        value: this.sum(overview.threadStat.counts),
        states: overview.states,
        counts: overview.threadStat.counts,
        icon: 'el-icon-s-data',
        chartData: {
          labels: overview.states,
          datasets: [
            {
              data: overview.threadStat.counts,
              backgroundColor: this.color,
            }
          ]
        },
      })
      this.loading = false
    })
  }
}
</script>

<style scoped>
.key-span {
  margin-left: 10px;
  color: #409eff;
  cursor: pointer;
}
</style>
