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
import org.eclipse.jifa.common.aux.JifaException;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.util.PageViewBuilder;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.jifa.worker.vo.heapdump.classreference.Record;
import org.eclipse.mat.inspections.ClassReferrersQuery;
import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.query.IContextObjectSet;
import org.eclipse.mat.query.IIconProvider;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.query.Icons;

import java.net.URL;

class ClassReferenceRoute extends HeapBaseRoute {

    private static int getType(URL icon) {
        if (icon == Icons.CLASS_IN || icon == Icons.CLASS_OUT) {
            return ClassReferrersQuery.Type.NEW;
        } else if (icon == Icons.CLASS_IN_MIXED || icon == Icons.CLASS_OUT_MIXED) {
            return ClassReferrersQuery.Type.MIXED;
        } else if (icon == Icons.CLASS_IN_OLD || icon == Icons.CLASS_OUT_OLD) {
            return ClassReferrersQuery.Type.OLD_FAD;
        }
        throw new JifaException();
    }

    private static Record build(IResultTree result, Object row) {
        Record record = new Record();
        record.setLabel((String) result.getColumnValue(row, 0));
        record.setObjects((Integer) result.getColumnValue(row, 1));
        record.setShallowSize(((Bytes) result.getColumnValue(row, 2)).getValue());
        IContextObjectSet context = (IContextObjectSet) result.getContext(row);
        record.setObjectId(context.getObjectId());
        record.setObjectIds(context.getObjectIds());
        record.setType(getType(((IIconProvider) result).getIcon(row)));
        return record;

    }

    private void process(Future<Object> future, String file, int objectId, boolean inbound) throws Exception {
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        ClassReferrersQuery query = new ClassReferrersQuery();
        query.snapshot = snapshot;
        query.objects = HeapDumpSupport.buildHeapObjectArgument(new int[]{objectId});
        query.inbound = inbound;
        IResultTree result = (IResultTree) query.execute(HeapDumpSupport.VOID_LISTENER);

        Object node = result.getElements().get(0);
        future.complete(build(result, node));
    }

    private void process(Future<Object> future, String file, PagingRequest pagingRequest, int[] objectIds,
                         boolean inbound) throws Exception {
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        ClassReferrersQuery query = new ClassReferrersQuery();
        query.snapshot = snapshot;
        query.objects = HeapDumpSupport.buildHeapObjectArgument(objectIds);
        query.inbound = inbound;
        IResultTree result = (IResultTree) query.execute(HeapDumpSupport.VOID_LISTENER);

        Object node = result.getElements().get(0);
        future.complete(PageViewBuilder.build(result.getChildren(node), pagingRequest, e -> build(result, e)));
    }

    @RouteMeta(path = "/classReference/inbounds/class")
    void inboundsClassInfo(Future<Object> future, @ParamKey("file") String file,
                           @ParamKey("objectId") int objectId) throws Exception {

        process(future, file, objectId, true);
    }

    @RouteMeta(path = "/classReference/outbounds/class")
    void outboundsClassInfo(Future<Object> future, @ParamKey("file") String file,
                            @ParamKey("objectId") int objectId) throws Exception {

        process(future, file, objectId, false);
    }

    @RouteMeta(path = "/classReference/inbounds/children")
    void inboundsChildren(Future<Object> future, @ParamKey("file") String file, PagingRequest pagingRequest,
                          @ParamKey("objectIds") int[] objectIds) throws Exception {
        process(future, file, pagingRequest, objectIds, true);
    }

    @RouteMeta(path = "/classReference/outbounds/children")
    void outboundsChildren(Future<Object> future, @ParamKey("file") String file, PagingRequest pagingRequest,
                           @ParamKey("objectIds") int[] objectIds) throws Exception {
        process(future, file, pagingRequest, objectIds, false);
    }

}
