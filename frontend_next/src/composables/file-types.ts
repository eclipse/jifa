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
import GCLogView from '@/components/gclog/GCLogView.vue';
import HeapDump from '@/components/heapdump/HeapDump.vue';
import HeapDumpSetup from '@/components/heapdump/Setup.vue';
import ThreadDumpView from '@/components/threaddump/ThreadDumpView.vue';

export const fileTypeMap = new Map<string, FileType>();

export class FileType {
  constructor(
    public readonly key: string,
    public readonly labelKey: string,
    public readonly namespace: string,
    public readonly routePath: string,
    public readonly setupComponent: object | null,
    public readonly analysisComponent: object
  ) {
    fileTypeMap.set(key, this);
  }
}

function def(
  key: string,
  labelKey: string,
  namespace: string,
  setupView: object | null,
  analysisView: object
) {
  return new FileType(key, labelKey, namespace, namespace + '-analysis', setupView, analysisView);
}

def('HEAP_DUMP', 'heapDump', 'heap-dump', HeapDumpSetup, HeapDump);

def('GC_LOG', 'GCLog', 'gc-log', null, GCLogView);

def('THREAD_DUMP', 'threadDump', 'thread-dump', null, ThreadDumpView);
