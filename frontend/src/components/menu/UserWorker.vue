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
    <b-nav-item>
      <el-dialog
          :title="$t('jifa.setUserWorker')"
          :visible.sync="setUserWorkerViewVisible"
          width="40%" :close-on-click-modal=false>

        <el-form ref="setUserWorkerForm" :model="userWorker" :rules="userWorkerRules" label-width="50px"
                 size="medium"
                 label-position="right"
                 style="margin-top: 10px" status-icon validate-on-rule-change inline-message show-message>

          <el-form-item label="IP" prop="hostIP">
            <el-input v-model="userWorker.hostIP" style="width: 50%"
                      clearable></el-input>
          </el-form-item>

          <el-form-item label="Port" prop="port">
            <el-input v-model="userWorker.port" style="width: 50%" clearable/>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="setUserWorkerConfirm">{{$t('jifa.confirm')}}</el-button>
          </el-form-item>
        </el-form>
      </el-dialog>


      <i class="el-icon-s-platform"></i>
      <span slot="title" class="portal-menu-tab" @click="handleSetUserWorker">
        {{$t("jifa.setUserWorker")}}
      </span>
    </b-nav-item>
  </b-navbar-nav>
</template>

<script>
  import axios from 'axios'
  import {service} from '../../util'

  export default {
    data() {
      let hostIPChecker = (rule, value, callback) => {
        if (value) {
          if (/^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/.test(value)) {
            callback();
          } else {
            callback(new Error('Illegal IP format'));
          }
        } else {
          callback()
        }
      }

      let portChecker = (rule, value, callback) => {
        if (value) {
          if (/^([1-9][0-9]*)$/.test(value)) {
            callback();
          } else {
            callback(new Error('Invalid Port format'));
          }
        } else {
          callback(new Error('Invalid Port format'));
        }
      }

      return {
        setUserWorkerViewVisible: false,
        userWorker: {
          hostIP: this.$route.query.userWorker ? this.$route.query.userWorker.hostIP : '',
          port: this.$route.query.userWorker ? this.$route.query.userWorker.port : 9102
        },
        userWorkerRules: {
          hostIP: [{validator: hostIPChecker, trigger: 'blur'}],
          port: [{validator: portChecker, trigger: 'blur'}],
        },
      }
    },
    methods: {
      handleSetUserWorker() {
        this.setUserWorkerViewVisible = true
      },
      setUserWorkerConfirm() {
        this.$refs['setUserWorkerForm'].validate((valid) => {
          if (valid) {
            if (this.userWorker.hostIP) {
              axios.get(service('/userWorker/check'), {
                params: {
                  specifiedWorkerIP: this.userWorker.hostIP,
                  specifiedWorkerPort: this.userWorker.port
                }
              }).then(() => {
                this.setUserWorkerViewVisible = false
                this.$router.push({
                  name: 'portal',
                  query: {
                    specifiedWorkerIP: this.userWorker.hostIP,
                    specifiedWorkerPort: this.userWorker.port
                  }
                })
              })
            } else {
              this.setUserWorkerViewVisible = false
              this.$router.push({
                name: 'portal',
                query: {
                  specifiedWorkerIP: ''
                }
              })
            }
          }
        })
      },
    }
  }
</script>