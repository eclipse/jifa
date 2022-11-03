/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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


import {formatTime, toReadableSizeWithUnit} from "@/util";

export function getPhaseHint(phase) {
  switch (phase) {
    case 'Full GC':
      return 'jifa.gclog.phase.fullGC'
    case 'Concurrent Mark Abort':
      return 'jifa.gclog.phase.concMarkAbort'
    case 'Concurrent Mode Interrupted':
      return 'jifa.gclog.phase.concModeInterrupt'
    case 'Concurrent Mode Failure':
      return 'jifa.gclog.phase.cmFailure'
    case "Allocation Stall":
      return 'jifa.gclog.phase.allocationStall'
    case "Concurrent Mark Reset For Overflow":
      return 'jifa.gclog.phase.cmReset'
    case "Out Of Memory":
      return 'jifa.gclog.phase.oom'
    case "Initial Mark Situation":
      return 'jifa.gclog.phase.initialMarkSituation'
    case "Prepare Mixed Situation":
      return 'jifa.gclog.phase.prepareMixedSituation'
    // todo: add more phases
    default:
      return ""
  }
}

export function getCauseHint(cause) {
  switch (cause) {
    case "CMS Final Remark":
      return 'jifa.gclog.cause.cmsFinalRemark'
    case "System.gc()":
      return 'jifa.gclog.cause.systemgc'
    case "JvmtiEnv ForceGarbageCollection":
      return 'jifa.gclog.cause.jvmti'
    case "GCLocker Initiated GC":
      return 'jifa.gclog.cause.gclocker'
    case "Heap Inspection Initiated GC":
      return 'jifa.gclog.cause.heapInspection'
    case "Heap Dump Initiated GC":
      return 'jifa.gclog.cause.heapDump'
    case "Allocation Failure":
      return 'jifa.gclog.cause.allocationFail'
    case "Metadata GC Threshold":
      return 'jifa.gclog.cause.metaspace'
    case "Ergonomics":
      return 'jifa.gclog.cause.ergonomics'
    case "G1 Evacuation Pause":
      return 'jifa.gclog.cause.g1Evacuation'
    case "G1 Humongous Allocation":
      return ['jifa.gclog.cause.humongous', 'jifa.gclog.generation.humongousHint']
    case "Last ditch collection":
      return 'jifa.gclog.cause.lastDitch'
    case "Promotion Failed":
      return 'jifa.gclog.cause.promotionFail'
    case "To-space Exhausted":
      return 'jifa.gclog.cause.toSpaceExhausted'
    case "Proactive":
      return 'jifa.gclog.cause.proactive'
    case "Allocation Rate":
      return 'jifa.gclog.cause.allocationRate'
    case "Timer":
      return 'jifa.gclog.cause.timer'
    case "Allocation Stall":
      return 'jifa.gclog.cause.allocationStall'
    case "High Usage":
      return 'jifa.gclog.cause.highUsage'
    case "Warmup":
      return 'jifa.gclog.cause.warmup'
    case "Metadata GC Clear Soft References":
      return 'jifa.gclog.cause.metaspaceClearSoftRef'
    case "G1 Periodic Collection":
      return 'jifa.gclog.cause.g1Periodic'
    case "Diagnostic Command":
      return 'jifa.gclog.cause.dcmd'
    case "G1 Compaction Pause":
      return 'jifa.gclog.cause.g1Compaction'
    case "G1 Preventive Collection":
      return 'jifa.gclog.cause.g1Preventive'

    // other causes are not supported by jifa for the time being, or used for debugging or testing gc,
    // or never printed in gc log

    // case "Allocation Failure During Evacuation":
    // case "Stopping VM":
    // case "Concurrent GC":
    // case "Upgrade To Full GC":
    // case "FullGCAlot":
    // case "ScavengeAlot":
    // case "Allocation Profiler":
    // case "WhiteBox Initiated Young GC":
    // case "WhiteBox Initiated Concurrent Mark":
    // case "Update Allocation Context Stats":
    // case "No GC":
    // case "Tenured Generation Full":
    // case "CMS Generation Full":
    // case "CMS Initial Mark":
    // case "CMS Concurrent Mark":
    // case "Old Generation Expanded On Last Scavenge":
    // case "Old Generation Too Full To Scavenge":
    // case "ILLEGAL VALUE - last gc cause - ILLEGAL VALUE":
    // case "WhiteBox Initiated Full GC":
    // case "unknown GCCause":
    default:
      return ""
  }
}

export function badPhase(phase, component) {
  const bad = ['Full GC', 'Concurrent Mark Abort', 'Concurrent Mode Failure', "Concurrent Mode Interrupted",
    "Allocation Stall", "Concurrent Mark Reset For Overflow", "Out Of Memory"].indexOf(phase) >= 0
  return {
    bad: bad,
    badHint: component.$t('jifa.gclog.badHint.badPhase', {name: phase})
  }
}

