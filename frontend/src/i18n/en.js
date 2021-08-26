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
      request:'Request to start worker',
      requestFailed:'Failed to request to start worker, please try again later',
      error:'Starting worker fails, please try again later',
      success:'Succeed to start worker!',
      timeout:'Starting worker timeout, please try again later',
      starting:'Starting worker',
    },
    searchTip:'Use Java regex for text searching(such as matching java.lang.String with the regex pattern .*String.* ). Use >num,<num,>=num,<=num,!=num,num for numeric searching',
    searchPlaceholder:'Search...',
    heapDumpAnalysis: 'Heap Dump Analysis',

    unlockFileSuccessPrompt:'Unlock successfully!',
    unlockFilePrompt:'Are you sure unlocking this file to allow arbitrary access?',
    unlockFile:'Unlock File',
    setting: 'Setting',
    diskCleanup: 'Disk Cleanup',
    help: 'Help',
    consoleMsg: '',
    getStarted: 'Get Started',
    success: 'Success',
    console: 'Console',
    setUserWorker: 'Set Worker',
    qm: '?',
    feedback: 'Feedback',
    options: 'Options',
    optionsWithHelp: 'Options',
    close: 'Close',
    uploadPrompt: 'Choose your file (drag or click)',
    enterPrompt: 'Please enter ',
    inLine: 'In Line',
    addFile: 'Add File',
    addHeapDumpFile:'Add Heap Dump File',
    copy: 'Copy',
    copySuccessfully: 'Copy Successfully',
    requestFailed: 'Request failed',
    config: 'Config',
    prompt: 'Prompt',
    confirm: 'Confirm',
    reset: 'Reset',
    cancel: 'Cancel',
    fileTransfer: 'File Transfer',
    progress: 'Progress',
    analyze: 'analyze',
    reanalyze: 'Reanalyze',
    release: 'Release',
    edit: 'edit',
    delete: 'Delete',
    loading: 'Loading',
    goToOverViewPrompt: 'Go to the overview page',
    deletePrompt: 'This will permanently delete the file. Do you want to continue?',
    deleteSuccessPrompt: 'Delete success!',
    deleteFailedPrompt: 'Delete failed!',
    deleteCanceled: 'Delete operation is canceled',
    returnValue: 'Are you sure to leave?',
    gotoParseFile: 'Will go to parse file',

    typeKeyWord: 'type key word to search',

    transferring: 'transferring',
    transferError: 'transfer error',

    show: 'Show',
    hide: 'Hide',

    expandResultDivWidth: 'Expand Width',
    shrinkResultDivWidth: 'Shrink Width',
    resetResultDivWidth: 'Reset Width',

    addResultDivWidth: 'Add width',

    backToHome: 'Back to home',
    promote404: 'The page you request was not found, you can click the following button to go to home page.',

    tip: {
      copyName: 'Copy file name',
      rename: 'Rename file name',
      uploadToOSS: 'Upload file to OSS',
      setShare: 'Set file as shared',
      deleteFile: 'Delete file permanently',
    },

    heap: {
      basicInformation: 'Basic Information',
      reanalyzePrompt: 'Do you want to continue?',
      releasePrompt: 'Do you want to continue?',
      overview: 'Overview',
      leakSuspects: 'Leak Suspects',
      description: 'Description',
      detail: 'Detail',
      GCRoots: 'GC Roots',
      systemProperty: 'System Property',
      OSBit: 'OS Bit',
      jvmInfo: 'JVM',
      heapCreationDate: 'Creation Date',
      usedHeapSize: 'Used Heap Size',
      numberOfClasses: 'Class Count',
      numberOfObjects: 'Object Count',
      numberOfClassLoaders: 'Class Loaders Count',
      numberOfGCRoots: 'GC Root Count',
      threadInfo: 'Thread Info',
      dominatorTree: 'Dominator Tree',
      histogram: 'Histogram',
      unreachableObjects: 'Unreachable Objects',
      duplicatedClasses: 'Duplicated Classes',
      classLoaders: 'Class Loaders',
      directByteBuffer: 'Direct Byte Buffer',
      compare: 'Heap File Compare',
      ref: {
        object: {
          label: 'References by Object',
          outgoing: 'outgoing references',
          incoming: 'incoming references',
        },
        type: {
          label: 'Reference by Class',
          outgoing: 'outgoing references',
          incoming: 'incoming references',
        }
      },

      pathToGCRoots: 'Path to GC Roots',
      mergePathToGCRoots: 'Merge Path to GC Roots',
    },
  }
};