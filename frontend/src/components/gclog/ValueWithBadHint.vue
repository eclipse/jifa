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
    <span :class="bad ? 'bad-metric' : ''"> {{ value }}</span>
    <Hint :info="getContent()"/>
  </span>
</template>

<script>
import Hint from "@/components/gclog/Hint";

export default {
  props: {
    value: {
      default: '',
    },
    bad: {
      type: Boolean,
      default: false,
    },
    badHint: { // text shown when bad
      type: String,
      default: '',
    },
    hint: { // text shown whatever bad or not
      type: String,
      default: '',
    }
  },
  data() {
    return {}
  },
  components: {
    Hint
  },
  methods: {
    getContent() {
      if (this.bad) {
        return [this.hint, this.badHint].flat()
      } else {
        return this.hint
      }
    },
  },
  mounted() {
  },
  name: "ValueWithBadHint"
}
</script>

<style scoped>
.bad-metric {
  color: #E74C3C;
}
</style>