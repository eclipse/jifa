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
    <el-tooltip v-if="show()"
                effect="dark"
                placement="top-start">
      <div slot="content" style="max-width: 600px">
        <div v-for="line of getContent()" :key="line">
          {{ line }}
        </div>
      </div>
      <i class="el-icon-warning"></i>
    </el-tooltip>
  </span>
</template>

<script>
export default {
  props: ["info"],
  data() {
    return {}
  },
  methods: {
    getContent() {
      return (typeof this.info === "string" ? [this.info] : this.info)
          .filter(s => s)
          .map(s => s.startsWith("jifa.") ? this.$t(s) : s);
    },
    show() {
      if (this.info === undefined) {
        return false;
      } else if (typeof this.info === "string") {
        return this.info !== ''
      } else if (Array.isArray(this.info)) {
        return this.info.filter(s => s).length > 0
      } else {
        return false;
      }
    }
  },
  mounted() {
  },
  name: "Hint"
}
</script>