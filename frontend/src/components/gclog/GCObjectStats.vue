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
    <el-table :data="data"
              :show-header="true"
              v-loading="loading">
      <el-table-column prop="objectCreationSpeed" :label="$t('jifa.gclog.objectCreationSpeed')"/>
      <el-table-column prop="objectPromotionSpeed" :label="$t('jifa.gclog.objectPromotionSpeed')"/>
      <el-table-column prop="objectPromotionAvg" :label="$t('jifa.gclog.objectPromotionAvg')"/>
      <el-table-column prop="objectPromotionMax" :label="$t('jifa.gclog.objectPromotionMax')"/>
    </el-table>
  </el-card>
</template>

<script>
import {toSizeString, toSizeSpeedString, gclogService} from '@/util'
import axios from "axios";

export default {
  props: ["file", "metadata", "timeRange"],
  data() {
    return {
      loading: true,
      data: null,
    }
  },
  methods: {
    loadData() {
      this.loading = true
      const requestConfig = {params: {...this.timeRange}}
      axios.get(gclogService(this.file, 'objectStatistics'), requestConfig).then(resp => {
        this.data = [{
          objectCreationSpeed: toSizeSpeedString(resp.data.objectCreationSpeed),
          objectPromotionSpeed: toSizeSpeedString(resp.data.objectPromotionSpeed),
          objectPromotionAvg: toSizeString(resp.data.objectPromotionAvg),
          objectPromotionMax: toSizeString(resp.data.objectPromotionMax),
        }]
        this.loading = false;
      })
    }
  },
  watch: {
    timeRange() {
      this.loadData();
    }
  },
  mounted() {
    this.loadData();
  },
  name: "GCObjectStats"
}
</script>