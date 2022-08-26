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
        :clearable="false"
        :picker-options="pickerOptions"
    >
    </el-date-picker>

    <span v-if="useUptime()">
      <el-input-number :controls="false" v-model="low" style="width: 100px"
                       :min="this.min" :max="Math.min(this.max, this.high)"
                       @change="emitValue"/>
      <span style="margin-left: 10px; margin-right: 10px">~</span>
      <el-input-number :controls="false" v-model="high" style="width: 100px"
                       :min="Math.max(this.min, this.low)" :max="this.max"
                       @change="emitValue"/>
      <span style="margin-left: 10px;margin-right: 10px">s</span>
      <Hint :info="getNoDateStampHint()"/>
    </span>
  </div>
</template>

<script>
import Hint from "@/components/gclog/Hint";
import {uppercaseFirstLetter} from "@/components/gclog/GCLogUtil";
export default {
  components: {Hint},
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
          return !(this.metadata.startTime - this.oneDayMilli < uptime && uptime < this.metadata.endTime + this.oneDayMilli);
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
        value = {
          start: typeof this.low === "undefined" ? this.metadata.startTime : this.low * 1000,
          end: typeof this.high === "undefined" ? this.metadata.endTime : this.high * 1000
        }
      } else {
        if (this.timeRange == null) {
          value = {
            start: this.metadata.startTime,
            end: this.metadata.endTime,
          }
        } else {
          value = {
            start: this.timeRange[0] === null ? this.startTime : this.timeRange[0].getTime() - this.metadata.timestamp,
            end: this.timeRange[1] === null ? this.endTime : this.timeRange[1].getTime() - this.metadata.timestamp,
          }
        }
      }
      value = {
        start: Math.max(this.metadata.startTime, value.start),
        end: Math.min(this.metadata.endTime, value.end),
      }
      this.$emit('input', value)
    },
    setComponentData() {
      if (this.useUptime()) {
        this.low = Math.floor(this.value.start / 1000);
        this.high = Math.ceil(this.value.end / 1000);
      } else {
        this.timeRange = [new Date(this.value.start + this.metadata.timestamp),
          new Date(this.value.end + this.metadata.timestamp)]
      }
    },
    getNoDateStampHint() {
      return 'jifa.gclog.noDateStamp' + uppercaseFirstLetter(this.metadata.logStyle)
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