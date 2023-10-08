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
import { emit, EventType } from '@/components/heapdump/event-bus';
import { hdt } from '@/components/heapdump/utils';
import { item, menu, subMenu } from '@/composables/contextmenu';

function title(key: string) {
  return () => hdt(`contextmenu.${key}`);
}

const referencesByObject_outbounds = item(title('outbounds'), (payload) =>
  emit(EventType.OBJECT_OUTBOUNDS, payload)
);

const referencesByObject_inbounds = item(title('inbounds'), (payload) =>
  emit(EventType.OBJECT_INBOUNDS, payload)
);

const referencesByClass_outbounds = item(title('outbounds'), (payload) =>
  emit(EventType.CLASS_OUTBOUNDS, payload)
);

const referencesByClass_inbounds = item(title('inbounds'), (payload) =>
  emit(EventType.CLASS_INBOUNDS, payload)
);

const pathToGCRoots = item(title('pathToGCRoots'), (payload) =>
  emit(EventType.PATH_TO_GC_ROOTS, payload)
);

const mergedPathToGCRoots = item(title('mergedPathToGCRoots'), (payload) =>
  emit(EventType.MERGED_PATH_TO_GC_ROOTS, payload)
);

export const commonMenu = menu([
  subMenu(title('referencesByObject'), [referencesByObject_outbounds, referencesByObject_inbounds]),
  subMenu(
    title('referencesByClass'),
    [referencesByClass_outbounds, referencesByClass_inbounds],
    true
  ),
  pathToGCRoots,
  mergedPathToGCRoots
]);
