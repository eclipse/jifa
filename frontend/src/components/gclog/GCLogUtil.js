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


import {formatTime} from "@/util";

export function getPhaseHint(phase) {
  // todo
  return ""
}

export function getCauseHint(cause) {
  switch (cause) {
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
      return 'jifa.gclog.cause.humongous'
    case "Last ditch collection":
      return 'jifa.gclog.cause.lastDitch'
    case "Promotion failed":
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
    // case "CMS Final Remark":
    // case "CMS Concurrent Mark":
    // case "Old Generation Expanded On Last Scavenge":
    // case "Old Generation Too Full To Scavenge":
    // case "ILLEGAL VALUE - last gc cause - ILLEGAL VALUE":
    // case "WhiteBox Initiated Full GC":
    default:
      return ""
  }
}

export function badPhase(phase) {
  return ['Full GC', 'Concurrent Mark Abort', 'Concurrent Mode Failure',
    "Allocation Stall", "Concurrent Mark Reset For Overflow"].indexOf(phase) >= 0
}

export function badCause(phase, cause) {
  if (["To-space Exhausted", "Allocation Stall"].indexOf(cause) >= 0) {
    return true
  }
  if (phase === "Full GC" && ["Allocation Failure", "Metadata GC Threshold", "Ergonomics", "Last ditch collection",
    "Promotion failed", "Metadata GC Clear Soft References", "System.gc()"].indexOf(cause) >= 0) {
    return true;
  }
  return false
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

export function hasOldGC(gc) {
  return gc === 'CMS GC' || gc === 'G1 GC'
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

export function formatTimeRange(start, end, timestamp) {
  if (timestamp > 0) {
    return formatTime(start + timestamp, 'Y-M-D h:m:s') + ' ~ '
      + formatTime(end + timestamp, 'Y-M-D h:m:s')
  } else {
    return `${Math.floor(start / 1000)} ~ ${Math.ceil(end / 1000)} s`
  }
}