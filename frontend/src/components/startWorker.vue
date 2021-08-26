<!--
    Copyright (c) 2021 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<template>
  <el-dialog
      width="30%"
      :visible.sync="startWorkerProgressViewVisible"
      :before-close="closeProgressView"
      :close-on-press-escape=false :close-on-click-modal=false
      append-to-body>
    <b-card class="mt-3" bg-variant="dark" text-variant="white" style="margin-top: 20px">
      <p style="font-size: 14px; white-space: pre-line;">
      <p>{{startWorkerText}}</p>
    </b-card>
  </el-dialog>
</template>

<script>
  import axios from 'axios'
  import {service} from "../util";

  export default {
    data() {
      return {
        pollingInternal: 1000,
        startWorkerProgressViewVisible: false,
        startWorkerErrorMessage: '',
        startWorkerText: '',
        cnt: 0,
      }
    },
    methods: {
      startWorker(formData, onSuccess) {
        axios.post(service('/worker/startWorker'), new URLSearchParams(formData)).then(resp => {
          if (resp.data !== undefined && resp.data != null) {
            let {workerName, originName, fileName} = resp.data;
            formData.append("originName", originName);
            formData.append("fileName", fileName);
            this.startWorkerProgressViewVisible = true;
            this.startWorkerText = this.$t('jifa.startingStatus.request')
            this.checkIfWorkerStarted(workerName, onSuccess)
          }
        }).catch(e => {
          this.startWorkerText = this.$t('jifa.startingStatus.requestFailed')
        })
      },

      checkIfWorkerStarted(workerName, onSuccess) {
        axios.get(service('/worker/startWorkerDone'), {
          params: {
            workerName: workerName
          }
        }).then(resp => {
          console.log(resp)
          if (resp === undefined || resp.data === undefined) {
            return;
          }
          let formData = new FormData();
          formData.append("workerName", workerName);

          if (resp.data === "STARTING") {
            this.startWorkerText = this.$t('jifa.startingStatus.starting') + '.'.repeat(++this.cnt % 5)
            setTimeout(() => {
              this.checkIfWorkerStarted(workerName, onSuccess)
            }, this.pollingInternal)
          } else if (resp.data === "TIMEOUT") {
            this.startWorkerText = this.$t('jifa.startingStatus.timeout')
            axios.post(service("/worker/stopWorker"), new URLSearchParams(formData));
          } else if (resp.data === "ERROR") {
            this.startWorkerText = this.$t('jifa.startingStatus.error')
            axios.post(service("/worker/stopWorker"), new URLSearchParams(formData));
          } else if (resp.data === "SUCCESS") {
            this.startWorkerText = this.$t('jifa.startingStatus.success')
            this.startWorkerProgressViewVisible = false;
            onSuccess(workerName)
          } else {
            throw "should not reach here";
          }
        }).catch(err => {
          this.startWorkerText = this.$t('jifa.startingStatus.error')
          let formData = new FormData();
          formData.append("workerName", workerName);
          axios.post(service("/worker/stopWorker"), new URLSearchParams(formData));
        })
      },

      closeProgressView(done) {
        this.$confirm(this.$t('jifa.close') + this.$t('jifa.qm'), '', {
          confirmButtonText: this.$t('jifa.confirm'),
          cancelButtonText: this.$t('jifa.cancel'),
        }).then(() => {
          done();
        })
      },
    }
  }
</script>