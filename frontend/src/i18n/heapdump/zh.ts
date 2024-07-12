/********************************************************************************
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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
  option: {
    labelOfKeepUnreachableObjects: '保留不可达对象',
    descOfKeepUnreachableObjects:
      '如果启用此选项，分析器会将不可达对象视为不可达根，进而分析其中的引用关系。 否则，将不对不可达对象进行进一步的分析，仅统计类型信息，即不可达类视图。',
    labelOfStrictness: '分析失败时的策略',
    descOfStrictness: "'分析失败时的策略' 表示当分析过程中遇到错误时的后续动作",
    descOfStopStrictness: '终止分析',
    descOfWarnStrictness: '报告警告信息并继续分析',
    descOfPermissiveStrictness: '报告警告信息，尝试修复错误并继续分析',
    labelOfDiscardObjects: '丢弃部分对象',
    descOfDiscardObjects: '分析的时候丢弃部分对象，以减少 jifa 的堆内存占用，防止 OOM',
    descOfDiscardObjectsDetail: '如果堆内存特别巨大的话，其中某一类 objects 的数量可能会超过 2,147,483,639 这个限制，从而导致 1. analyze 无法为其创建索引数组而解析失败, 2. jifa 本身OOM 而解析失败. 这个选项可以通过指定丢弃类和丢弃比例的方式来丢弃一部分 objects，从而避免这些问题（如果堆内存特别巨大，建议开启此选项）',
    labelOfDiscardObjectsRatio: "丢弃比例",
    descOfDiscardObjectsRatio: '丢弃的百分比，数值范围：0 ~ 100. 匹配了 discard pattern 的类将会被根据这个比例进行随机丢弃.',
    labelOfDiscardObjectsPattern: '丢弃规则',
    descOfDiscardObjectsPattern: '丢弃类的正则匹配表达式，最好选择一些不会引用其他 object 的类，例如: byte\\[\\]，java\\.lang\\.String\ 或者 java\\.lang\\.String\\[\\] （记得对关键字进行转义）.',
    labelAdditionalAnalyseOptions: '其他选项',
    descAdditionalAnalyseOptions: 'Eclipse Memory Analyser 支持的其他选项, 详情: https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.mat.ui.help%2Ftasks%2Fconfigure_mat.html'
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
    envVariables: '环境变量',
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