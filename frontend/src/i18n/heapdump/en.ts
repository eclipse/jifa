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
  option: {
    descOfKeepUnreachableObjects:
      'If this option is enabled, the analyzer will treat the unreachable objects as the unreachable roots, and then analyze the reference relationship in them. Otherwise, no further analysis will be performed on the unreachable objects, but only the statistics of types of the unreachable object, that is Unreachable Objects Histogram.',
    descOfStrictness: "'strictness' indicates the follow-up action when an error occurred",
    descOfStopStrictness: 'Throw an error and stop analyzing the dump',
    descOfWarnStrictness: 'Raise a warning and continue',
    descOfPermissiveStrictness: 'Raise a warning and try to "fix" it'
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