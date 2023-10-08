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
import mitt from 'mitt';

const emitter = mitt();

export enum EventType {
  OBJECT_OUTBOUNDS,
  OBJECT_INBOUNDS,
  CLASS_OUTBOUNDS,
  CLASS_INBOUNDS,

  PATH_TO_GC_ROOTS,
  MERGED_PATH_TO_GC_ROOTS
}

export function emit(event: EventType, payload: any) {
  emitter.emit(EventType[event], payload);
}

export function listen(event: EventType, handler: any) {
  emitter.on(EventType[event], handler);
}

export function listenAll(handler: any) {
  emitter.on('*', handler);
}
