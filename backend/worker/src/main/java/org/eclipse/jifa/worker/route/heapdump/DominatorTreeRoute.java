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
import org.eclipse.jifa.common.util.ReflectionUtil;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.route.PageViewBuilder;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.ExoticTreeFinder;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.jifa.worker.vo.feature.SearchPredicate;
import org.eclipse.jifa.worker.vo.feature.SearchType;
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
import static org.eclipse.jifa.common.util.ReflectionUtil.getFieldValueOrNull;

class DominatorTreeRoute extends HeapBaseRoute {

    private static final Constructor<?> DEFAULT_NODE;

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
    void roots(Future<PageView<? extends BaseRecord>> future, @ParamKey("file") String file,
               @ParamKey("grouping") Grouping grouping, @ParamKey(value = "sortBy", mandatory = false) String sortBy,
               @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder,
               @ParamKey(value = "searchText", mandatory = false) String searchText,
               @ParamKey(value = "searchType", mandatory = false) SearchType searchType,
               PagingRequest pagingRequest) throws Exception {
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        DominatorQuery query = new DominatorQuery();
        query.snapshot = snapshot;
        query.groupBy = grouping.internalGroping;
        DominatorQuery.Tree tree = query.execute(HeapDumpSupport.VOID_LISTENER);

        if (grouping == Grouping.NONE) {
            future.complete(buildDefaultRecord(snapshot, tree, tree.getElements(), ascendingOrder, sortBy, searchText, searchType, pagingRequest));
        } else if (grouping == Grouping.BY_CLASS) {
            future.complete(buildClassRecord(tree, tree.getElements(), ascendingOrder, sortBy, searchText, searchType, pagingRequest));
        } else if (grouping == Grouping.BY_CLASSLOADER) {
            future.complete(buildClassLoaderRecord(snapshot, tree, tree.getElements(), ascendingOrder, sortBy, searchText, searchType, pagingRequest));
        } else if (grouping == Grouping.BY_PACKAGE) {
            future.complete(buildPackageRecord(snapshot, tree, tree.getElements(), ascendingOrder, sortBy, searchText, searchType, pagingRequest));
        }
    }

