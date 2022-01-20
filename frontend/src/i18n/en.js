/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
    searchTip:'Use Java regex for text searching(such as matching java.lang.String with the regex pattern .*String.* ). Use >num,<num,>=num,<=num,!=num,num for numeric searching',
    searchPlaceholder:'Search...',
    heapDumpAnalysis: 'Heap Dump Analysis',
    gclogAnalysis: 'GC Log Analysis',

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
    addGCLogFile: 'Add GC Log File',
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
    release: 'release',
    download: 'Download File',
    edit: 'edit',
    delete: 'Delete',
    loading: 'Loading',
    goToOverViewPrompt: 'Go to the overview page',
    deletePrompt: 'This will permanently delete the file. Do you want to continue?',
    deleteSuccessPrompt: 'Delete success!',
    deleteFailedPrompt: 'Delete failed!',
    deleteCanceled: 'Delete operation is canceled',
    downloadBegin: 'Downloading has began. Please wait',
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
      downloadFile: 'Download file. Max supported size is 512MB',
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

    gclog:{
      minute:'M',
      hour:'H',
      day:'D',

      overview: 'Overview',
      gcCause: 'GC Cause',
      gcPhase: 'GC Phase',
      gcCauseAndPhase: 'GC Cause And Phase',
      gcDetail: 'GC Detail',
      graph: 'Graphs',
      count: 'Count',
      avgPause: 'Avg Pause',
      maxPause: 'Max Pause',
      totalPause: 'Total Pause',
      pausePercent: 'Time Percentage',
      avgTime: 'Avg Time',
      maxTime: 'MaxTime',
      totalTime: 'Total Time',
      avgInterval:'Avg Interval',
      timePercent: 'Time Percent',
      noRealTime: 'Real time of gc is not displayed in the log. Searching has to be based on the uptime. Strongly recommend turn on the vm option to display real time.',

      youngGC: 'Young GC',
      fullGC: 'Full GC',
      promotion:"Promotion",
      allocation:"Allocation",
      reclamation:"Reclamation",
      youngRegion:"Young",
      oldRegion:"Old",
      humongousRegion:"Humongous",
      metaspaceRegion:"Metaspace",
      metaspaceMax:"Max",
      totalHeap:"Used",
      heapMax:"Nax",
      stwTooltip:"Stop the word phase.",

      gcOverview:{
        basicInfo:"Basic Info",
        diagnosis:"Diagnosis",
        kpi:"Key Performance Indicators",
        problems:"Problems",
        suggestions:"Suggestions",


        vmOptions:"VM Options",
        collector:"Collector",
        duration:"Log Duration",
        heapSize:"Heap Size",
        youngGenSize:"Young Generation Size",
        oldGenSize:"Tenured Generation Size",
        metaspaceSize:"Metaspace Size",

        throughput:"Throughput",
        gcDurationPercentage:"GC Time Percentage",
        throughputHint:"Throughput is percentage of time spent in processing real transactions vs time spent in GC. Higher throughput means lower gc overhead.",
        maxPause:"Max Pause",
        youngGCIntervalAvgMin: "Avg/Max Young GC Interval",
        youngGCPauseAvgMax:"Avg/Max Young GC Pause",
        oldGCIntervalAvgMin:"Avg/Min Old GC Interval",
        fullGCIntervalAvgMin:"Avg/Min Full GC Interval",
        fullGCPauseAvgMax:"Avg/Max Full GC Pause",
        promotionAvgMax:"Avg/Max Object Promotion",
        promotionSpeed:"Object Promotion Speed",
        objectCreationSpeed:"Object Creation Speed",
      },
      graphs:{
        timeSpan:"Time Span",
        timePoint:"Time",
        noData: 'No data available.',
        gcCount:'GC Count',
        gcPause: 'Pause Time(ms)',
        heap:'Heap(MB)',
        metaspace:'Metaspace(MB)',
        alloRec:'Allocation & Reclamation rate(MB/s)',
        promotion:'Promotion rate(KB/s)',
        gccycle:'GC Cycle(%)',
      },
      cause:{
        systemgc:'Triggered when System.gc() or Runtime.getRuntime().gc() is called.',
        jvmti:"Triggered whenForceGarbageCollection is called using JVMTI.",
        gclocker:'When a thread is in JNI critical section and a gc is triggered，GC Locker will prevent this gc from executing and prevent other threads from entering critical section.  When the last thread exits the critical section, GCLocker Initiated GC is triggered.',
        heapInspection:'Triggered by heap inspection operation(such as jmap).',
        heapDump:'Triggered by heap dump operation.',
        allocationFail:'Triggered when there is insufficient space to allocate object.',
        metaspace:'Full gc is triggered when there is insufficient space in Metaspace.',
        ergonomics:'Triggered in order to adjust heap size dynamically to meet a specified goal such as minimum pause time or throughput.',
        g1Evacuation:'Triggered when there is insufficient space to allocate object.',
        humongous:'Full GC is triggered when there is insufficient space to allocate humongous object. A humongous object is larger than 50% of heap region size.',
        lastDitch:'After Metadata GC Threshold GC, JVM will trigger another full gc and clear soft reference.',
        promotionFail:'During young gc，if there is not sufficient space in tenured generation for promotion, this gc will become an expensive full gc.',
        toSpaceExhausted:'During young gc, if there is not sufficient region for promotion or as to space, all objects in young generation will enter tenured generation.',
        proactive:'JVM actively triggers a gc to reduce heap usage. Typically triggered when object allocation rate is low.',
        allocationRate:'Triggered when JVM estimates that heap will be used up according to the current object allocation rate',
        timer:'Triggered periodically',
        allocationStall:'Triggered when heap is used up',
        highUsage:'Triggered when heap usage is higher than a specific percent.',
        warmup:'After JVM starts up, if no other gcs are triggered, three gcs will triggered when heap usage is higher than 10%, 20% and 30%.',
        metaspaceClearSoftRef: 'After Metadata GC Threshold GC, JVM will trigger another full gc and clear soft reference.',
        g1Periodic:"GC triggered periodically",
        dcmd:"Full gc triggered by jcmd",
        g1Compaction:'G1 Full gc',
        g1Preventive:'Triggered to prevent "to-space exhausted"',
      },
      detail:{
        filters:"Filters",
        eventType:"GC Event Type",
        logTime:"Log Time",
        pauseTime:"Pause Time",
      },
      diagnosis:{
        problems:{
          cmsFrequentConcurrentMarkSwept:'Detect continual CMS GC.',
          fullGC:'{count} full GC happened.',
          g1FrequentConcurrentCycle:'Detect continual concurrent cycle.',
          g1ToSpaceExhausted:'{count} to-space exhausted events happened.',
          humongousToSpaceExhausted:'Detect suspicious to-space exhausted event because of humongous object allocation.',
          noProblem:"No problem detected.",
          lowThroughput:"Throughput({throughput}%) was too low.",
          metaspaceFullGC:"{count} full GC happened because of Metadata GC Threshold.",
          systemGC:"{count} Full GC happened because of System.gc() calls.",
          promotionFailed:"{count} promotion failures happened during young GC.",
          concurrentFailure:"{count} concurrent failures happened during CMS.",
          smallYoungGen:"Young generation is too small，just {percent}% of the whole heap",
          smallOldGen:"Old generation is too small，just {percent}% of the whole heap",
          zgcTooFrequent:"GCs are too frequent.",
          allocationStall:"There are too many allocation stalls, which may lead to long pauses.",
        },
        suggestions:{
          addHeapRegionSize:'Enlarge heap region size by -XX:G1HeapRegionSize',
          noXMN:'Recommend not set -Xmn for g1 collector',
          reviewHuge:'Check if there is too many large object allocation. You can use Heap Dump Analysis of Grace.',
          addGCThread:"Add parallel gc thread count(no more than CPU count) by -XX:ParallelGCThreads.",
          enlargeHeap:"Use a larger heap size by -Xmx -Xms.",
          enlargeMetaspace:"Enlarge metaspace size by -XX:MetaspaceSize -XX:MaxMetaspaceSize, at least 512M recommended.",
          reviewMetaspace:"Check what objects is metaspace filled with.",
          reviewSystemGC:"Check if it is necessary to call System.gc().",
          disableSystemGC:"Just disable System.gc() by -XX:+DisableExplicitGC.",
          enlargeOld:"Enlarge size of tenured generation by -Xmn.",
          lowerCMSOldGCThreshold:"Reduce CMS GC trigger threshold by -XX:CMSInitiatingOccupancyFraction.",
          reviewOld:"Check if there is memory leak or too many large object allocation. You can use Heap Dump Analysis of Grace.",
          enlargeYoung:"Enlarge young genertion size by -Xmn",
          reviewFullGCCause:"Check causes of Full GCs",
          noNewSizeEqual:"Don't set -MaxNewSize and -NewSize equal",
          setReasonableXmn:"Please set reasonable Xmn (or similar JVM options like MaxNewSize)",
          addConcurrentGCThread:"Add concurrent gc thread count by -XX:ConcGCThreads.",
          enlargeHeapWithRecommend:"Use a larger heap, recommend setting is -Xmx{size}g -Xms{size}g",
          addZGCAllocationSpikeTolerance:"Increase -XX:ZAllocationSpikeTolerance or Decrease -XX:ZHighUsagePercent (default: 95, trigger gc if the heap reaches 95% high usage)",
        }
      },
    },
  }
};