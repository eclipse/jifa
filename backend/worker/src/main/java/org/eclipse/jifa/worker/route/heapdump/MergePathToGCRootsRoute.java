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

import java.util.Collections;
import java.util.List;

import org.eclipse.jifa.common.aux.JifaException;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.util.PageViewBuilder;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.jifa.worker.vo.heapdump.HeapObject;
import org.eclipse.jifa.worker.vo.heapdump.gcrootpath.MergePathToGCRootsTreeNode;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.internal.snapshot.inspections.MultiplePath2GCRootsQuery;
import org.eclipse.mat.parser.model.ClassImpl;
import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;

import io.vertx.core.Future;

class MergePathToGCRootsRoute extends HeapBaseRoute {

    @RouteMeta(path = "/mergePathToGCRoots/roots/byClassId")
    void rootsByClassId(Future<PageView<MergePathToGCRootsTreeNode>> future, @ParamKey("file") String file,
            @ParamKey("classId") int classId, @ParamKey("grouping") MultiplePath2GCRootsQuery.Grouping grouping,
            PagingRequest pagingRequest) throws Exception {
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        MultiplePath2GCRootsQuery.Tree tree = buildMultiplePath2GCRootsTreeByClassId(snapshot, classId, grouping);
        future.complete(buildRecords(snapshot, tree, pagingRequest, tree.getElements()));
    }

    @RouteMeta(path = "/mergePathToGCRoots/children/byClassId")
    void childrenByClassId(Future<PageView<MergePathToGCRootsTreeNode>> future, @ParamKey("file") String file,
            @ParamKey("grouping") MultiplePath2GCRootsQuery.Grouping grouping, PagingRequest pagingRequest,
            @ParamKey("classId") int classId, @ParamKey("objectIdPathInGCPathTree") int[] objectIdPathInGCPathTree)
            throws Exception {
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        MultiplePath2GCRootsQuery.Tree tree = buildMultiplePath2GCRootsTreeByClassId(snapshot, classId, grouping);
        Object object = HeapDumpSupport.fetchObjectInResultTree(tree, objectIdPathInGCPathTree);
        List<?> elements = object == null ? Collections.emptyList() : tree.getChildren(object);
        future.complete(buildRecords(snapshot, tree, pagingRequest, elements));
    }

    private MultiplePath2GCRootsQuery.Tree buildMultiplePath2GCRootsTreeByClassId(ISnapshot snapshot, int classId,
            MultiplePath2GCRootsQuery.Grouping grouping)
            throws Exception {
        if (grouping != MultiplePath2GCRootsQuery.Grouping.FROM_GC_ROOTS) {
            throw new JifaException("unsupported grouping now.");
        }

        ClassImpl clazz = (ClassImpl) snapshot.getObject(classId);

        MultiplePath2GCRootsQuery multiplePath2GCRootsQuery = new MultiplePath2GCRootsQuery();
        multiplePath2GCRootsQuery.snapshot = snapshot;
        multiplePath2GCRootsQuery.groupBy = grouping;
        multiplePath2GCRootsQuery.objects = HeapDumpSupport.buildHeapObjectArgument(clazz.getObjectIds());

        return (MultiplePath2GCRootsQuery.Tree) multiplePath2GCRootsQuery.execute(HeapDumpSupport.VOID_LISTENER);
    }

    private PageView<MergePathToGCRootsTreeNode> buildRecords(ISnapshot snapshot,
            MultiplePath2GCRootsQuery.Tree tree,
            PagingRequest pagingRequest, List<?> elements) {
        return PageViewBuilder.build(elements, pagingRequest, element -> {
            try {
                MergePathToGCRootsTreeNode record = new MergePathToGCRootsTreeNode();
                int objectId = tree.getContext(element).getObjectId();
                IObject object = snapshot.getObject(objectId);
                record.setObjectId(tree.getContext(element).getObjectId());
                record.setObjectType(HeapObject.Type.typeOf(object));
                record.setGCRoot(snapshot.isGCRoot(objectId));
                record.setClassName(tree.getColumnValue(element, 0).toString());
                record.setSuffix(HeapDumpSupport.suffix(object.getGCRootInfo()));
                record.setRefObjects((int) tree.getColumnValue(element, 1));
                record.setShallowHeap(((Bytes) tree.getColumnValue(element, 2)).getValue());
                record.setRefShallowHeap(((Bytes) tree.getColumnValue(element, 3)).getValue());
                record.setRetainedHeap(((Bytes) tree.getColumnValue(element, 4)).getValue());
                return record;
            } catch (SnapshotException ex) {
                throw new JifaException(ex);
            }
        });
    }

}
