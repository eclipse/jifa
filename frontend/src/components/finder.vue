<!--
    Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<template xmlns:v-clipboard="http://www.w3.org/1999/xhtml">
  <el-container style="height: 100%">
    <el-dialog
            :title="this.getAddFileTitle()"
            :visible.sync="transferViewVisible"
            width="40%" :close-on-click-modal=false>
      <TransferFile :fileType="currentMenuItem" :transferViewVisible="transferViewVisible"
                    @transferFileFinishNotify="()=>{this.transferViewVisible = false; this.go(1)}"
                    style="margin-top: -20px"/>
    </el-dialog>

    <el-header>
      <view-menu subject="finder"
                 :fileType="currentMenuItem"
                 :title="this.getAddFileTitle()"
                 @chooseMenu="item => this.currentMenuItem= item"
                 @addFile="() => this.transferViewVisible = true"/>
    </el-header>

    <el-dialog
            :title="$t('jifa.prompt')"
            :visible="true"
            v-if="fileToDelete"
            width="25%"
            :before-close="cancelDelete">
      <span style="font-size: 20px; color: #e6a23c"><i class="el-icon-warning"/> &nbsp;</span>
      <span style="font-size: 18px">{{ $t('jifa.deletePrompt') }}</span>
      <span slot="footer" class="dialog-footer">
        <el-button @click="cancelDelete">{{ $t('jifa.cancel') }}</el-button>
        <el-button @click="doDelete" type="primary">{{ $t('jifa.confirm') }}</el-button>
      </span>
    </el-dialog>

    <el-main v-loading.fullscreen.lock="loading" style="height: 100%">
      <el-row type="flex" v-for="row in rows" :key="row">
        <el-col :span="6" v-for="col in (row < rows ? cols : colsOfLastRow)" :key="col">
          <el-card class="box-card" shadow="hover" style="margin: 20px">
            <div>
              <el-row style="margin: -15px -8px 8px -8px">
                <el-col align="right" class="icons-col">
                  <el-tooltip class="item" effect="light" :content="$t('jifa.tip.copyName')" placement="top-start">
                    <span>
                      <el-link icon="el-icon-document-copy" v-clipboard:copy="file(row, col).name"
                               :underline="false" target="_blank"/>
                    </span>
                  </el-tooltip>

                  <el-tooltip class="item" effect="light" :content="$t('jifa.tip.rename')" placement="top-start"
                              v-if="file(row,col).hasOwnProperty('displayName')">
                    <span>
                      <el-divider direction="vertical"></el-divider>
                      <el-link icon="el-icon-edit" :underline="false" @click="updateFile(file(row,col))"/>
                    </span>
                  </el-tooltip>

                  <el-tooltip class="item" effect="light" :content="$t('jifa.tip.setShare')" placement="top-start"
                              v-if="file(row,col).hasOwnProperty('shared')">
                    <span>
                      <el-divider direction="vertical"></el-divider>
                      <el-link :icon="file(row, col).shared ? 'el-icon-unlock' : 'el-icon-lock'"
                               v-on:click="toggleSharedState(file(row, col))"
                               :underline="false"
                               target="_blank"/>
                    </span>
                  </el-tooltip>

                  <el-tooltip class="item" effect="light" :content="$t('jifa.tip.downloadFile')" placement="top-start"
                              v-if="transferIsSuccess(file(row,col))">
                    <span>
                      <el-divider direction="vertical"></el-divider>
                      <el-link icon="el-icon-download" :underline="false"
                               target="_blank"
                               download
                               :href="`/jifa-api/file/download?name=${file(row,col).name}&type=${currentMenuItem}`"
                      />
                    </span>
                  </el-tooltip>

                  <el-tooltip class="item" effect="light" :content="$t('jifa.tip.deleteFile')" placement="top-start"
                              v-if="canDelete(file(row,col))">
                    <span>
                      <el-divider direction="vertical"></el-divider>
                      <el-link icon="el-icon-delete" :underline="false"
                               v-on:click="fileToDelete =  file(row, col).name"/>
                    </span>
                  </el-tooltip>
                </el-col>
              </el-row>

              <el-row type="flex">
                <el-col>
                  <p style='font-size: 15px; margin:0 auto; white-space: nowrap; text-overflow:ellipsis; overflow:hidden;
                            color: #606266;'>
                    <i class="el-icon-document"></i>
                    {{file(row, col).displayName ? file(row, col).displayName : file(row, col).name}}
                  </p>
                </el-col>
              </el-row>

              <el-row :align='"middle"' type="flex">
                <el-col :span="12">
                  <p style="font-size: 12px; margin: 10px auto 7px; white-space: nowrap; color: #606266;" align="left">
                    {{toReadableSizeWithUnit(file(row, col).size)}}</p>
                </el-col>

                <el-col :span="12">
                  <p style="font-size: 12px; margin:10px auto 7px; white-space: nowrap; color: #606266;" align="right">
                    {{formatDate(new Date(file(row, col).creationTime), "yyyy-MM-dd HH:mm:ss")}}</p>
                </el-col>
              </el-row>

              <el-row>
                <hr style="margin: 0 -8px 7px -8px;"/>
              </el-row>

              <el-row type="flex" justify="space-around" style="margin-bottom: -15px">
                <el-col :span="8" align="middle"
                        v-if="file(row, col).transferState === 'NOT_STARTED' || file(row, col).transferState ==='IN_PROGRESS'">
                  <el-button type="text"><i class="el-icon-loading"></i> {{$t('jifa.transferring')}}</el-button>
                </el-col>

                <el-col :span="8" align="middle"
                        v-if="file(row, col).transferState === 'ERROR'">
                  <el-button type="text"><i class="el-icon-error"></i> {{$t('jifa.transferError')}}</el-button>
                </el-col>

                <el-col :span="8" align="middle"
                        v-if="file(row, col).transferState === 'SUCCESS'">
                  <router-link
                          :to="{path : analysisPath() , query: {file: file(row, col).name, displayName: file(row, col).displayName} }">
                    <el-button type="text"><i class="el-icon-view"></i> {{$t('jifa.analyze')}}</el-button>
                  </router-link>
                </el-col>
              </el-row>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row type="flex" justify="space-around" style="margin-top: 77px">
        <el-col :span="8">
          <el-pagination
                  background
                  layout="prev, pager, next"
                  :current-page="page"
                  :page-size="pageSize"
                  :total="totalSize"
                  @current-change="handleCurrentPageChange"
                  hide-on-single-page
                  align="center">
          </el-pagination>
        </el-col>
      </el-row>
    </el-main>
  </el-container>
