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
    <div v-if="!options">{{ $t('jifa.gclog.vmOptions.unknown') }}</div>
    <div v-else style="padding: 10px">
      <span class="vmOption" v-for="option in options">{{ option }}</span>
    </div>
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
      options: null
    }
  },
  methods: {
    loadOptions() {
      this.loading = true
      axios.get(gclogService(this.file, 'vmOptions')).then(resp => {
        this.options = resp.data.split(/ +/)
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
  padding: 5px;
  font-weight: bold;
  font-size: 10px;
  border-radius: 1px;
  display: inline;
  margin-right: 10px;
  word-wrap: break-word;
  word-break: break-all;
}
</style>