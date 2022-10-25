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
  <el-card>
    <div slot="header">
      <span>{{ $t('jifa.gclog.objectStats') }}</span>
    </div>
    <div>
      <el-table :data="tableData"
                :loading="loading"
                :show-header="true"
      >
        <el-table-column v-for="column in columns" :key="column">
          <template slot="header">
            <span>{{ $t('jifa.gclog.' + column) }}</span>
          </template>
          <template slot-scope="scope">
            <ValueWithBadHint :value="scope.row[column].value"
                              :bad="scope.row[column].bad"
                              :badHint="scope.row[column].badHint"/>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-card>
</template>

<script>
import {gclogService} from '@/util'
import axios from "axios";
import {formatSize, formatSizeSpeed} from "@/components/gclog/GCLogUtil";
import ValueWithBadHint from "@/components/gclog/ValueWithBadHint";

export default {
  components: {ValueWithBadHint},
  props: ["file", "metadata", "analysisConfig", "sharedInfo"],
  data() {
    return {
      loading: true,
      originalData: null,
      tableData: [],
      columns: ['objectCreationSpeed', 'objectPromotionSpeed', 'objectPromotionAvg', 'objectPromotionMax']
    }
  },
  methods: {
    loadData() {
      this.loading = true
      const requestConfig = {params: {...this.analysisConfig.timeRange}}
      axios.get(gclogService(this.file, 'objectStatistics'), requestConfig).then(resp => {
        this.originalData = resp.data
        this.calTableData()
        this.loading = false;
      })
    },
    calTableData() {
      if (this.originalData === null) {
        return
      }
      const youngCapacity = this.sharedInfo.youngCapacityAvg;
      const oldCapacity = this.sharedInfo.oldCapacityAvg;
      const heapCapacity = this.sharedInfo.heapCapacityAvg;
      const highPromotionThresholdPercent = this.analysisConfig.highPromotionThreshold / 100;
      this.tableData = [
        {
          objectCreationSpeed: {
            value: formatSizeSpeed(this.originalData.objectCreationSpeed),
            bad: this.metadata.generational ?
                (youngCapacity >= 0 && youngCapacity / this.originalData.objectCreationSpeed < this.analysisConfig.youngGCFrequentIntervalThreshold) :
                (heapCapacity >= 0 && heapCapacity / this.originalData.objectCreationSpeed < this.analysisConfig.fullGCFrequentIntervalThreshold),
            badHint: this.$t('jifa.gclog.badHint.badObjectAllocSpeed')
          },
          objectPromotionSpeed: {
            value: formatSizeSpeed(this.originalData.objectPromotionSpeed),
            bad: oldCapacity > 0 && this.originalData.objectPromotionSpeed > oldCapacity * highPromotionThresholdPercent / this.analysisConfig.youngGCFrequentIntervalThreshold,
            badHint: this.$t('jifa.gclog.badHint.badPromotionSpeed')
          },
          objectPromotionAvg: {
            value: formatSize(this.originalData.objectPromotionAvg),
            bad: oldCapacity > 0 && this.originalData.objectPromotionAvg > oldCapacity * highPromotionThresholdPercent,
            badHint: this.$t('jifa.gclog.badHint.badPromotionSpeed')
          },
          objectPromotionMax: {
            value: formatSize(this.originalData.objectPromotionMax),
            bad: oldCapacity > 0 && this.originalData.objectPromotionMax > oldCapacity * highPromotionThresholdPercent * 2,
            badHint: this.$t('jifa.gclog.badHint.badSinglePromotion')
          },
        }]
    },
  },
  watch: {
    analysisConfig() {
      this.loadData();
    },
    sharedInfo() {
      this.calTableData()
    }
  },
  mounted() {
    this.loadData();
  },
  name: "GCObjectStats"
}
</script>