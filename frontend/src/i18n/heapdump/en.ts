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
    labelOfKeepUnreachableObjects: 'Keep Unreachable Objects',
    descOfKeepUnreachableObjects:
      'If this option is enabled, the analyzer will treat the unreachable objects as the unreachable roots, and then analyze the reference relationship in them. Otherwise, no further analysis will be performed on the unreachable objects, but only the statistics of types of the unreachable object, that is Unreachable Objects Histogram.',
    labelOfStrictness: 'Strictness',
    descOfStrictness: "'Strictness' indicates the follow-up action when an error occurred",
    descOfStopStrictness: 'Throw an error and stop analyzing the dump',
    descOfWarnStrictness: 'Raise a warning and continue',
    descOfPermissiveStrictness: 'Raise a warning and try to "fix" it',
    labelOfDiscardObjects: 'Discard objects',
    descOfDiscardObjects: 'Discard some objects to reduce memory consume while analyse',
    descOfDiscardObjectsDetail: 'Sometimes a heap dump is generated with more objects than Memory Analyzer can handle, either from lack of heap to run Memory Analyzer itself, or because the number exceeds the Memory Analyzer limit of 2,147,483,639 objects. This option controls some experimental settings to help analyze such huge dumps, by purposely discarding objects in the original heap dump.',
    labelOfDiscardObjectsRatio: "Discard ratio",
    descOfDiscardObjectsRatio: 'A number between 0 and 100, treated as a percentage. Approximately this percentage of ordinary objects matching the discard pattern will be discarded by the HPROF parser.',
    labelOfDiscardObjectsPattern: 'Discard pattern',
    descOfDiscardObjectsPattern: 'Only objects with a class name matching this regular expression will be discarded. It is best to chose objects of a type which does not link to other objects, such as primitive arrays, or objects which just link to other such objects. This avoids breaking the object graph too much, and gives a hope that the leak analysis will find the problem.',
    labelAdditionalAnalyseOptions: 'Additional options',
    descAdditionalAnalyseOptions: 'Analyse options of Eclipse Memory Analyser, see: https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.mat.ui.help%2Ftasks%2Fconfigure_mat.html'
  },

  tab: {
    overview: 'Overview',
    leakSuspects: 'Leak Suspects',
    dominatorTree: 'Dominator Tree',
    GCRoots: 'GC Roots',
    histogram: 'Histogram',
    threads: 'Threads',
    unreachableObjects: 'Unreachable Objects',
    systemProperties: 'System Properties',
    envVariables: 'Environment Variables',
    directByteBuffers: 'Direct Byte Buffers',
    classLoaders: 'Class Loaders',
    duplicateClasses: 'Duplicated Classes',
    query: 'OQL',

    inspector: 'Inspector',
    statics: 'Statics',
    attributes: 'Attributes',
    value: 'Value'
  },

  overview: {
    basicInformation: 'Basic Information',
    usedHeapSize: 'Used Heap Size',
    numberOfClasses: 'Class Count',
    numberOfObjects: 'Object Count',
    numberOfClassLoaders: 'Class Loaders Count',
    numberOfGCRoots: 'GC Root Count',
    creationDate: 'Creation Date',
    identifierSize: 'OS Bit',
    jvmOptions: 'JVM Options',

    biggestObjectsChartTitle: 'Biggest Objects (by Retained Size)'
  },

  column: {
    className: 'Class Name',
    percentage: 'Percentage',
    objectCount: 'Object Count',
    superClass: 'Super Class',
    classLoader: 'Class Loader',

    key: 'Key',
    value: 'Value'
  },

  field: {
    type: 'Type',
    name: 'Name',
    value: 'Value'
  },

  dynamicTab: {
    objectOutbounds: 'Outgoing References',
    objectInbounds: 'Incoming References',
    classOutbounds: 'Outgoing References',
    classInbounds: 'Incoming References',
    pathToGCRoots: 'Path to GC Roots',
    mergedPathToGCRoots: 'Merged Path to GC Roots'
  },

  contextmenu: {
    referencesByObject: 'References by Object',
    referencesByClass: 'References by Class',
    outbounds: 'Outgoing',
    inbounds: 'Incoming',
    pathToGCRoots: 'Path to GC Roots',
    mergedPathToGCRoots: 'Merged Path to GC Roots'
  },

  placeholder: {
    query: 'Enter {0}. Click the link icon in the selection box for detailed help.'
  }
}