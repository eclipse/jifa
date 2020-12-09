<!--
    Copyright (c) 2020 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<template>
  <b-navbar-nav>
    <b-nav-item >
      <i class="el-icon-edit"></i>
      <span slot="title" @click="feedback">
        {{$t("jifa.feedback")}}
      </span>
    </b-nav-item>
  </b-navbar-nav>
</template>

<script>
  export default {
    methods: {
      feedback() {
        window.open("https://github.com/eclipse/jifa/issues", "_blank")
      }
    }
  }
</script>