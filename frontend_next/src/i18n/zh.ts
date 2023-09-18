/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
export default {
  jifa: {
    common: {
      confirm: '确定',
      submit: '提交',
      operations: '操作',
      copy: '复制',
      search: '搜索',
      result: '结果',
      description: '描述',
      detail: '细节',
      noData: '没有数据',
      file: '文件',
      messageBeforeLeave: '确定离开吗？',
      clickToLoadMore: '点击加载更多数据',
      expandedColumnWidth: '增加列宽'
    },

    header: {
      login: '登录',
      logout: '登出'
    },

    serviceUnavailable: {
      title: '服务不可用',
      subtitle: '请联系系统管理员'
    },

    file: {
      name: '名称',
      type: '类型',
      size: '大小',
      uploadedTime: '上传时间',
      operations: '操作',

      all: '所有类型',
      heapDump: '堆内存快照',
      GCLog: 'GC 日志',
      threadDump: '线程栈快照',

      new: '新文件',

      analyze: '分析'
    },

    form: {
      requiredMessage: '请输入{0}',
      selectionRequiredMessage: "请选择 '{0}'",
      invalidLengthMessage: '{0}的有效长度为 [{1}, {2}]'
    },

    loginForm: {
      loginTitle: '请登录您的账号',
      loginSubtitle: '没有可用的账号？',
      signup: '注册',
      signupTitle: '注册一个新账号',
      signupSubtitle: '使用已有的账号？',
      login: '登录',
      usernameLabel: '用户名',
      usernamePlaceholder: "输入您的用户名，如: grace{'@'}gmail.com",
      passwordLabel: '密码',
      passwordPlaceholder: '输入您的密码',
      fullNameLabel: '姓名',
      fullNamePlaceholder: '输入您的姓名',
      emailLabel: '邮箱',
      usedAsUsername: '（将作为您的用户名）',
      emailPlaceholder: "输入您的邮箱, 如 grace{'@'}gmail.com",
      or: '或者',
      continueWith: '使用 {0} 账号登录',
      invalidEmailMessage: '请输入有效的邮箱地址'
    },

    fileTransferForm: {
      transferMethod: '传输方式',
      type: '类型',

      upload: '上传',

      dragOrClickToUpload: '将文件拖拽到此处 或 点击进行文件选择',

      host: '主机',
      user: '用户',
      authentication: '认证方式',
      password: '密码',
      publicKey: '公钥',
      path: '路径',

      publicKeyCopyPrompt: '请复制公钥到 ~/.ssh/authorized_keys'
    },

    analysis: {
      log: '日志',
      success: '分析成功'
    },

    heapDump: {
      option: {
        descOfKeepUnreachableObjects:
          '如果启用此选项，分析器会将不可达对象视为不可达根，进而分析其中的引用关系。 否则，将不对不可达对象进行进一步的分析，仅统计类型信息，即不可达类视图。',
        descOfStrictness: "'strictness' 表示当分析过程中遇到错误时的后续动作",
        descOfStopStrictness: '终止分析',
        descOfWarnStrictness: '报告警告信息并继续分析',
        descOfPermissiveStrictness: '报告警告信息，尝试修复错误并继续分析'
      },

      tab: {
        overview: '概况',
        leakSuspects: '泄漏检测',
        dominatorTree: '支配树',
        GCRoots: 'GC 根对象',
        histogram: '类',
        threads: '线程',
        unreachableObjects: '不可达对象',
        systemProperties: '系统属性',
        directByteBuffers: '堆外内存',
        classLoaders: '类加载器',
        duplicateClasses: '重复类',
        query: '对象查询语言',

        inspector: '对象视图',
        statics: '静态属性',
        attributes: '属性',
        value: '值'
      },

      overview: {
        basicInformation: '基本信息',
        usedHeapSize: '堆使用大小',
        numberOfClasses: '类数量',
        numberOfObjects: '对象数量',
        numberOfClassLoaders: '类加载器数量',
        numberOfGCRoots: 'GC 根对象数量',
        creationDate: '创建时间',
        identifierSize: '系统位数',

        biggestObjectsChartTitle: '大对象（按对象的支配内存大小计算）'
      },

      column: {
        className: '类型名称',
        percentage: '百分比',
        objectCount: '对象数量',
        superClass: '父类',
        classLoader: '类加载器',

        key: '键',
        value: '值'
      },

      field: {
        type: '类型',
        name: '名称',
        value: '值'
      },

      dynamicTab: {
        objectOutbounds: '此对象引用的对象集合',
        objectInbounds: '引用此对象的对象集合',
        classOutbounds: '此类型引用的类型集合',
        classInbounds: '引用此类型的类型集合',
        pathToGCRoots: 'GC 根路径',
        mergedPathToGCRoots: '合并的 GC 根路径'
      },

      contextmenu: {
        referencesByObject: '对象引用',
        referencesByClass: '类型引用',
        outbounds: '引用集合',
        inbounds: '被引用集合',
        pathToGCRoots: 'GC 根路径',
        mergedPathToGCRoots: '合并的 GC 根路径'
      },

      placeholder: {
        query: '输入 {0}，点击左侧选择框中的链接图标查看帮助文档。'
      }
    }
  }
};
