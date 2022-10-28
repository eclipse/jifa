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
    <el-card :body-style="{padding:'10px'}">
      <div slot="header">
        <span>{{ $t('jifa.gclog.basicInfo') }}</span>
      </div>
      <div>
        <div class="row">
          <i class="el-icon-document icon"/>
          <span class="content"
                style="overflow: hidden; display:block; text-overflow: ellipsis; max-width: 900px; white-space: nowrap">
            {{file }}
          </span>
        </div>
        <div class="row">
          <i class="el-icon-brush icon"/>
          <span class="content">{{ this.metadata.collector }}</span>
        </div>
        <div class="row">
          <i class="el-icon-time icon"/>
          <span class="content" style="margin-right: 10px">{{ $t('jifa.gclog.logTimeRange') }}</span>
          <span class="content" style="margin-right: 20px">{{ this.logTimeRange }}</span>
          <span class="content" style="margin-right: 10px">{{ $t('jifa.gclog.duration') }}</span>
          <span class="content">{{ this.logDuration }}</span>
        </div>
        <div class="row">
          <i class="el-icon-timer icon"/>
          <span class="content" style="margin-right: 10px">{{ $t('jifa.gclog.analysisTimeRange') }}</span>
          <a href="javascript:void(0);" @click="() => $emit('showAnalysisConfig')">
            <span class="content">{{ this.configTimeRange }}</span>
          </a>
          <span class="content" style="margin-right: 20px; font-size: 10px">{{ '(' + $t('jifa.gclog.clickToChooseTime') + ')' }}</span>
          <span class="content" style="margin-right: 10px">{{ $t('jifa.gclog.duration') }}</span>
          <span class="content">{{ this.configDuration }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import Hint from "@/components/gclog/Hint";
import {formatTimePeriod, formatTimeRange} from "@/components/gclog/GCLogUtil";

export default {
  props: ["file", "metadata", "analysisConfig"],
  data() {
    return {
      logTimeRange: "",
      logDuration: "",
      configTimeRange: "",
      configDuration: ""
    }
  },
  components: {
    Hint
  },
  methods: {},
  watch: {
    analysisConfig(newValue) {
      this.configTimeRange = formatTimeRange(newValue.timeRange.start,
          newValue.timeRange.end, this.metadata.timestamp)
      this.configDuration = formatTimePeriod(newValue.timeRange.end - newValue.timeRange.start)
    }
  },
  mounted() {
    this.logTimeRange = formatTimeRange(this.metadata.startTime, this.metadata.endTime, this.metadata.timestamp)
    this.logDuration = formatTimePeriod(this.metadata.endTime - this.metadata.startTime)
    this.configTimeRange = formatTimeRange(this.analysisConfig.timeRange.start,
        this.analysisConfig.timeRange.end, this.metadata.timestamp)
    this.configDuration = formatTimePeriod(this.analysisConfig.timeRange.end - this.analysisConfig.timeRange.start)
  },
  name: "GCOverview"
}
</script>

<style scoped>
.icon {
  margin-left: 25px;
  margin-right: 10px;
  margin-top: 4px;
  height: 20px;
}

.row {
  margin-bottom: 10px;
}

.content {
  height: 24px;
  line-height: 24px;
}
</style>