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
import GCLog from '@/components/gclog/GCLog.vue';
import GCLogToolBar from '@/components/gclog/Toolbar.vue';

import HeapDump from '@/components/heapdump/HeapDump.vue';
import HeapDumpSetup from '@/components/heapdump/Setup.vue';
import HeapDumpToolBar from '@/components/heapdump/Toolbar.vue';

import ThreadDump from '@/components/threaddump/ThreadDump.vue';
import ThreadDumpToolBar from '@/components/threaddump/Toolbar.vue';

export const fileTypeMap = new Map<string, FileType>();

export class FileType {
  constructor(
    public readonly key: string,
    public readonly labelKey: string,
    public readonly namespace: string,
    public readonly routePath: string,
    public readonly toolBarComponent: object | null,
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
  toolBarComponent: object,
  setupComponent: object | null,
  analysisComponent: object
) {
  return new FileType(
    key,
    labelKey,
    namespace,
    namespace + '-analysis',
    toolBarComponent,
    setupComponent,
    analysisComponent
  );
}

export const HEAP_DUMP = def(
  'HEAP_DUMP',
  'heapDump',
  'heap-dump',
  HeapDumpToolBar,
  HeapDumpSetup,
  HeapDump
);

export const GC_LOG = def('GC_LOG', 'GCLog', 'gc-log', GCLogToolBar, null, GCLog);

export const THREAD_DUMP = def(
  'THREAD_DUMP',
  'threadDump',
  'thread-dump',
  ThreadDumpToolBar,
  null,
  ThreadDump
);
