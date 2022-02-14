<!--
    Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<template>
  <b-navbar-nav>
    <b-nav-item @click="$emit('chooseMenu', 'HEAP_DUMP')"
                v-if="$jifa.fileManagement"
                :active="fileType==='HEAP_DUMP'">
      <i class="el-icon-coin" style="margin-right: 3px"/> {{$t("jifa.heapDumpAnalysis")}}
    </b-nav-item>

    <b-nav-item @click="$emit('chooseMenu', 'GC_LOG')"
                v-if="$jifa.fileManagement"
                :active="fileType==='GC_LOG'">
      <i class="el-icon-toilet-paper" style="margin-right: 3px"/> {{$t("jifa.gclogAnalysis")}}
    </b-nav-item>

    <b-nav-item @click="$emit('chooseMenu', 'THREAD_DUMP')"
                v-if="$jifa.fileManagement"
                :active="fileType==='THREAD_DUMP'">
      <i class="el-icon-s-operation" style="margin-right: 3px"/> {{$t("jifa.threadDump.title")}}
    </b-nav-item>

    <b-nav-item @click="handleAddFile" v-if="$jifa.fileManagement">
      <i class="el-icon-plus" style="margin-right: 3px"/> {{this.title}}
    </b-nav-item>
  </b-navbar-nav>
</template>

<script>

  export default {
    props: ['defaultActive', 'title', 'fileType'],
    methods: {
      handleAddFile() {
        this.$emit('addFile');
      }
    },
    data() {
      return {}
    },
  }
</script>
<style scoped>
  .navbar-light .navbar-nav .nav-link.active {
    color: #1989FA;
  }
</style>
