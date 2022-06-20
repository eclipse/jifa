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
    <el-date-picker
        v-if="!useUptime()"
        v-model="timeRange"
        type="datetimerange"
        @change="emitValue"
        :picker-options="pickerOptions"
    >
    </el-date-picker>

    <span v-if="useUptime()">
      <el-tooltip effect="dark" :content="this.$t('jifa.gclog.noDatestamp')"
                  placement="top-start">
        <i class="el-icon-warning"></i>
      </el-tooltip>

      <el-input-number :controls="false" v-model="low" style="width: 200px"
                       :min="this.min" :max="Math.min(this.max, this.high)"
                       @change="emitValue"/>
      <span style="margin-left: 10px; margin-right: 10px">~</span>
      <el-input-number :controls="false" v-model="high" style="width: 200px"
                       :min="Math.max(this.min, this.low)" :max="this.max"
                       @change="emitValue"/>
      <span style="margin-left: 10px">s</span>
    </span>
  </div>
</template>

<script>
export default {
  props: ["metadata", "value"],
  data() {
    return {
      timeRange: undefined, // for datetime
      low: 0, // for uptime
      high: 0, // for uptime
      oneDayMilli: 1000 * 60 * 60 * 24,
      min: 0, // for rangeCheck
      max: 0, // for rangeCheck
      pickerOptions: {
        disabledDate: time => {
          if (this.useUptime() < 0) {
            return false;
          }
          const uptime = time.getTime() - this.metadata.timestamp;
          return !(this.metadata.startTime - this.oneDayMilli < uptime && uptime < this.metadata.endTime);
        }
      },
    }
  },
  methods: {
    useUptime() {
      return this.metadata.timestamp < 0;
    },
    emitValue() {
      let value;
      if (this.useUptime()) {
        value = [
          typeof this.low === "undefined" ? this.min : this.low * 1000,
          typeof this.high === "undefined" ? this.max : this.high * 1000
        ]
      } else {
        value = [
          this.timeRange[0] === null ? this.min : this.timeRange[0].getTime() - this.metadata.timestamp,
          this.timeRange[1] === null ? this.min : this.timeRange[1].getTime() - this.metadata.timestamp,
        ]
      }
      this.$emit('input', value)
    },
    setComponentData() {
      if (this.useUptime()) {
        this.low = Math.floor(this.value[0] / 1000);
        this.high = Math.ceil(this.value[1] / 1000);
      } else {
        this.timeRange = [new Date(this.value[0] + this.metadata.timestamp),
          new Date(this.value[1] + this.metadata.timestamp)]
      }
    }
  },
  watch: {
    value() {
      this.setComponentData()
    }
  },
  mounted() {
    this.min = Math.floor(this.metadata.startTime / 1000);
    this.max = Math.ceil(this.metadata.endTime / 1000);
    this.setComponentData()
  },
  name: "GCLogTimePicker"
}
</script>