</template>

<script>
import axios from 'axios'

import {formatDate} from 'element-ui/src/utils/date-util'
import {service, toReadableSizeWithUnit} from '../util'
import TransferFile from './transferFile'
import ViewMenu from './menu/ViewMenu'

const defaultMenuItem = 'HEAP_DUMP'

  export default {
    components: {TransferFile, ViewMenu},
    data() {
      return {
        files: [],
        cols: 4,
        rows: 0,
        colsOfLastRow: 0,

        page: 0,
        pageSize: 12,
        totalSize: 0,

        loading: false,
        defaultMenuItem,

        transferViewVisible: false,
        fileToDelete: null,

        currentMenuItem: defaultMenuItem,
      }
    },
    methods: {
      getAddFileTitle() {
        switch (this.currentMenuItem) {
          case "HEAP_DUMP":
            return this.$i18n.t('jifa.addHeapDumpFile');
          case "GC_LOG":
            return this.$i18n.t('jifa.addGCLogFile');
          case "THREAD_DUMP":
            return this.$i18n.t('jifa.threadDump.addFile');
          default:
            return this.$i18n.t('jifa.addFile');
        }
      },

      file(row, col) {
        return this.files[(row - 1) * this.cols + col - 1]
      },

      service,
      formatDate,
      toReadableSizeWithUnit,

      handleCurrentPageChange(page) {
        this.go(page)
      },

      analysisPath() {
        let lowered = this.currentMenuItem.toLowerCase().split("_");

        if (lowered.length === 0) {
          return "";
        } else if (lowered.length === 1) {
          return lowered[0];
        }

        let finalPath = lowered[0];
        for (let i = 1; i < lowered.length; i++) {
          if (lowered[i].length === 0) {
            continue;
          }
          finalPath += lowered[i][0].toUpperCase();
          if (lowered[i].length > 1) {
            finalPath += lowered[i].substring(1);
          }
        }
        return finalPath;
      },

      go(page) {
        this.page = page
        this.fetchFiles()
      },

      toggleSharedState(file) {
        let formData = new FormData()
        formData.append('name', file.name)
        axios.post(service(file.shared ? "/file/unsetShared" : "/file/setShared"),
            new URLSearchParams(formData)).then(() => {
          file.shared = !file.shared
        })
      },

      transferIsSuccess(file) {
        return file.transferState === 'SUCCESS'
      },

      canDelete(file) {
        return file.transferState === 'SUCCESS' || file.transferState === 'ERROR'
      },

      cancelDelete() {
        this.fileToDelete = null
        this.$message({
          type: 'info',
          message: this.$t('jifa.deleteCanceled'),
          duration: 1000
        });
      },

      doDelete() {
        if (!this.fileToDelete) {
          return
        }
        let formData = new FormData()
        formData.append('type', this.currentMenuItem)
        formData.append('name', this.fileToDelete)
        axios.post(service("/file/delete"), new URLSearchParams(formData)).then(() => {
          this.fileToDelete = null
          this.$message({
            message: this.$t('jifa.deleteSuccessPrompt'),
            type: 'success',
            duration: 500,
            onClose: () => {
              if (this.files.length === 1) {
                this.go(this.page > 1 ? this.page - 1 : this.page)
              } else {
                this.go(this.page)
              }
            }
          });
        }).catch(() => {
          this.fileToDelete = null
          this.$message({
            message: this.$t('jifa.deleteFailedPrompt'),
            type: 'error',
            duration: 1000
          });
        })
      },

      fetchFiles() {
        this.loading = true;
        axios.get(service('/files'), {
          params: {
            type: this.currentMenuItem,
            page: this.page,
            pageSize: this.pageSize
          }
        }).then(resp => {
          this.files = resp.data.data
          this.totalSize = resp.data.totalSize
          if (this.files.length > 0) {
            this.rows = Math.ceil(this.files.length / this.cols)
            let mod = this.files.length % this.cols
            this.colsOfLastRow = mod > 0 ? mod : this.cols
          } else {
            this.rows = 0
            this.colsOfLastRow = 0
          }
          this.loading = false
        })
      },

      updateFile(file) {
        this.$prompt('New display name: ', 'Edit', {
          confirmButtonText: this.$t("jifa.confirm"),
          cancelButtonText: this.$t('jifa.cancel'),
          inputValue: file.displayName
        }).then(({value}) => {
          let formData = new FormData()
          formData.append('name', file.name)
          formData.append('displayName', value)
          axios.post(service("/file/updateDisplayName"), new URLSearchParams(formData))
              .then(() => {
                this.go(this.page)
              })
        })
      }
    },

    watch: {
      currentMenuItem() {
        this.go(1)
      },
    },

    created() {
      this.go(1)
    }
  }
</script>

<style scoped>

.icons-col .el-divider {
  margin-left: 4px;
  margin-right: 4px;
}
</style>
