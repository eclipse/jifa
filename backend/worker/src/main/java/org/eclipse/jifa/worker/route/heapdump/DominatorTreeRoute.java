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
import org.eclipse.jifa.common.aux.ErrorCode;
import org.eclipse.jifa.common.aux.JifaException;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.common.util.PageViewBuilder;
import org.eclipse.jifa.common.util.ReflectionUtil;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.ExoticTreeFinder;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.jifa.worker.vo.heapdump.HeapObject;
import org.eclipse.jifa.worker.vo.heapdump.RecordType;
import org.eclipse.jifa.worker.vo.heapdump.dominatortree.*;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.internal.snapshot.inspections.DominatorQuery;
import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;
import static org.eclipse.jifa.common.util.ReflectionUtil.getFieldValue;

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
               @ParamKey("grouping") Grouping grouping, @ParamKey(value = "sortBy", mandatory = false) String sortBy,
               @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder, PagingRequest pagingRequest) throws Exception {
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        DominatorQuery query = new DominatorQuery();
        query.snapshot = snapshot;
        query.groupBy = grouping.internalGroping;
        DominatorQuery.Tree tree = query.execute(HeapDumpSupport.VOID_LISTENER);

        if (grouping == Grouping.NONE) {
            future.complete(buildDefaultRecord(snapshot, tree, tree.getElements(), ascendingOrder, sortBy, pagingRequest));
        } else if (grouping == Grouping.BY_CLASS) {
            future.complete(buildClassRecord(tree, tree.getElements(), ascendingOrder, sortBy, pagingRequest));
        } else if (grouping == Grouping.BY_CLASSLOADER) {
            future.complete(buildClassLoaderRecord(snapshot, tree, tree.getElements(), ascendingOrder, sortBy, pagingRequest));
        } else if (grouping == Grouping.BY_PACKAGE) {
            future.complete(buildPackageRecord(snapshot, tree, tree.getElements(), ascendingOrder, sortBy, pagingRequest));
        }
    }

    @RouteMeta(path = "/dominatorTree/children")
    void children(Future<PageView<BaseRecord>> future, @ParamKey("file") String file,
                  @ParamKey("grouping") Grouping grouping, @ParamKey(value = "sortBy", mandatory = false) String sortBy,
                  @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder, PagingRequest pagingRequest,
                  @ParamKey("parentObjectId") int parentObjectId) throws Exception {

        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        DominatorQuery query = new DominatorQuery();
        query.snapshot = snapshot;
        query.groupBy = grouping.internalGroping;
        DominatorQuery.Tree tree = query.execute(HeapDumpSupport.VOID_LISTENER);

        if (grouping == Grouping.NONE) {
            Object parentNode = DEFAULT_NODE.newInstance(parentObjectId);
            future.complete(buildDefaultRecord(snapshot, tree, tree.getChildren(parentNode), ascendingOrder, sortBy, pagingRequest));
        } else if (grouping == Grouping.BY_CLASS) {
            ErrorUtil.errorWith(ErrorCode.SHOULD_NOT_REACH_HERE, "item does not have children");
        } else if (grouping == Grouping.BY_CLASSLOADER) {
            List<?> children = new ExoticTreeFinder(tree)
                    .setGetChildrenCallback(tree::getChildren)
                    .setPredicate((theTree, theNode) -> theTree.getContext(theNode).getObjectId())
                    .findChildrenOf(parentObjectId);

            if (children != null) {
                future.complete(buildClassLoaderRecord(snapshot, tree, children, ascendingOrder, sortBy, pagingRequest));
            } else {
                future.complete(PageViewBuilder.build(new ArrayList<>(), pagingRequest));
            }
        } else if (grouping == Grouping.BY_PACKAGE) {
            Object targetParentNode = new ExoticTreeFinder(tree)
                    .setGetChildrenCallback(node -> {
                        Map<String, ?> subPackages = ReflectionUtil.getFieldValue(node, "subPackages");
                        if (subPackages != null) {
                            return new ArrayList<>(subPackages.values());
                        } else {
                            return null;
                        }
                    })
                    .setPredicate((theTree, theNode) -> {
                        try {
                            Field field = theNode.getClass().getSuperclass().getSuperclass().getDeclaredField("label");
                            field.setAccessible(true);
                            String labelName = (String) field.get(theNode);
                            return labelName.hashCode();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                        return null;
                    })
                    .findTargetParentNode(parentObjectId);
            if (targetParentNode != null) {
                Map<String, ?> packageMap = ReflectionUtil.getFieldValue(targetParentNode, "subPackages");
                List<?> elems = new ArrayList<>();
                if (packageMap != null) {
                    if (packageMap.size() == 0) {
                        elems = ReflectionUtil.getFieldValue(targetParentNode, "classes");
                    } else {
                        elems = new ArrayList<>(packageMap.values());
                    }
                }
                if (elems != null) {
                    future.complete(buildPackageRecord(snapshot, tree, elems, ascendingOrder, sortBy, pagingRequest));
                } else {
                    future.complete(PageViewBuilder.build(new ArrayList<>(), pagingRequest));
                }
            } else {
                future.complete(PageViewBuilder.build(new ArrayList<>(), pagingRequest));
            }
        }
    }

    private PageView<BaseRecord> buildDefaultRecord(ISnapshot snapshot, DominatorQuery.Tree tree, List<?> elements, boolean ascendingOrder, String sortBy,
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
        }, DefaultRecord.sortBy(sortBy, ascendingOrder));
    }

    private PageView<BaseRecord> buildClassRecord(DominatorQuery.Tree tree, List<?> elements, boolean ascendingOrder, String sortBy,
                                                  PagingRequest pagingRequest) {

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
        }, ClassRecord.sortBy(sortBy, ascendingOrder));
    }

    private PageView<BaseRecord> buildClassLoaderRecord(ISnapshot snapshot, DominatorQuery.Tree tree, List<?> elements, boolean ascendingOrder, String sortBy,
                                                        PagingRequest pagingRequest) {

        return PageViewBuilder.build(elements, pagingRequest, e -> {
            ClassLoaderRecord record = new ClassLoaderRecord();
            int objectId = tree.getContext(e).getObjectId();
            IObject object = null;
            try {
                object = snapshot.getObject(objectId);
            } catch (SnapshotException snapshotException) {
                snapshotException.printStackTrace();
            }
            record.setObjectId(objectId);
            record.setLabel((String) tree.getColumnValue(e, 0));
            if (tree.getColumnValue(e, 1) != null) {
                record.setObjects((Integer) tree.getColumnValue(e, 1));
            } else {
                record.setObjects(0);
            }
            record.setShallowSize(((Bytes) tree.getColumnValue(e, 2)).getValue());
            record.setRetainedSize(((Bytes) tree.getColumnValue(e, 3)).getValue());
            record.setPercent((Double) tree.getColumnValue(e, 4));
            record.setObjectType(HeapObject.Type.typeOf(object));
            return record;
        }, ClassLoaderRecord.sortBy(sortBy, ascendingOrder));
    }


    private PageView<BaseRecord> buildPackageRecord(ISnapshot snapshot, DominatorQuery.Tree tree, List<?> elements, boolean ascendingOrder, String sortBy,
                                                    PagingRequest pagingRequest) {

        elements.sort((e1, e2) -> ((Bytes) tree.getColumnValue(e2, 3)).compareTo(tree.getColumnValue(e1, 3)));

        return PageViewBuilder.build(elements, pagingRequest, e -> {
            PackageRecord record = new PackageRecord();
            String label = (String) tree.getColumnValue(e, 0);
            // objectId is actually the hashcode of label string, so don't set it as real object id
            record.setLabel(label);
            record.setObjectId(label.hashCode());
            if (tree.getColumnValue(e, 1) != null) {
                record.setObjects((Integer) tree.getColumnValue(e, 1));
            } else {
                record.setObjects(0);
            }
            record.setShallowSize(((Bytes) tree.getColumnValue(e, 2)).getValue());
            record.setRetainedSize(((Bytes) tree.getColumnValue(e, 3)).getValue());
            record.setPercent((Double) tree.getColumnValue(e, 4));
            record.setObjectType(RecordType.PACKAGE);
            if ((((HashMap) Objects.requireNonNull(getFieldValue(e, "subPackages"))).size() == 0)) {
                record.setObjectType(RecordType.CLASS);
            }
            record.setObjType(false);
            return record;
        }, PackageRecord.sortBy(sortBy, ascendingOrder));
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
