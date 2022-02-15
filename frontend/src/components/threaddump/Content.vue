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
  <b-card class="mt-3" bg-variant="dark" text-variant="white">
    <b-card-text style="white-space: pre;">{{ content }}</b-card-text>
    <div class="d-flex justify-content-center mb-3" v-if="!end">
      <b-button pill variant="primary" size="sm" squared="false" @click="loadContent">
        {{ content.length === 0 ? $t('jifa.threadDump.loadFileContent') : $t('jifa.threadDump.loadMoreFileContent') }}
      </b-button>
    </div>
  </b-card>
</template>

<script>

import axios from "axios";
import {threadDumpService} from "@/util";

export default {
  props: ['file'],
  data() {
    return {
      content: '',
      lineNo: 1,
      lineLimit: 512,
      end: false,
    }
  },
  methods: {
    loadContent() {
      if (this.end) {
        return
      }
      axios.get(threadDumpService(this.file, 'content'), {
        params: {
          lineNo: this.lineNo,
          lineLimit: this.lineLimit
        }
      }).then(resp => {
        let data = resp.data.content
        if (data && data.length > 0) {
          if (this.content.length > 0) {
            this.content += "\n"
          }
          for (let i = 0; i < data.length; i++) {
            this.content += data[i]
            if (i !== data.length - 1) {
              this.content += "\n"
            }
          }
          this.lineNo += data.length
        }
        this.end = resp.data.end
      })
    }
  },
}
</script>