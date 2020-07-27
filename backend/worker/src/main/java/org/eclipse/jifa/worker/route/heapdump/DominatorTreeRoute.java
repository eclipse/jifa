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
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.util.PageViewBuilder;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.jifa.worker.vo.heapdump.HeapObject;
import org.eclipse.jifa.worker.vo.heapdump.dominatortree.BaseRecord;
import org.eclipse.jifa.worker.vo.heapdump.dominatortree.ClassRecord;
import org.eclipse.jifa.worker.vo.heapdump.dominatortree.DefaultRecord;
import io.vertx.core.Future;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.internal.snapshot.inspections.DominatorQuery;
import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class DominatorTreeRoute extends HeapBaseRoute {

    private static Constructor<?> DEFAULT_NODE;

    static {
        try {
            Class<?> clazz = Class.forName("org.eclipse.mat.internal.snapshot.inspections.DominatorQuery$Node");
            DEFAULT_NODE = clazz.getConstructor(int.class);
            DEFAULT_NODE.setAccessible(true);
            ASSERT.notNull(DEFAULT_NODE);

        } catch (Exception e) {
            throw new JifaException(e);
        }
    }

    @RouteMeta(path = "/dominatorTree/roots")
    void roots(Future<PageView<BaseRecord>> future, @ParamKey("file") String file,
               @ParamKey("grouping") Grouping grouping, PagingRequest pagingRequest) throws Exception {
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        DominatorQuery query = new DominatorQuery();
        query.snapshot = snapshot;
        query.groupBy = grouping.internalGroping;
        DominatorQuery.Tree tree = query.execute(HeapDumpSupport.VOID_LISTENER);

        if (grouping == Grouping.NONE) {
            future.complete(buildDefaultRecord(snapshot, tree, tree.getElements(), pagingRequest));
        } else if (grouping == Grouping.BY_CLASS) {
            future.complete(buildClassRecord(tree, tree.getElements(), pagingRequest));
        }
    }

    @RouteMeta(path = "/dominatorTree/children")
    void children(Future<PageView<BaseRecord>> future, @ParamKey("file") String file,
                  @ParamKey("grouping") Grouping grouping, PagingRequest pagingRequest,
                  @ParamKey("parentObjectId") int parentObjectId) throws Exception {

        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        DominatorQuery query = new DominatorQuery();
        query.snapshot = snapshot;
        query.groupBy = grouping.internalGroping;
        DominatorQuery.Tree tree = query.execute(HeapDumpSupport.VOID_LISTENER);

        if (grouping == Grouping.NONE) {
            Object parentNode = DEFAULT_NODE.newInstance(parentObjectId);
            future.complete(buildDefaultRecord(snapshot, tree, tree.getChildren(parentNode), pagingRequest));
        } else if (grouping == Grouping.BY_CLASS) {

        }
    }

    private PageView<BaseRecord> buildDefaultRecord(ISnapshot snapshot, DominatorQuery.Tree tree, List<?> elements,
                                                    PagingRequest pagingRequest) {

        return PageViewBuilder.build(elements, pagingRequest, e -> {
            try {
                DefaultRecord record = new DefaultRecord();
                int objectId = tree.getContext(e).getObjectId();
                IObject object = snapshot.getObject(objectId);
                record.setObjectId(objectId);
                record.setSuffix(HeapDumpSupport.suffix(object.getGCRootInfo()));
                record.setObjectType(HeapObject.Type.typeOf(object));
                record.setGCRoot(snapshot.isGCRoot(objectId));
                record.setLabel((String) tree.getColumnValue(e, 0));
                record.setShallowSize(((Bytes) tree.getColumnValue(e, 1)).getValue());
                record.setRetainedSize(((Bytes) tree.getColumnValue(e, 2)).getValue());
                record.setPercent((Double) tree.getColumnValue(e, 3));
                return record;
            } catch (SnapshotException ex) {
                throw new JifaException(ex);
            }
        });
    }

    private PageView<BaseRecord> buildClassRecord(DominatorQuery.Tree tree, List<?> elements,
                                                  PagingRequest pagingRequest) {

        elements.sort((e1, e2) -> ((Bytes) tree.getColumnValue(e2, 3)).compareTo(tree.getColumnValue(e1, 3)));

        return PageViewBuilder.build(elements, pagingRequest, e -> {
            ClassRecord record = new ClassRecord();
            int objectId = tree.getContext(e).getObjectId();
            record.setObjectId(objectId);
            record.setLabel((String) tree.getColumnValue(e, 0));
            record.setObjects((Integer) tree.getColumnValue(e, 1));
            record.setShallowSize(((Bytes) tree.getColumnValue(e, 2)).getValue());
            record.setRetainedSize(((Bytes) tree.getColumnValue(e, 3)).getValue());
            record.setPercent((Double) tree.getColumnValue(e, 4));
            return record;
        });
    }

    public enum Grouping {

        NONE(DominatorQuery.Grouping.NONE),

        BY_CLASS(DominatorQuery.Grouping.BY_CLASS),

        BY_CLASSLOADER(DominatorQuery.Grouping.BY_CLASSLOADER),

        BY_PACKAGE(DominatorQuery.Grouping.BY_PACKAGE);

        DominatorQuery.Grouping internalGroping;

        Grouping(DominatorQuery.Grouping internalGroping) {
            this.internalGroping = internalGroping;
        }
    }
}
