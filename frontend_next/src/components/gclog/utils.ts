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

import { prettySize, prettyTime } from '@/support/utils';

export function getPhaseHint(phase: string) {
  switch (phase) {
    case 'Full GC':
      return 'phase.fullGC';
    case 'Concurrent Mark Abort':
      return 'phase.concMarkAbort';
    case 'Concurrent Mode Interrupted':
      return 'phase.concModeInterrupt';
    case 'Concurrent Mode Failure':
      return 'phase.concModeFailure';
    case 'Allocation Stall':
      return 'phase.allocationStall';
    case 'Concurrent Mark Reset For Overflow':
      return 'phase.cmReset';
    case 'Out Of Memory':
      return 'phase.oom';
    case 'Initial Mark Situation':
      return 'phase.initialMarkSituation';
    case 'Prepare Mixed Situation':
      return 'phase.prepareMixedSituation';
    // todo: add more phases
    default:
      return '';
  }
}

export function getCauseHint(cause: string) {
  switch (cause) {
    case 'Full GC for -Xshare:dump':
      return 'cause.archiveShare';
    case 'CMS Final Remark':
      return 'cause.cmsFinalRemark';
    case 'System.gc()':
      return 'cause.systemgc';
    case 'JvmtiEnv ForceGarbageCollection':
      return 'cause.jvmti';
    case 'GCLocker Initiated GC':
      return 'cause.gclocker';
    case 'Heap Inspection Initiated GC':
      return 'cause.heapInspection';
    case 'Heap Dump Initiated GC':
      return 'cause.heapDump';
    case 'Allocation Failure':
      return 'cause.allocationFail';
    case 'Metadata GC Threshold':
      return 'cause.metaspace';
    case 'Ergonomics':
      return 'cause.ergonomics';
    case 'G1 Evacuation Pause':
      return 'cause.g1Evacuation';
    case 'G1 Humongous Allocation':
      return ['cause.humongous', 'generation.humongousHint'];
    case 'Last ditch collection':
      return 'cause.lastDitch';
    case 'Promotion Failed':
      return 'cause.promotionFail';
    case 'To-space Exhausted':
      return 'cause.toSpaceExhausted';
    case 'Proactive':
      return 'cause.proactive';
    case 'Allocation Rate':
      return 'cause.allocationRate';
    case 'Timer':
      return 'cause.timer';
    case 'Allocation Stall':
      return 'cause.allocationStall';
    case 'High Usage':
      return 'cause.highUsage';
    case 'Warmup':
      return 'cause.warmup';
    case 'Metadata GC Clear Soft References':
      return 'cause.metaspaceClearSoftRef';
    case 'G1 Periodic Collection':
      return 'cause.g1Periodic';
    case 'Diagnostic Command':
      return 'cause.dcmd';
    case 'G1 Compaction Pause':
      return 'cause.g1Compaction';
    case 'G1 Preventive Collection':
      return 'cause.g1Preventive';

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
      return '';
  }
}

export function badPhase(phase: string) {
  const bad =
    [
      'Full GC',
      'Concurrent Mark Abort',
      'Concurrent Mode Failure',
      'Concurrent Mode Interrupted',
      'Allocation Stall',
      'Concurrent Mark Reset For Overflow',
      'Out Of Memory',
      'Evacuation Failure'
    ].indexOf(phase) >= 0;
  return {
    bad: bad,
    badHint: 'badHint.badPhase',
    badHintArgs: { name: phase }
  };
}

export function badCause(phase: string, cause: string) {
  if (['To-space Exhausted', 'Allocation Stall'].indexOf(cause) >= 0) {
    return {
      bad: true,
      badHint: 'badHint.badCause',
      badHintArgs: { name: cause }
    };
  }
  if (
    phase === 'Full GC' &&
    [
      'Allocation Failure',
      'Metadata GC Threshold',
      'Ergonomics',
      'Last ditch collection',
      'Promotion Failed',
      'Metadata GC Clear Soft References',
      'System.gc()',
      'G1 Humongous Allocation',
      'GCLocker Initiated GC',
      'G1 Compaction Pause'
    ].indexOf(cause) >= 0
  ) {
    return {
      bad: true,
      badHint: 'badHint.badCauseFull',
      badHintArgs: { name: cause }
    };
  }
  return { bad: false };
}

export function isYoungGC(phase: string) {
  return phase === 'Young GC' || phase === 'Mixed GC';
}

