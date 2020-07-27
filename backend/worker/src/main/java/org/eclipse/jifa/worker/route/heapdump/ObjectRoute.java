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
package org.eclipse.jifa.worker.route.heapdump;

import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.jifa.worker.vo.heapdump.HeapObject;
import io.vertx.core.Future;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class ObjectRoute extends HeapBaseRoute {

    @RouteMeta(path = "/object")
    void info(Future<HeapObject> future, @ParamKey("file") String file,
              @ParamKey("objectId") int objectId) throws Exception {

        ASSERT.isTrue(objectId >= 0, "Object id must be greater than or equal to 0");
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();

        HeapObject o = new HeapObject();
        IObject object = snapshot.getObject(objectId);
        o.setObjectId(objectId);
        o.setLabel(object.getDisplayName());
        o.setShallowSize(object.getUsedHeapSize());
        o.setRetainedSize(object.getRetainedHeapSize());
        o.setObjectType(HeapObject.Type.typeOf(object));
        o.setGCRoot(snapshot.isGCRoot(objectId));
        o.setHasOutbound(true);
        o.setSuffix(HeapDumpSupport.suffix(snapshot, objectId));
        future.complete(o);
    }
}
