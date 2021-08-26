/********************************************************************************
 * Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
'use strict';

exports.__esModule = true;
exports.default = {
  jifa: {
    startingStatus:{
      request:'已请求启动worker',
      requestFailed:'请求启动worker失败，请稍后重试',
      error:'启动worker失败，请稍后重试',
      success:'启动worker成功！',
      timeout:'启动worker超时，请稍后重试',
      starting:'正在启动worker',
    },
    searchTip:"搜索文本使用Java语言正则语法，比如用 *.String.* 来搜索java.lang.String，搜索数值使用>num,<num,>=num,<=num,==num,!=num,num的语法",
    searchPlaceholder:'搜索...',
    heapDumpAnalysis: '堆分析',

    unlockFileSuccessPrompt:'解锁成功！',
    unlockFilePrompt:'是否解锁文件，允许任意用户访问?',
    unlockFile:'解锁文件',
    setting: '设置',
    diskCleanup: '清理磁盘',
    help: '帮助',
    consoleMsg: '',
    getStarted: '开始使用',
    success: '成功',
    console: '控制台',
    setUserWorker: '设置 Worker 地址',
    qm: '？',
    feedback: '建议与反馈',
    options: '选项',
    optionsWithHelp: '选项',
    close: '关闭',
    uploadPrompt: '选择文件(拖拽或点击选择)',
    enterPrompt: '请输入',
    inLine: '排队中',
    addFile: '添加文件',
    addHeapDumpFile:'添加 Heap Dump',
    copy: '复制',
    copySuccessfully: '复制成功',
    requestFailed: '请求失败',
    config: '配置',
    prompt: '提示',
    confirm: '确定',
    reset: '重置',
    cancel: '取消',
    fileTransfer: '文件传输',
    progress: '进度',
    analyze: '分析',
    reanalyze: '重新分析',
    release: '释放',
    edit: '编辑',
    delete: '删除',
    loading: '加载中',
    goToOverViewPrompt: '即将进入概况页面...',
    deletePrompt: '此操作将永久删除该文件，是否继续？',
    deleteSuccessPrompt: '删除成功！',
    deleteFailedPrompt: '删除失败！',
    deleteCanceled: '已取消删除',
    returnValue: '确定离开吗?',
    gotoParseFile: '即将解析文件',

    typeKeyWord: '输入关键字搜索',

    transferring: '传输中',
    transferError: '传输失败',

    show: '显示',
    hide: '隐藏',

    expandResultDivWidth: '显示宽度 Expand',
    shrinkResultDivWidth: '显示宽度 Shrink',
    resetResultDivWidth: '显示宽度 Reset',

    backToHome: '返回主页',
    promote404: '您所寻找的页面不存在。可以点击下面的按钮，返回主页。',

    tip: {
      copyName: '复制文件名',
      rename: '修改文件名',
      uploadToOSS: '上传文件到OSS',
      setShare: '设置文件共享',
      deleteFile: '删除文件',
    },

    heap: {
      basicInformation: '基础信息',
      reanalyzePrompt: '是否继续？',
      releasePrompt: '是否继续？',
      overview: '概况',
      leakSuspects: '泄露报表',
      description: '描述',
      detail: '细节',
      GCRoots: 'GC 根对象',
      systemProperty: '系统属性',
      OSBit: '操作系统位数',
      jvmInfo: 'JVM',
      heapCreationDate: '创建时间',
      usedHeapSize: '堆使用大小',
      numberOfClasses: '类数量',
      numberOfObjects: '对象数量',
      numberOfClassLoaders: '类加载器数量',
      numberOfGCRoots: '根对象数量',
      threadInfo: '线程信息',
      dominatorTree: '支配树',
      histogram: '类视图',
      unreachableObjects: '不可达类视图',
      duplicatedClasses: '重复类视图',
      classLoaders: '类加载器视图',
      directByteBuffer: '堆外内存视图',
      compare: '内存文件对比',
      ref: {
        object: {
          label: '对象引用',
          outgoing: '引用对象集合',
          incoming: '被引用对象集合',
        },
        type: {
          label: '类型引用',
          outgoing: '引用类型集合',
          incoming: '被引用类型集合',
        }
      },

      pathToGCRoots: 'GC 根路径',
      mergePathToGCRoots: '合并GC 根路径',
    },
  }
};