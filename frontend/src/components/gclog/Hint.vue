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
  <span>
    <el-tooltip v-if="info"
                effect="dark"
                :content="getContent(info)"
                placement="top-start">
      <i class="el-icon-warning-outline"></i>
    </el-tooltip>
  </span>
</template>

<script>
export default {
  props: ["info"],
  data() {
    return {
    }
  },
  methods: {
    getContent(info) {
      return info.startsWith("jifa.") ? this.$t(info) : info;
    }
  },
  mounted() {},
  name: "Hint"
}
</script>