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
    <el-row :gutter="10">
      <el-col :span="16">
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span>{{$t('jifa.gclog.gcPhase')}}</span>
          </div>
          <el-table :data="phase"
                    :highlight-current-row="false"
                    :show-header="true"
                    stripe
                    v-loading="loading">
            <el-table-column width="300px" :label="$t('jifa.gclog.gcPhase')">
              <template slot-scope="scope">
                {{scope.row.name}}
                <el-tooltip
                    v-if="scope.row.stw"
                    effect="dark"
                    :content="$t('jifa.gclog.stwTooltip')"
                    placement="bottom">
                  <i class="el-icon-video-pause"></i>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="count" :label="$t('jifa.gclog.count')">
            </el-table-column>
            <el-table-column prop="avgTime" :label="$t('jifa.gclog.avgTime')">
            </el-table-column>
            <el-table-column prop="maxTime" :label="$t('jifa.gclog.maxTime')">
            </el-table-column>
            <el-table-column prop="totalTime" :label="$t('jifa.gclog.totalTime')">
            </el-table-column>
            <el-table-column prop="avgInterval" :label="$t('jifa.gclog.avgInterval')">
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
        phase: null,
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
    },
    created() {
      this.loading = true;
      axios.get(gclogService(this.file, 'gcPhase')).then(resp => {
        this.totalPauseSum = 0
        this.phase = resp.data.map((item => {
          this.totalPauseSum += item.totalTime
          return {
            name: item.name,
            count: item.count,
            stw: item.stw,
            avgTime: formatTimePeriod(item.avgTime),
            maxTime: formatTimePeriod(item.maxTime),
            totalTime: formatTimePeriod(item.totalTime),
            avgInterval: formatTimePeriod(item.avgInterval),
          }
        }));
        let labels = [];
        let data = [];
        resp.data.sort((p1, p2) => p2.totalTime - p1.totalTime).forEach(item => {
          labels.push(item.name);
          data.push(item.totalTime);
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
