<!--
    Copyright (c) 2020 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<template xmlns:v-clipboard="http://www.w3.org/1999/xhtml">
  <div style="margin-top: -20px">
    <el-dialog
            width="30%"
            title="Public Key"
            :visible.sync="publicKeyViewVisible"
            append-to-body>
      <div style="margin-top: -20px">
        <el-button size="small" round v-clipboard:copy="publicKey" v-clipboard:success="publicKeyCopySuccessfully">
          {{$t('jifa.copy')}}
        </el-button>
        <el-input
                type="textarea"
                autosize
                readonly
                resize
                v-model="publicKey" style="margin-top: 20px">
        </el-input>
      </div>
    </el-dialog>

    <el-dialog
            width="30%"
            :visible.sync="transferProgressViewVisible"
            :before-close="closeProgressView"
            :close-on-press-escape=false :close-on-click-modal=false
            append-to-body>
      <el-row>
        <el-col :span="24" align="center">
          <el-progress type="circle" :percentage=transferProgress :status="transferState"
                       v-if="totalSize > 0"></el-progress>
          <b-spinner variant="secondary" type="grow" v-if="totalSize < 0"></b-spinner>
        </el-col>
      </el-row>
      <div style="margin-top: 8px;">
        <p align="center" style="font-size: 14px"><strong>{{currentProgress}}</strong></p>
      </div>

      <b-card class="mt-3" bg-variant="dark" text-variant="white" v-if="transferErrorMessage" style="margin-top: 20px">
        <p style="font-size: 14px; white-space: pre-line;">
          {{transferErrorMessage}}
        </p>
      </b-card>
    </el-dialog>

    <el-tabs value="upload" style="margin-top: 10px">
      <el-tab-pane label="Upload" name="upload">
        <div align="center">
          <el-upload ref="uploadComp"
                     drag
                     :headers="authHeader"
                     :limit=1
                     :data="{uploadName:'upload', type: fileType}"
                     :action="service('/file/upload')"
                     :multiple=true
                     :on-success="onSuccess">
            <i class="el-icon-upload"></i>
            <div class="el-upload__text">{{ $t('jifa.uploadPrompt') }}</div>
          </el-upload>
        </div>
      </el-tab-pane>

      <el-tab-pane label="S C P" name="scp">

        <el-form ref="scpForm" :model="scp" :rules="scpRules" label-width="150px" size="medium" label-position="right"
                 style="margin-top: 10px" :show-message=false status-icon>

          <el-form-item label="">
            <el-radio-group v-model="scp.authType" @change="changeAuthType">
              <el-radio label="publicKey">Public Key</el-radio>
              <el-radio label="password">Password</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="Hostname" prop="hostname">
            <el-input v-model="scp.hostname" placeholder="Hostname" style="width: 60%" clearable></el-input>
          </el-form-item>

          <el-form-item label="Path" prop="path">
            <el-input v-model="scp.path" placeholder="Path" style="width: 60%" clearable></el-input>
          </el-form-item>

          <el-form-item label="User" prop="user">
            <el-input v-model="scp.user" placeholder="User" style="width: 60%" clearable></el-input>
          </el-form-item>

          <el-form-item label="Password" v-if="scp.authType === 'password'" prop="password">
            <el-input v-model="scp.password" placeholder="Password" style="width: 60%" clearable
                      show-password></el-input>
          </el-form-item>

          <el-form-item label="Public Key" v-if="scp.authType === 'publicKey'">
            <el-button icon="el-icon-view" round @click="loadPublicKey" style="outline:none;"></el-button>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="scpConfirm" :disabled="inTransferring">{{$t('jifa.confirm')}}</el-button>
          </el-form-item>

        </el-form>
      </el-tab-pane>

      <el-tab-pane label="U R L" name="url">
        <el-form ref="urlForm" :model="url" :rules="urlRules" label-width="150px" size="medium" label-position="right"
                 style="margin-top: 10px" :show-message=false status-icon>
          <el-form-item label="U R L" prop="url" :show-message=false>
            <el-input v-model="url.url" placeholder="U R L" style="width: 80%" clearable></el-input>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="urlConfirm" :disabled="inTransferring">{{$t('jifa.confirm')}}</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="File System" name="fileSystem" v-if="false">
        <el-form ref="fileSystemForm" :model="fileSystem" :rules="fileSystemRules" label-width="150px" size="medium"
                 label-position="right"
                 style="margin-top: 10px" :show-message=false status-icon>
          <el-form-item label="">
            <el-radio-group v-model="fileSystem.moveOrCopy">
              <el-radio label="move">Move</el-radio>
              <el-radio label="copy">Copy</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="Path" prop="path" :show-message=false>
            <el-input v-model="fileSystem.path" placeholder="path" style="width: 80%" clearable></el-input>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="fileSystemConfirm" :disabled="inTransferring">{{$t('jifa.confirm')}}</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="O S S" name="oss">
        <el-form ref="ossForm" :model="oss" :rules="ossRules" label-width="150px" size="medium" label-position="right"
                 style="margin-top: 10px" status-icon :show-message=false>
          <el-form-item label="Endpoint" prop="endpoint">
            <el-input v-model="oss.endpoint" placeholder="Endpoint" style="width: 80%" clearable></el-input>
          </el-form-item>

          <el-form-item label="Access Key Id" prop="accessKeyId">
            <el-input v-model="oss.accessKeyId" placeholder="Access Key Id" style="width: 80%" clearable
                      show-password=""></el-input>
          </el-form-item>

          <el-form-item label="Access Key Secret" prop="accessKeySecret">
            <el-input v-model="oss.accessKeySecret" placeholder="Access Key Secret" style="width: 80%" clearable
                      show-password=""></el-input>
          </el-form-item>

          <el-form-item label="Bucket Name" prop="bucketName">
            <el-input v-model="oss.bucketName" placeholder="Bucket Name" style="width: 80%" clearable></el-input>
          </el-form-item>

          <el-form-item label="Object Name" prop="objectName">
            <el-input v-model="oss.objectName" placeholder="Object Name" style="width: 80%" clearable></el-input>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="ossConfirm" :disabled="inTransferring">{{$t('jifa.confirm')}}</el-button>
          </el-form-item>
        </el-form>
       </el-tab-pane>

      <el-tab-pane label="S3" name="s3">
        <el-form ref="s3Form" :model="s3" :rules="s3Rules" label-width="150px" size="medium" label-position="right"
                 style="margin-top: 10px" status-icon :show-message=false>
          <el-form-item label="Region" prop="endpoint">
            <el-input v-model="s3.endpoint" placeholder="Region name in lowercase" style="width: 80%" clearable></el-input>
          </el-form-item>

          <el-form-item label="Access Key ID" prop="accessKey">
            <el-input v-model="s3.accessKey" placeholder="Access Key ID" style="width: 80%" clearable
                      show-password=""></el-input>
          </el-form-item>

          <el-form-item label="Access Key Secret" prop="accessKeySecret">
            <el-input v-model="s3.secretKey" placeholder="Access Key Secret" style="width: 80%" clearable
                      show-password=""></el-input>
          </el-form-item>

          <el-form-item label="Bucket Name" prop="bucketName">
            <el-input v-model="s3.bucketName" placeholder="Bucket Name" style="width: 80%" clearable></el-input>
          </el-form-item>

          <el-form-item label="Object Name" prop="objectName">
            <el-input v-model="s3.objectName" placeholder="Object Name" style="width: 80%" clearable></el-input>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="s3Confirm" :disabled="inTransferring">{{$t('jifa.confirm')}}</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="Raw Text" name="rawText" v-if="rawTextUploadSupported()">
        <el-form ref="rawTextForm" :model="rawText" :rules="rawTextRules" label-width="150px" size="medium" label-position="right"
                 style="margin-top: 10px" status-icon :show-message=false>
          <el-form-item label="File Name" prop="originalName">
            <el-input v-model="rawText.originalName" placeholder="File Name" style="width: 80%" clearable></el-input>
          </el-form-item>

          <el-form-item label="Content" prop="content">
            <el-input v-model="rawText.content" placeholder="Content" style="width: 80% " clearable
                      type="textarea" resize="none" rows="10"
            ></el-input>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="rawTextConfirm" :loading="inTransferring">{{$t('jifa.confirm')}}</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

    </el-tabs>


  </div>
