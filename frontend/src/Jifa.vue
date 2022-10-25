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
  <div style="height: 100vh; display: flex; flex-direction: column; overflow: hidden">
    <div style="flex-grow: 1; overflow: scroll">
      <router-view/>
    </div>
    <div style="flex-shrink: 0; height: 30px; line-height: 30px; text-align: center;">
      <a href="https://github.com/eclipse/jifa" target="_blank"
         style="color: rgba(0, 0, 0, 0.6); font-size: 14px; font-weight: bold">Eclipse Jifa v0.1.0</a>
    </div>
  </div>
</template>

<script>
  export default {}
</script>

<style>
  html, body {
    height: 100%;
  }
</style>