export function badCause(phase, cause, component) {
  if (["To-space Exhausted", "Allocation Stall"].indexOf(cause) >= 0) {
    return {
      bad: true,
      badHint: component.$t('jifa.gclog.badHint.badCause', {name: cause})
    }
  }
  if (phase === "Full GC" && ["Allocation Failure", "Metadata GC Threshold", "Ergonomics", "Last ditch collection",
    "Promotion Failed", "Metadata GC Clear Soft References", "System.gc()", "G1 Humongous Allocation",
    "GCLocker Initiated GC"].indexOf(cause) >= 0) {
    return {
      bad: true,
      badHint: component.$t('jifa.gclog.badHint.badCauseFull', {name: cause})
    }
  }
  return {bad: false}
}

export function isYoungGC(phase) {
  return phase === "Young GC" || phase === "Mixed GC"
}

export function isOldGC(phase) {
  return phase === "Concurrent Cycle" || phase === "CMS"
}

export function isFullGC(phase) {
  return phase === "Full GC" || phase === "Garbage Collection"
}

export function isPause(phase, metadata) {
  return metadata.pauseEventTypes.indexOf(phase) >= 0
}

export function hasOldGC(metadata) {
  return metadata.collector === 'CMS GC' || metadata.collector === 'G1 GC'
}

export function badIntervalThreshold(phase, config) {
  if (isYoungGC(phase)) {
    return config.youngGCFrequentIntervalThreshold
  } else if (isOldGC(phase)) {
    return config.oldGCFrequentIntervalThreshold
  } else if (isFullGC(phase)) {
    return config.fullGCFrequentIntervalThreshold
  } else {
    return -1;// don't check its interval
  }
}

export function badDurationThreshold(phase, config, metadata) {
  if (isPause(phase, metadata)) {
    return config.longPauseThreshold
  } else {
    return config.longConcurrentThreshold
  }
}

export function hasConcurrentGCThreads(metadata) {
  return ['CMS GC', 'G1 GC', 'ZGC'].indexOf(metadata.collector) >= 0
}

export function hasParallelGCThreads(metadata) {
  return metadata.collector !== 'Serial GC'
}

export function formatSize(bytes) {
  if (bytes < 0) {
    return "N/A"
  }
  return toReadableSizeWithUnit(bytes)
}

export function formatSizeSpeed(bytesPerMs) {
  if (bytesPerMs < 0) {
    return "N/A"
  }
  return toReadableSizeWithUnit(bytesPerMs * 1000) + "/s"
}

// e.g 0.1234 -> '12.34%'
export function formatPercentage(percent) {
  if (percent < 0) {
    return "N/A"
  }
  return (percent * 100).toFixed(2) + '%'
}

// e.g 100,000 (ms) -> '1.67min'
export function formatTimePeriod(time) {
  // negative means not available
  if (time < 0 || typeof time === "undefined") {
    return "N/A"
  }
  if (time < 0.001) {
    return "0"
  }
  if (time < 1) {
    return `${time.toFixed(3)}ms`
  }
  const units = ["ms", "s", "min", "h"]
  const gap = [1000, 60, 60]
  let i;
  for (i = 0; i < 3; i++) {
    if (time > gap[i]) {
      time /= gap[i]
    } else {
      break
    }
  }
  return (time >= 1000 ? Math.round(time) : time.toPrecision(3)) + " " + units[i];
}

export function formatTimeRange(start, end, timestamp) {
  if (timestamp >= 0) {
    const time1 = new Date(start + timestamp)
    const time2 = new Date(end + timestamp)
    const format1 = 'Y-M-D h:m:s'
    let format2;
    if (time1.getFullYear() !== time2.getFullYear()) {
      format2 = 'Y-M-D h:m:s'
    } else if (time1.toDateString() !== time2.toDateString()) {
      format2 = 'M-D h:m:s'
    } else {
      format2 = 'h:m:s'
    }
    return formatTime(start + timestamp, format1) + ' ~ '
      + formatTime(end + timestamp, format2)
  } else {
    return `${Math.floor(start / 1000)} ~ ${Math.ceil(end / 1000)} s`
  }
}

export function getUrlParams(url) {
  let params = {}
  const question = url.indexOf('?')
  if (question >= 0) {
    let str = url.substr(question + 1);
    str.split("&").forEach(paramString => {
      const items = paramString.split("=");
      params[decodeURIComponent(items[0])] = decodeURIComponent(items[1]);
    })
  }
  return params;
}

export function uppercaseFirstLetter(string) {
  return string[0].toUpperCase() + string.slice(1)
}