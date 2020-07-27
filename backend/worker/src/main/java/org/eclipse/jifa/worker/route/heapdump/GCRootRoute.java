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

import org.eclipse.jifa.common.aux.JifaException;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.util.PageViewBuilder;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.jifa.worker.vo.heapdump.gcroot.Result;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.vo.heapdump.HeapObject;
import io.vertx.core.Future;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.inspections.GCRootsQuery;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;

import java.util.List;
import java.util.stream.Collectors;

class GCRootRoute extends HeapBaseRoute {

    @RouteMeta(path = "/GCRoots")
    void roots(Future<List<Result>> future, @ParamKey("file") String file) throws Exception {
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        GCRootsQuery query = new GCRootsQuery();
        query.snapshot = snapshot;

        IResultTree tree = (IResultTree) query.execute(HeapDumpSupport.VOID_LISTENER);
        future.complete(tree.getElements().stream().map(e -> {
            Result r = new Result();
            r.setClassName((String) tree.getColumnValue(e, 0));
            r.setObjects((Integer) tree.getColumnValue(e, 1));
            return r;
        }).collect(Collectors.toList()));
    }

    @RouteMeta(path = "/GCRoots/classes")
    void classes(Future<PageView<Result>> future, @ParamKey("file") String file,
                 @ParamKey("rootTypeIndex") int rootTypeIndex, PagingRequest pagingRequest) throws Exception {

        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        GCRootsQuery query = new GCRootsQuery();
        query.snapshot = snapshot;

        IResultTree tree = (IResultTree) query.execute(HeapDumpSupport.VOID_LISTENER);
        Object root = tree.getElements().get(rootTypeIndex);
        List<?> classes = tree.getChildren(root);
        future.complete(PageViewBuilder.build(classes, pagingRequest, clazz -> {
            Result r = new Result();
            r.setClassName((String) tree.getColumnValue(clazz, 0));
            r.setObjects((Integer) tree.getColumnValue(clazz, 1));
            r.setObjectId(tree.getContext(clazz).getObjectId());
            return r;
        }));
    }

    @RouteMeta(path = "/GCRoots/class/objects")
    void objects(Future<PageView<HeapObject>> future, @ParamKey("file") String file,
                 @ParamKey("rootTypeIndex") int rootTypeIndex, @ParamKey("classIndex") int classIndex,
                 PagingRequest pagingRequest) throws Exception {

        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        GCRootsQuery query = new GCRootsQuery();
        query.snapshot = snapshot;

        IResultTree tree = (IResultTree) query.execute(HeapDumpSupport.VOID_LISTENER);
        Object root = tree.getElements().get(rootTypeIndex);
        List<?> classes = tree.getChildren(root);
        Object clazz = classes.get(classIndex);
        List<?> objects = tree.getChildren(clazz);
        future.complete(PageViewBuilder.build(objects, pagingRequest, o -> {
            try {
                HeapObject ho = new HeapObject();
                int objectId = tree.getContext(o).getObjectId();
                IObject object = snapshot.getObject(objectId);
                ho.setLabel(object.getDisplayName());
                ho.setObjectId(objectId);
                ho.setShallowSize(object.getUsedHeapSize());
                ho.setRetainedSize(object.getRetainedHeapSize());
                ho.setObjectType(HeapObject.Type.typeOf(object));
                ho.setGCRoot(snapshot.isGCRoot(objectId));
                ho.setSuffix(HeapDumpSupport.suffix(object.getGCRootInfo()));
                ho.setHasOutbound(true);
                ho.setHasInbound(true);
                return ho;
            } catch (SnapshotException e) {
                throw new JifaException(e);
            }
        }));
    }
}
