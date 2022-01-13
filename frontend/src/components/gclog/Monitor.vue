<template>
  <div>
    <el-col :span="22" :offset="1" v-loading="loadingMetadata">
      <el-card>
        <el-row>
          <span style="margin-right: 30px" class="label" @change="optionChange">{{$t('jifa.gclog.monitors.timeSpan')}}:</span>
          <el-radio-group v-model="timeSpan">
            <el-radio-button label="300000">5{{$t('jifa.gclog.minute')}}</el-radio-button>
            <el-radio-button :disabled="endTime - startTime < 30000" label="3600000">1{{$t('jifa.gclog.hour')}}</el-radio-button>
            <el-radio-button :disabled="endTime - startTime < 3600000" label="10800000">3{{$t('jifa.gclog.hour')}}</el-radio-button>
            <el-radio-button :disabled="endTime - startTime < 10800000" label="43200000">12{{$t('jifa.gclog.hour')}}</el-radio-button>
            <el-radio-button :disabled="endTime - startTime < 43200000" label="259200000">3{{$t('jifa.gclog.day')}}</el-radio-button>
          </el-radio-group>
        </el-row>
        <el-row>
          <span style="margin-right: 30px" class="label">
            {{$t('jifa.gclog.monitors.timePoint')}}:
            <el-tooltip effect="dark"
                        :content="this.$t('jifa.gclog.noRealTime')"
                        v-if="referenceTimestamp < 0"
                        placement="top-start">
              <i class="el-icon-warning"></i>
            </el-tooltip>
          </span>
          <el-date-picker
              v-if="referenceTimestamp>=0"
              @change="optionChange"
              v-model="timePoint"
              type="datetime"
              :picker-options="pickerOptions"
          >
          </el-date-picker>

          <span v-if="referenceTimestamp < 0">
            <el-input-number :controls="false" v-model="timePoint"
                             @change="optionChange"
                             :clearable="false"
                             :min="Math.floor(this.startTime/1000)"
                             :max="Math.ceil(this.endTime/1000)"/>
          </span>
        </el-row>
      </el-card>
      <el-card>
        <el-row>
          <el-col :span="10" :offset="1">
            <TimeLineChart
                v-if="!loadingMetadata"
                :file="file"
                :time-span="timeSpan"
                :time-point="chartTimePoint"
                :reference-timestamp="referenceTimestamp"
                :type="'count'"
                :title="$t('jifa.gclog.monitors.gcCount')"
            />
          </el-col>
          <el-col :span="10" :offset="2">
            <TimeLineChart
                v-if="!loadingMetadata"
                :file="file"
                :time-span="timeSpan"
                :time-point="chartTimePoint"
                :reference-timestamp="referenceTimestamp"
                :type="'pause'"
                :title="$t('jifa.gclog.monitors.gcPause')"
            />
          </el-col>
        </el-row>
        <el-divider></el-divider>
        <el-row>
          <el-col :span="10" :offset="1">
            <TimeLineChart
                v-if="!loadingMetadata"
                :file="file"
                :time-span="timeSpan"
                :time-point="chartTimePoint"
                :reference-timestamp="referenceTimestamp"
                :type="'heap'"
                :title="$t('jifa.gclog.monitors.heap')"
            />
          </el-col>
          <el-col :span="10" :offset="2">
            <TimeLineChart
                v-if="!loadingMetadata"
                :file="file"
                :time-span="timeSpan"
                :time-point="chartTimePoint"
                :reference-timestamp="referenceTimestamp"
                :type="'metaspace'"
                :title="$t('jifa.gclog.monitors.metaspace')"
            />
          </el-col>
        </el-row>
        <el-divider></el-divider>
        <el-row>
          <el-col :span="10" :offset="1">
            <TimeLineChart
                v-if="!loadingMetadata"
                :file="file"
                :time-span="timeSpan"
                :time-point="chartTimePoint"
                :reference-timestamp="referenceTimestamp"
                :type="'alloRec'"
                :title="$t('jifa.gclog.monitors.alloRec')"
            />
          </el-col>
          <el-col :span="10" :offset="2">
            <TimeLineChart
                v-if="!loadingMetadata && collector !== 'ZGC'"
                :file="file"
                :time-span="timeSpan"
                :time-point="chartTimePoint"
                :reference-timestamp="referenceTimestamp"
                :type="'promotion'"
                :title="$t('jifa.gclog.monitors.promotion')"
            />
          </el-col>
          <el-col :span="10" :offset="2">
            <TimeLineChart
                v-if="!loadingMetadata && collector === 'ZGC'"
                :file="file"
                :time-span="timeSpan"
                :time-point="chartTimePoint"
                :reference-timestamp="referenceTimestamp"
                :type="'gccycle'"
                :title="$t('jifa.gclog.monitors.gccycle')"
            />
          </el-col>
        </el-row>
      </el-card>
    </el-col>
  </div>
</template>

<script>
  import TimeLineChart from "./TimeLineChart";
  import {gclogService} from "@/util"
  import axios from "axios";

  export default {
    /* eslint-disable */
    props: ['file'],
    components: {TimeLineChart},
    data() {
      return {
        timeSpan: 300000,
        timePoint: 0,
        referenceTimestamp: 0,
        startTime: 0,
        endTime: 0,
        chartTimePoint: 0,
        loadingMetadata: false,
        collector: "",
        oneDayMilli: 1000 * 60 * 60 * 24,
        pickerOptions: {
          disabledDate: time => {
            if (this.referenceTimestamp < 0) {
              return false;
            }
            const uptime = time.getTime() - this.referenceTimestamp;
            return !(this.startTime - this.oneDayMilli < uptime && uptime < this.endTime);
          }
        },
      }
    },
    methods: {
      fetchMetadata() {
        this.loadingMetadata = true
        axios.get(gclogService(this.file, 'metadata')).then(resp => {
          this.startTime = resp.data.startTime
          this.endTime = resp.data.endTime
          this.referenceTimestamp = resp.data.timestamp
          this.collector = resp.data.collector
          this.timeSpan = this.getOptimalDefaultTimeSpan()
          this.timePoint = 0
          if (this.referenceTimestamp >= 0) {
            this.timePoint += this.referenceTimestamp;
          } else {
            this.timePoint = Math.floor(this.timePoint / 1000)
          }
          this.optionChange()
          this.loadingMetadata = false
        })
      },
      getOptimalDefaultTimeSpan() {
        const span = this.endTime - this.startTime
        if (span >= 10800000) {
          return 10800000;
        } else if (span >= 3600000) {
          return 3600000
        }
        return 300000
      },
      optionChange() {
        if (this.referenceTimestamp >= 0) {
          this.chartTimePoint = this.timePoint - this.referenceTimestamp;
        } else {
          this.chartTimePoint = this.timePoint * 1000;
        }
      }
    },
    created() {
      this.fetchMetadata()
    },
  }
</script>

<style scoped>
  .label {
    width: 80px;
    display: inline-block;
  }
</style>
