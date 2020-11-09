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

import io.vertx.core.Future;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.util.PageViewBuilder;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.jifa.worker.vo.heapdump.HeapObject;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class ObjectListRoute extends HeapBaseRoute {

    private static void fill(Future<PageView<HeapObject>> future, ISnapshot snapshot, int src, int[] objId,
                             PagingRequest pagingRequest, boolean outbound) {
        future.complete(PageViewBuilder.build(objId, pagingRequest, id -> {
            try {
                HeapObject o = new HeapObject();
                IObject object = snapshot.getObject(id);
                o.setObjectId(id);
                o.setLabel(object.getDisplayName());
                o.setShallowSize(object.getUsedHeapSize());
                o.setRetainedSize(object.getRetainedHeapSize());
                o.setObjectType(HeapObject.Type.typeOf(object));
                o.setGCRoot(snapshot.isGCRoot(id));
                o.setHasOutbound(true);
                o.setHasInbound(true);
                o.setPrefix(HeapDumpSupport.prefix(snapshot, outbound ? src : id, outbound ? id : src));
                o.setSuffix(HeapDumpSupport.suffix(snapshot, id));
                return o;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    @RouteMeta(path = "/outbounds")
    void outbounds(Future<PageView<HeapObject>> future, @ParamKey("file") String file, PagingRequest pagingRequest,
                   @ParamKey("objectId") int objectId) throws Exception {

        ASSERT.isTrue(objectId >= 0, "Object id must be greater than or equal to 0");

        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        int[] objId = snapshot.getOutboundReferentIds(objectId);
        fill(future, snapshot, objectId, objId, pagingRequest, true);
    }

    @RouteMeta(path = "/inbounds")
    void inbounds(Future<PageView<HeapObject>> future, @ParamKey("file") String file, PagingRequest pagingRequest,
                  @ParamKey("objectId") int objectId) throws Exception {

        ASSERT.isTrue(objectId >= 0, "Object id must be greater than or equal to 0");

        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        int[] objId = snapshot.getInboundRefererIds(objectId);
        fill(future, snapshot, objectId, objId, pagingRequest, false);
    }
}