export function isOldGC(phase: string) {
  return phase === 'Concurrent Mark Cycle' || phase === 'CMS';
}

export function isFullGC(phase: string) {
  return phase === 'Full GC' || phase === 'Garbage Collection';
}

export function isPause(phase: string, metadata: any) {
  return metadata.pauseEventTypes.indexOf(phase) >= 0;
}

export function hasOldGC(metadata: any) {
  return metadata.collector === 'CMS GC' || metadata.collector === 'G1 GC';
}

export function badIntervalThreshold(phase: string, config: any) {
  if (isYoungGC(phase)) {
    return config.youngGCFrequentIntervalThreshold;
  } else if (isOldGC(phase)) {
    return config.oldGCFrequentIntervalThreshold;
  } else if (isFullGC(phase)) {
    return config.fullGCFrequentIntervalThreshold;
  } else {
    return -1; // don't check its interval
  }
}

export function badDurationThreshold(phase: string, config: any, metadata: any) {
  if (isPause(phase, metadata)) {
    return config.longPauseThreshold;
  } else {
    return config.longConcurrentThreshold;
  }
}

export function hasConcurrentGCThreads(metadata: any) {
  return ['CMS GC', 'G1 GC', 'ZGC'].indexOf(metadata.collector) >= 0;
}

export function hasParallelGCThreads(metadata: any) {
  return metadata.collector !== 'Serial GC';
}

export function formatSize(bytes: number) {
  if (bytes < 0) {
    return 'N/A';
  }
  return prettySize(bytes);
}

export function formatSizeSpeed(bytesPerMs: number) {
  if (bytesPerMs < 0) {
    return 'N/A';
  }
  return prettySize(bytesPerMs * 1000) + '/s';
}

// e.g 0.1234 -> '12.34%'
export function formatPercentage(percent: number) {
  if (percent < 0) {
    return 'N/A';
  }
  return (percent * 100).toFixed(2) + '%';
}

// e.g 100,000 (ms) -> '1.67min'
export function formatTimePeriod(time: number) {
  // negative means not available
  if (time < 0 || typeof time === 'undefined') {
    return 'N/A';
  }
  if (time < 0.001) {
    return '0';
  }
  if (time < 1) {
    return `${time.toFixed(3)}ms`;
  }
  const units = ['ms', 's', 'min', 'h'];
  const gap = [1000, 60, 60];
  let i;
  for (i = 0; i < 3; i++) {
    if (time > gap[i]) {
      time /= gap[i];
    } else {
      break;
    }
  }
  return (time >= 1000 ? Math.round(time) : time.toPrecision(3)) + ' ' + units[i];
}

export function formatTimeRange(start: number, end: number, timestamp: number) {
  if (timestamp >= 0) {
    const time1 = new Date(start + timestamp);
    const time2 = new Date(end + timestamp);
    const format1 = 'Y-M-D h:m:s';
    let format2;
    if (time1.getFullYear() !== time2.getFullYear()) {
      format2 = 'Y-M-D h:m:s';
    } else if (time1.toDateString() !== time2.toDateString()) {
      format2 = 'M-D h:m:s';
    } else {
      format2 = 'h:m:s';
    }
    return prettyTime(start + timestamp, format1) + ' ~ ' + prettyTime(end + timestamp, format2);
  } else {
    return `${Math.floor(start / 1000)} ~ ${Math.ceil(end / 1000)} s`;
  }
}

export function getUrlParams(url: string) {
  let params = {};
  const question = url.indexOf('?');
  if (question >= 0) {
    let str = url.substr(question + 1);
    str.split('&').forEach((paramString) => {
      const items = paramString.split('=');
      params[decodeURIComponent(items[0])] = decodeURIComponent(items[1]);
    });
  }
  return params;
}

export function uppercaseFirstLetter(string: string) {
  return string[0].toUpperCase() + string.slice(1);
}

export const COLORS = [
  '#ECA943',
  '#42436C',
  '#238E23',
  '#8F8FBD',
  '#70DB93',
  '#5C3317',
  '#9F5F9F',
  '#5F9F9F',
  '#FF7F00',
  '#9932CD',
  '#2F4F4F',
  '#7093DB',
  '#D19275',
  '#8E2323',
  '#CD7F32',
  '#DBDB70',
  '#527F76',
  '#93DB70',
  '#215E21',
  '#4E2F2F',
  '#9F9F5F',
  '#C0D9D9'
];

export function getIthColor(i: number) {
  return COLORS[i % COLORS.length];
}
