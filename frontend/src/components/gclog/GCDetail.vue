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
  <div id="gc-detail-parent">
    <el-col :span="22" :offset="1">
      <el-card>
        <div slot="header" class="filter">
          <div>{{ $t("jifa.gclog.detail.filters") }}</div>

          <div style="margin-left: 40px" class="filter-item">
            <span>{{ $t('jifa.gclog.detail.eventType') + ": " }}</span>
            <el-select
                v-model="gcEventTypeSelect"
                clearable
                style="margin-left: 10px"
                @change="filterChange"
            >
              <el-option
                  v-for="option of metadata.parentEventTypes"
                  :key="option"
                  :label="option"
                  :value="option"
              ></el-option>
            </el-select>
          </div>

          <div style="margin-left: 40px" class="filter-item">
            <span>{{ $t('jifa.gclog.gcCause') + ": " }}</span>
            <el-select
                v-model="gcCauseSelect"
                clearable
                style="margin-left: 10px"
                @change="filterChange"
            >
              <el-option
                  v-for="option of metadata.causes"
                  :key="option"
                  :label="option"
                  :value="option"
              ></el-option>
            </el-select>
          </div>

          <div style="margin-left: 40px" class="filter-item">
            <span>{{ $t('jifa.gclog.detail.logTime') + ": " }}</span>

            <el-date-picker
                v-if="metadata.timestamp>=0"
                v-model="timeRange"
                type="datetimerange"
                @change="filterChange"
                :picker-options="pickerOptions"
            >
            </el-date-picker>

            <span class="gc-detail-number-parent" v-if="metadata.timestamp < 0">
              <el-tooltip v-if="metadata.timestamp < 0" effect="dark" :content="this.$t('jifa.gclog.noDatestamp')"
                          placement="top-start">
                <i class="el-icon-warning"></i>
              </el-tooltip>

              <el-input-number :controls="false" v-model="startTimeLow"
                               :min="Math.floor(this.metadata.startTime/1000)"
                               :max="Math.ceil(this.metadata.endTime/1000)" @change="filterChange"/>
              <span style="margin-left: 10px; margin-right: 10px">~</span>
              <el-input-number :controls="false" v-model="startTimeHigh"
                               :min="Math.floor(this.metadata.startTime/1000)"
                               :max="Math.ceil(this.metadata.endTime/1000)" @change="filterChange"/>
            </span>
          </div>

          <div style="margin-left: 40px" class="filter-item gc-detail-number-parent">
            <span>{{ $t('jifa.gclog.detail.pauseTime') + " > " }}</span>
            <el-input-number :controls="false" v-model="pauseTimeLow" :min="0" @change="filterChange"/>
            ms
          </div>
        </div>

        <div v-loading="loading">
          <el-row v-loading="this.loading">
            <el-table
                ref='recordTable'
                :data="tableData"
                stripe
                lazy
                :load="expandEvent"
                size="small"
                row-key="key"
                :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
                :cell-style="cellStyle"
                :show-header="false">
              <el-table-column prop="info">
                <template slot-scope="scope">
                  <GCEventView :metadata="metadata"
                               :gcEvent="scope.row.info"/>
                </template>
              </el-table-column>
            </el-table>
          </el-row>

          <el-row>
            <el-pagination
                layout="prev, pager, next, total, jumper"
                :total="totalSize"
                :current-page="currentPage"
                :page-size="pageSize"
                @current-change="pageChange"
            >
            </el-pagination>
          </el-row>

        </div>
      </el-card>
    </el-col>
  </div>
</template>

<script>
import axios from 'axios'
import {gclogService} from "@/util";
import GCEventView from "@/components/gclog/GCEventView";

  export default {
    components: {GCEventView},
    props: ['file', "metadata", "analysisConfig"],
    data() {
      return {
        loading: false,
        currentPage: 1,
        pageSize: 40,
        totalSize: 0,
        tableData: [],
        cellStyle: {
          padding: 0
        },
        oneDayMilli: 1000 * 60 * 60 * 24,
        pickerOptions: {
          disabledDate: time => {
            if (this.metadata.timestamp < 0) {
              return false;
            }
            const uptime = time.getTime() - this.metadata.timestamp;
            return !(this.metadata.startTime - this.oneDayMilli < uptime && uptime < this.metadata.endTime);
          }
        },

        gcEventTypeSelect: "",
        gcCauseSelect: "",
        logTimeRange: undefined,
        pauseTimeLow: undefined,
        timeRange: undefined,
        startTimeLow: undefined,
        startTimeHigh: undefined
      }
    },
    methods: {
      filterChange() {
        this.currentPage = 1;
        this.fetchData();
      },
      pageChange(nextPage) {
        this.currentPage = nextPage;
        this.fetchData();
      },
      fetchData() {
        if (this.loading) {
          return;
        }
        this.loading = true;
        axios.get(gclogService(this.file, 'gcDetails'), this.getDetailRequestParams()).then(resp => {
          this.tableData = resp.data.data.map(e => this.dealGCEvent(e))
          this.totalSize = resp.data.totalSize;
          this.loading = false;
        });
      },
      dealGCEvent(gcEvent) {
        const result = {
          ...gcEvent,
          key: gcEvent.info.id + 1
        }
        if (gcEvent.phases.length > 0) {
          result.hasChildren = true;
        }
        return result
      },
      expandEvent(row, treeNode, resolve) {
        resolve(row.phases.map(e => this.dealGCEvent(e)))
      },
      getDetailRequestParams() {
        let params = {
          page: this.currentPage,
          pageSize: this.pageSize,
        }
        if (typeof this.gcEventTypeSelect != 'undefined' && this.gcEventTypeSelect !== "") {
          params.eventType = this.gcEventTypeSelect
        }
        if (typeof this.gcCauseSelect != 'undefined' && this.gcCauseSelect !== "") {
          params.gcCause = this.gcCauseSelect
        }
        if (this.metadata.timestamp >= 0) {
          if (this.timeRange !== undefined && this.timeRange !== null && this.timeRange.length === 2) {
            params.logTimeLow = this.timeRange[0].getTime() - this.metadata.timestamp
            params.logTimeHigh = this.timeRange[1].getTime() - this.metadata.timestamp
          }
        } else {
          if (typeof this.startTimeLow != 'undefined') {
            params.logTimeLow = this.startTimeLow * 1000
          }
          if (typeof this.startTimeHigh != 'undefined') {
            params.logTimeHigh = this.startTimeHigh * 1000
          }
        }
        if (typeof this.pauseTimeLow != 'undefined') {
          params.pauseTimeLow = this.pauseTimeLow
        }
        params.config = this.analysisConfig
        return {params: params}
      }
    },
    created() {
      this.fetchData();
    }
  }
</script>

<style scoped>
  .filter{
    overflow: hidden;
    font-size: 14px;
  }

  .filter-item{
    float: left;
    line-height: 40px;
  }
</style>

<style>
  #gc-detail-parent .el-input__inner{
    height: 30px;
  }

  .gc-detail-number-parent .el-input-number{
    width: 100px;
  }

  #gc-detail-parent .el-date-editor{
    padding-top: 0;
    padding-bottom: 0;
  }
</style>