    @RouteMeta(path = "/dominatorTree/children")
    void children(Future<PageView<? extends BaseRecord>> future, @ParamKey("file") String file,
                  @ParamKey("grouping") Grouping grouping, @ParamKey(value = "sortBy", mandatory = false) String sortBy,
                  @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder,
                  PagingRequest pagingRequest,
                  @ParamKey("parentObjectId") int parentObjectId, @ParamKey(value = "idPathInResultTree", mandatory = false) int[] idPathInResultTree) throws Exception {

        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        DominatorQuery query = new DominatorQuery();
        query.snapshot = snapshot;
        query.groupBy = grouping.internalGroping;
        DominatorQuery.Tree tree = query.execute(HeapDumpSupport.VOID_LISTENER);

        if (grouping == Grouping.NONE) {
            Object parentNode = DEFAULT_NODE.newInstance(parentObjectId);
            future.complete(buildDefaultRecord(snapshot, tree, tree.getChildren(parentNode), ascendingOrder, sortBy, null, null, pagingRequest));
        } else if (grouping == Grouping.BY_CLASS) {
            Object object = HeapDumpSupport.fetchObjectInResultTree(tree, idPathInResultTree);
            List<?> elements = object == null ? Collections.emptyList() : tree.getChildren(object);
            future.complete(buildClassRecord(tree, elements, ascendingOrder, sortBy, null, null, pagingRequest));
        } else if (grouping == Grouping.BY_CLASSLOADER) {
            List<?> children = new ExoticTreeFinder(tree)
                    .setGetChildrenCallback(tree::getChildren)
                    .setPredicate((theTree, theNode) -> theTree.getContext(theNode).getObjectId())
                    .findChildrenOf(parentObjectId);

            if (children != null) {
                future.complete(buildClassLoaderRecord(snapshot, tree, children, ascendingOrder, sortBy, null, null, pagingRequest));
            } else {
                future.complete(PageViewBuilder.build(new ArrayList<>(), pagingRequest));
            }
        } else if (grouping == Grouping.BY_PACKAGE) {
            Object targetParentNode = new ExoticTreeFinder(tree)
                    .setGetChildrenCallback(node -> {
                        Map<String, ?> subPackages = ReflectionUtil.getFieldValueOrNull(node, "subPackages");
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
                Map<String, ?> packageMap = ReflectionUtil.getFieldValueOrNull(targetParentNode, "subPackages");
                List<?> elems = new ArrayList<>();
                if (packageMap != null) {
                    if (packageMap.size() == 0) {
                        elems = ReflectionUtil.getFieldValueOrNull(targetParentNode, "classes");
                    } else {
                        elems = new ArrayList<>(packageMap.values());
                    }
                }
                if (elems != null) {
                    future.complete(buildPackageRecord(snapshot, tree, elems, ascendingOrder, sortBy, null, null, pagingRequest));
                } else {
                    future.complete(PageViewBuilder.build(new ArrayList<>(), pagingRequest));
                }
            } else {
                future.complete(PageViewBuilder.build(new ArrayList<>(), pagingRequest));
            }
        }
    }

    private PageView<DefaultRecord> buildDefaultRecord(ISnapshot snapshot, DominatorQuery.Tree tree, List<?> elements, boolean ascendingOrder, String sortBy,
                                                       String searchText, SearchType searchType, PagingRequest pagingRequest) {
        PageViewBuilder<?, DefaultRecord> builder = PageViewBuilder.fromList(elements);
        return builder
                .paging(pagingRequest)
                .map(e -> {
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
                })
                .sort(DefaultRecord.sortBy(sortBy, ascendingOrder))
                .filter(SearchPredicate.createPredicate(searchText, searchType))
                .done();
    }

    private PageView<ClassRecord> buildClassRecord(DominatorQuery.Tree tree, List<?> elements, boolean ascendingOrder, String sortBy,
                                                   String searchText, SearchType searchType, PagingRequest pagingRequest) {

        PageViewBuilder<?, ClassRecord> builder = PageViewBuilder.fromList(elements);
        return builder
                .paging(pagingRequest)
                .map(e -> {
                    ClassRecord record = new ClassRecord();
                    int objectId = tree.getContext(e).getObjectId();
                    record.setObjectId(objectId);
                    record.setLabel((String) tree.getColumnValue(e, 0));
                    record.setObjects((Integer) tree.getColumnValue(e, 1));
                    record.setShallowSize(((Bytes) tree.getColumnValue(e, 2)).getValue());
                    record.setRetainedSize(((Bytes) tree.getColumnValue(e, 3)).getValue());
                    record.setPercent((Double) tree.getColumnValue(e, 4));
                    return record;
                })
                .sort(ClassRecord.sortBy(sortBy, ascendingOrder))
                .filter(SearchPredicate.createPredicate(searchText, searchType))
                .done();
    }

    private PageView<ClassLoaderRecord> buildClassLoaderRecord(ISnapshot snapshot, DominatorQuery.Tree tree, List<?> elements, boolean ascendingOrder, String sortBy,
                                                               String searchText, SearchType searchType, PagingRequest pagingRequest) {

        PageViewBuilder<?, ClassLoaderRecord> builder = PageViewBuilder.fromList(elements);
        return builder
                .paging(pagingRequest)
                .map(e -> {
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
                })
                .sort(ClassLoaderRecord.sortBy(sortBy, ascendingOrder))
                .filter(SearchPredicate.createPredicate(searchText, searchType))
                .done();
    }


    private PageView<PackageRecord> buildPackageRecord(ISnapshot snapshot, DominatorQuery.Tree tree, List<?> elements, boolean ascendingOrder, String sortBy,
                                                       String searchText, SearchType searchType, PagingRequest pagingRequest) {

        PageViewBuilder<?, PackageRecord> builder = PageViewBuilder.fromList(elements);
        return builder
                .paging(pagingRequest)
                .map(e -> {
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
                    if ((((HashMap) Objects.requireNonNull(getFieldValueOrNull(e, "subPackages"))).size() == 0)) {
                        int objectId = tree.getContext(e).getObjectId();
                        System.out.println(objectId);
                        record.setObjectType(RecordType.CLASS);
                    }
                    record.setObjType(false);
                    return record;
                })
                .sort(PackageRecord.sortBy(sortBy, ascendingOrder))
                .filter(SearchPredicate.createPredicate(searchText, searchType))
                .done();
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