</template>

<script>
  import axios from 'axios'
  import {service, toReadableSizeWithUnit} from '../util'

  export default {
    props: ['fileType', 'transferViewVisible'],
    data() {
      return {
        authHeader:{ Authorization: this.$jifa.get_authorization_header()},
        url: {
          url: '',
        },
        urlRules: {
          url: [
            {required: true, trigger: 'blur'}
          ],
        },
        fileSystem: {
          path: '',
          moveOrCopy: 'move',
        },
        fileSystemRules: {
          path: [
            {required: true, trigger: 'blur'}
          ],
        },
        scp: {
          user: '',
          authType: 'publicKey',
          password: '',
          hostname: '',
          path: '',
        },
        scpRules: {
          user: [
            {required: true, trigger: 'blur'}
          ],
          password: [
            {required: true, trigger: 'blur'}
          ],
          hostname: [
            {required: true, trigger: 'blur'}
          ],
          path: [
            {required: true, trigger: 'blur'}
          ],
        },
        oss: {
          endpoint: '',
          accessKeyId: '',
          accessKeySecret: '',
          bucketName: '',
          objectName: ''
        },
        ossRules: {
          endpoint: [
            {required: true, trigger: 'blur'}
          ],
          accessKeyId: [
            {required: true, trigger: 'blur'}
          ],
          accessKeySecret: [
            {required: true, trigger: 'blur'}
          ],
          bucketName: [
            {required: true, trigger: 'blur'}
          ],
          objectName: [
            {required: true, trigger: 'blur'}
          ],
        },
        s3: {
          endpoint: '',
          accessKey: '',
          secretKey: '',
          bucketName: '',
          objectName: ''
        },
        s3Rules: {
          endpoint: [
            {required: true, trigger: 'blur'}
          ],
          accessKey: [
            {required: true, trigger: 'blur'}
          ],
          secretKey: [
            {required: true, trigger: 'blur'}
          ],
          bucketName: [
            {required: true, trigger: 'blur'}
          ],
          objectName: [
            {required: true, trigger: 'blur'}
          ],
        },
        rawTextFileDefaultName: {
          GC_LOG: "gc.log",
          THREAD_DUMP: "jstack.log"
        },
        rawText: {
          originalName: '',
          content: ''
        },
        rawTextRules: {
          originalName: [
            {required: true, trigger: 'blur'}
          ],
          content: [
            {required: true, trigger: 'blur'}
          ],
        },

        publicKeyViewVisible: false,
        publicKey: '',

        transferProgressViewVisible: false,
        transferProgress: 0,
        transferState: null,
        transferErrorMessage: '',
        currentProgress: '',
        lastTransferredSize: 0,
        transferredSize: 0,
        totalSize: 0,
        transferSpeed: 0,
        fileName: '',
        pollingInternal: 1000,

        inTransferring: false,
      }
    },
    watch: {
      fileType: function (newValue, oldValue) {
        this.rawText.originalName = this.rawTextFileDefaultName[newValue]
      }
    },
    mounted() {
      this.rawText.originalName = this.rawTextFileDefaultName[this.fileType]
    },
    methods: {
      service,
      rawTextUploadSupported() {
        return this.rawTextFileDefaultName[this.fileType] !== undefined
      },
      changeAuthType(authType) {
        this.$refs['scpForm'].clearValidate()
        this.scpRules.password[0].required = authType === 'password'
      },
      loadPublicKey() {
        if (!this.publicKey) {
          axios.get(service('/publicKey')).then(resp => {
            this.publicKey = resp.data
          })
        }
        this.publicKeyViewVisible = true
      },
      publicKeyCopySuccessfully() {
        this.$message(
            {
              duration: 1000,
              message: this.$t('jifa.copySuccessfully')
            });
      },
      urlConfirm() {
        this.$refs['urlForm'].validate((valid) => {
          if (valid) {
            let formData = new FormData()
            formData.append('url', this.url.url)
            this.doTransfer('/file/transferByURL', formData)
          }
        })
      },
      fileSystemConfirm() {
        this.$refs['fileSystemForm'].validate((valid) => {
          if (valid) {
            let formData = new FormData()
            formData.append('path', this.fileSystem.path)
            formData.append('move', this.fileSystem.moveOrCopy === 'move')
            this.doTransfer('/file/transferByFileSystem', formData)
          }
        })
      },
      scpConfirm() {
        this.$refs['scpForm'].validate((valid) => {
          if (valid) {
            let usePublicKey = this.scp.authType === 'publicKey'
            let formData = new FormData()
            formData.append('hostname', this.scp.hostname)
            formData.append('path', this.scp.path)
            formData.append('user', this.scp.user)
            formData.append('usePublicKey', usePublicKey)
            if (!usePublicKey) {
              formData.append('password', this.scp.password)
            }
            this.doTransfer('/file/transferBySCP', formData)
          }
        })
      },
      ossConfirm() {
        this.$refs['ossForm'].validate((valid) => {
          if (valid) {
            let formData = new FormData()
            formData.append('endpoint', this.oss.endpoint)
            formData.append('accessKeyId', this.oss.accessKeyId)
            formData.append('accessKeySecret', this.oss.accessKeySecret)
            formData.append('bucketName', this.oss.bucketName)
            formData.append('objectName', this.oss.objectName)
            this.doTransfer('/file/transferByOSS', formData)
          }
        })
      },
      s3Confirm() {
        this.$refs['s3Form'].validate((valid) => {
          if (valid) {
            let formData = new FormData()
            formData.append('endpoint', this.s3.endpoint)
            formData.append('accessKey', this.s3.accessKey)
            formData.append('keySecret', this.s3.secretKey)
            formData.append('bucketName', this.s3.bucketName)
            formData.append('objectName', this.s3.objectName)
            this.doTransfer('/file/transferByS3', formData)
          }
        })
      },
      rawTextConfirm() {
        this.$refs['rawTextForm'].validate((valid) => {
          if (valid) {
            let formData = new FormData()
            const blob = new Blob([this.rawText.content])
            const file = new File([blob], this.rawText.originalName)
            formData.append('file', file)
            formData.append('type', this.fileType)
            const config = {
              headers: {
                'content-type': 'multipart/form-data'
              },
            }
            this.inTransferring = true
            axios.post(service('/file/upload'), formData, config).then(resp => {
              this.onSuccess(resp.data)
            })
          }
        })
      },
      onSuccess() {
        this.transferState = 'success'
        this.inTransferring = false
        this.$notify({
          title: this.$t('jifa.success'),
          type: 'success',
          duration: 1500,
          onClose: () => {
            // clean
            if (this.$jifa.dev() && this.$refs['uploadComp']!=null) {
              this.$refs['uploadComp'].clearFiles()
            }
            this.publicKeyViewVisible = false
            this.transferProgressViewVisible = false
            this.transferProgress = 0
            this.transferState = null
            this.transferErrorMessage = ''
            this.currentProgress = ''
            this.lastTransferredSize = 0
            this.transferredSize = 0
            this.totalSize = 0
            this.transferSpeed = 0
            this.fileName = ''
            this.$emit('transferFileFinishNotify');
          }
        });
      },
      transferProgressPoll() {
        let self = this;
        if (!self || self._isDestroyed) {
          return
        }
        axios.get(service('/file/transferProgress'), {
          params: {
            name: this.fileName,
            type: this.fileType
          }
        }).then(resp => {
          let progress = resp.data
          this.totalSize = progress.totalSize
          this.lastTransferredSize = this.transferredSize
          this.transferredSize = progress.transferredSize
          this.currentProgress = toReadableSizeWithUnit((this.transferredSize - this.lastTransferredSize) / (this.pollingInternal / 1000))
              + '/s '
              + "[" + toReadableSizeWithUnit(this.transferredSize)
              + ", " + (this.totalSize > 0 ? toReadableSizeWithUnit(this.totalSize) : " ? ") + "]"
          if (progress.percent <= 1) {
            this.transferProgress = parseFloat((progress.percent * 100).toPrecision(3))
          } else {
            this.transferProgress = 99.9
          }
          if (progress.state === 'SUCCESS') {
            this.onSuccess()
          } else if (progress.state === 'ERROR') {
            this.transferState = 'exception'
            this.transferErrorMessage = progress.message
            this.inTransferring = false
          } else if (progress.state === 'IN_PROGRESS' || progress.state === 'NOT_STARTED') {
            setTimeout(this.transferProgressPoll, this.pollingInternal)
          }
        })

      },
      doTransfer(api, formData) {
        this.inTransferring = true
        this.currentProgress = '0 [0, 0]'
        this.transferProgress = 0
        this.transferState = null
        this.transferErrorMessage = ''
        this.totalSize = 0
        this.transferredSize = 0
        this.lastTransferredSize = 0

        formData.append('type', this.fileType)
        axios.post(service(api), new URLSearchParams(formData)).then(resp => {
          this.fileName = resp.data.name
          this.transferProgressViewVisible = true
          this.transferProgressPoll()
        }).catch(e => {
          this.transferState = 'exception'
          this.transferErrorMessage = e.response.data.message
          this.transferProgressViewVisible = true
          this.inTransferring = false
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
