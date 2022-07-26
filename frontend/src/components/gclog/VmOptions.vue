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
      <span>{{ $t('jifa.gclog.vmOptions.vmOptions') }}</span>
    </div>
    <div v-if="optionsAvailable" class="options-parent">
      <div>
        <span class="vmOption" >{{ $t('jifa.gclog.vmOptions.gcRelatedOptions') + ':'}}</span>
        <span class="vmOption" v-for="option in options.gcRelated" :key="option.text">{{ option.text }}</span>
      </div>
      <el-divider/>
      <div>
        <span class="vmOption" >{{ $t('jifa.gclog.vmOptions.otherOptions') + ':'}}</span>
        <span class="vmOption" v-for="option in options.other" :key="option.text">{{ option.text }}</span>
      </div>
    </div>
    <div v-else>{{ $t('jifa.gclog.vmOptions.unknown') }}</div>

  </el-card>
</template>

<script>
import {gclogService} from '@/util'
import axios from "axios";

export default {
  props: ["file", "metadata"],
  data() {
    return {
      loading: true,
      options: null,
      optionsAvailable: false,
    }
  },
  methods: {
    loadOptions() {
      this.loading = true
      axios.get(gclogService(this.file, 'vmOptions')).then(resp => {
        this.optionsAvailable = !!resp.data
        if (this.optionsAvailable) {
          this.options = resp.data
        }
        this.loading = false;
      })
    },
  },
  mounted() {
    this.loadOptions();
  },
  name: "VmOptions"
}
</script>

<style scoped>
.vmOption {
  display: inline-block;
  font-size: 14px;
  height: 18px;
  margin-right: 8px;
}

.options-parent {
  padding: 5px;
  display: flex;
  flex-wrap: wrap;
}
</style>