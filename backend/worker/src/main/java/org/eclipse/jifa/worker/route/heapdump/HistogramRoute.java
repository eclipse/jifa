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
import org.eclipse.jifa.common.util.ReflectionUtil;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.route.PageViewBuilder;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.ExoticTreeFinder;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.jifa.worker.vo.feature.SearchType;
import org.eclipse.jifa.worker.vo.heapdump.RecordType;
import org.eclipse.jifa.worker.vo.heapdump.histogram.Record;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.inspections.HistogramQuery;
import org.eclipse.mat.parser.model.XClassHistogramRecord;
import org.eclipse.mat.parser.model.XClassLoaderHistogramRecord;
import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.query.IResult;
import org.eclipse.mat.snapshot.ClassHistogramRecord;
import org.eclipse.mat.snapshot.Histogram;
import org.eclipse.mat.snapshot.ISnapshot;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.eclipse.jifa.worker.vo.feature.SearchPredicate.createPredicate;

class HistogramRoute extends HeapBaseRoute {

    private static IResult doQuery(ISnapshot snapshot, int[] ids, Grouping groupingBy) throws Exception {
        HistogramQuery query = new HistogramQuery();
        query.snapshot = snapshot;
        if (ids != null) {
            query.objects = HeapDumpSupport.buildHeapObjectArgument(ids);
        }

        query.groupBy = groupingBy.gb;

        return query.execute(HeapDumpSupport.VOID_LISTENER);
    }

    @RouteMeta(path = "/histogram")
    void histogram(Future<PageView<Record>> future, @ParamKey("file") String file,
                   @ParamKey("groupingBy") Grouping groupingBy, @ParamKey(value = "ids", mandatory = false) int[] ids,
                   @ParamKey(value = "sortBy", mandatory = false) String sortBy,
                   @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder,
                   @ParamKey(value = "searchText", mandatory = false) String searchText,
                   @ParamKey(value = "searchType", mandatory = false) SearchType searchType,
                   PagingRequest pagingRequest) throws Exception {
        try {
            ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
            IResult result = doQuery(snapshot, ids, groupingBy);

            switch (groupingBy.gb) {
                case BY_CLASS: {
                    Histogram histogram = (Histogram) result;
                    future.complete(buildClassHistogramRecord(snapshot, (List<? extends ClassHistogramRecord>) histogram
                        .getClassHistogramRecords(), ascendingOrder, sortBy, searchText, searchType, pagingRequest));
                    break;
                }
                case BY_CLASSLOADER: {
                    Histogram.ClassLoaderTree tree = (Histogram.ClassLoaderTree) result;
                    future.complete(buildClassLoaderHistogramRecord(snapshot,
                                                                    (List<? extends XClassLoaderHistogramRecord>) tree
                                                                        .getElements(), ascendingOrder, sortBy,
                                                                    searchText, searchType, pagingRequest));
                    break;
                }
                case BY_SUPERCLASS: {
                    Histogram.SuperclassTree tree = (Histogram.SuperclassTree) result;
                    future.complete(
                        buildSuperClassHistogramRecord(tree, tree.getElements(), ascendingOrder, sortBy, searchText,
                                                       searchType, pagingRequest));
                    break;
                }
                case BY_PACKAGE: {
                    Histogram.PackageTree tree = (Histogram.PackageTree) result;
                    future.complete(
                        buildPackageHistogramRecord(tree, tree.getElements(), ascendingOrder, sortBy, searchText,
                                                    searchType, pagingRequest));
                    break;
                }
                default: {
                    ErrorUtil.shouldNotReachHere();
                }
            }
        } catch (Exception e) {
            throw new JifaException(e);
        }
    }

    @RouteMeta(path = "/histogram/children")
    void children(Future<PageView<Record>> future, @ParamKey("file") String file,
                  @ParamKey("groupingBy") Grouping groupingBy, @ParamKey(value = "ids", mandatory = false) int[] ids,
                  @ParamKey(value = "sortBy", mandatory = false) String sortBy,
                  @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder,
                  @ParamKey("parentObjectId") int parentObjectId,
                  PagingRequest pagingRequest) {
        try {
            ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
            IResult result = doQuery(snapshot, ids, groupingBy);

            switch (groupingBy.gb) {
                case BY_CLASS: {
                    ErrorUtil.errorWith(ErrorCode.SHOULD_NOT_REACH_HERE, "item does not have children");
                    break;
                }
                case BY_CLASSLOADER: {
                    Histogram.ClassLoaderTree tree = (Histogram.ClassLoaderTree) result;
                    List<?> elems = tree.getElements();
                    List<? extends ClassHistogramRecord> children = null;
                    for (Object elem : elems) {
                        if (elem instanceof XClassLoaderHistogramRecord) {
                            if (((XClassLoaderHistogramRecord) elem).getClassLoaderId() == parentObjectId) {
                                children = (List<? extends ClassHistogramRecord>) ((XClassLoaderHistogramRecord) elem)
                                    .getClassHistogramRecords();
                                break;
                            }
                        }
                    }
                    if (children != null) {
                        future.complete(
                            buildClassHistogramRecord(snapshot, children, ascendingOrder, sortBy, null, null,
                                                      pagingRequest));
                    } else {
                        future.complete(PageViewBuilder.build(new ArrayList<>(), pagingRequest));

                    }
                    break;
                }
                case BY_SUPERCLASS: {
                    Histogram.SuperclassTree tree = (Histogram.SuperclassTree) result;
                    List<?> children = new ExoticTreeFinder(tree)
                        .setGetChildrenCallback(node -> {
                            Map<String, ?> subClasses = ReflectionUtil.getFieldValueOrNull(node, "subClasses");
                            if (subClasses != null) {
                                return new ArrayList<>(subClasses.values());
                            }
                            return null;
                        })
                        .setPredicate((theTree, theNode) -> theTree.getContext(theNode).getObjectId())
                        .findChildrenOf(parentObjectId);

                    if (children != null) {
                        future.complete(
                            buildSuperClassHistogramRecord(tree, children, ascendingOrder, sortBy, null, null,
                                                           pagingRequest));
                    } else {
                        future.complete(PageViewBuilder.build(new ArrayList<>(), pagingRequest));
                    }
                    break;
                }
                case BY_PACKAGE: {
                    Histogram.PackageTree tree = (Histogram.PackageTree) result;
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
                            if (!(theNode instanceof XClassHistogramRecord)) {
                                try {
                                    Field field = theNode.getClass().getSuperclass().getDeclaredField("label");
                                    field.setAccessible(true);
                                    String labelName = (String) field.get(theNode);
                                    return labelName.hashCode();
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
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
                        future.complete(buildPackageHistogramRecord(tree, elems, ascendingOrder, sortBy, null, null,
                                                                    pagingRequest));
                    } else {
                        future.complete(PageViewBuilder.build(new ArrayList<>(), pagingRequest));
                    }
                    break;
                }
                default: {
                    ErrorUtil.shouldNotReachHere();
                }
            }
        } catch (Exception e) {
            throw new JifaException(e);
        }
    }

    private PageView<Record> buildClassHistogramRecord(ISnapshot snapshot, List<? extends ClassHistogramRecord> records,
                                                       boolean ascendingOrder, String sortBy, String searchText,
                                                       SearchType searchType, PagingRequest pagingRequest) {
        PageViewBuilder<? extends ClassHistogramRecord, Record> builder = PageViewBuilder.fromList(records);
        return builder
            .beforeMap(record -> {
                try {
                    record.calculateRetainedSize(snapshot, true, true, HeapDumpSupport.VOID_LISTENER);
                } catch (SnapshotException e) {
                    throw new JifaException(e);
                }
            })
            .paging(pagingRequest)
            .map(record ->
                     new Record(record.getClassId(), record.getLabel(),
                                RecordType.CLASS,
                                record.getNumberOfObjects(),
                                record.getUsedHeapSize(),
                                record.getRetainedHeapSize(),
                                0, 0, 0, 0)
                //                                record.getNumberOfYoungObjects(),
                //                                record.getUsedHeapSizeOfYoung(),
                //                                record.getNumberOfOldObjects(),
                //                                record.getUsedHeapSizeOfOld())
            )
            .sort(Record.sortBy(sortBy, ascendingOrder))
            .filter(createPredicate(searchText, searchType))
            .done();
    }

    private PageView<Record> buildClassLoaderHistogramRecord(ISnapshot snapshot,
                                                             List<? extends XClassLoaderHistogramRecord> records,
                                                             boolean ascendingOrder, String sortBy, String searchText,
                                                             SearchType searchType, PagingRequest pagingRequest) {
        PageViewBuilder<? extends XClassLoaderHistogramRecord, Record> builder = PageViewBuilder.fromList(records);
        return builder
            .beforeMap(record -> {
                try {
                    record.calculateRetainedSize(snapshot, true, true, HeapDumpSupport.VOID_LISTENER);
                } catch (SnapshotException e) {
                    throw new JifaException(e);
                }
            })
            .paging(pagingRequest)
            .map(record ->
                     new Record(record.getClassLoaderId(), record.getLabel(),
                                RecordType.CLASS_LOADER,
                                record.getNumberOfObjects(),
                                record.getUsedHeapSize(),
                                record.getRetainedHeapSize(),
                         //                                record.getNumberOfYoungObjects(),
                         //                                record.getUsedHeapSizeOfYoung(),
                         //                                record.getNumberOfOldObjects(),
                         //                                record.getUsedHeapSizeOfOld())
                                0, 0, 0, 0)
            )
            .sort(Record.sortBy(sortBy, ascendingOrder))
            .filter(createPredicate(searchText, searchType))
            .done();
    }

    private PageView<Record> buildSuperClassHistogramRecord(Histogram.SuperclassTree tree, List<?> records,
                                                            boolean ascendingOrder, String sortBy, String searchText,
                                                            SearchType searchType, PagingRequest pagingRequest) {
        PageViewBuilder<?, Record> builder = PageViewBuilder.fromList(records);
        return builder
            .paging(pagingRequest)
            .map(e -> {
                Record record = new Record();
                int objectId = tree.getContext(e).getObjectId();
                record.setType(RecordType.SUPER_CLASS);
                record.setObjectId(objectId);
                record.setLabel((String) tree.getColumnValue(e, 0));
                record.setNumberOfObjects((Long) tree.getColumnValue(e, 1));
                record.setShallowSize(((Bytes) tree.getColumnValue(e, 2)).getValue());

                return record;
            })
            .sort(Record.sortBy(sortBy, ascendingOrder))
            .filter(createPredicate(searchText, searchType))
            .done();
    }

    private PageView<Record> buildPackageHistogramRecord(Histogram.PackageTree tree, List<?> records,
                                                         boolean ascendingOrder, String sortBy, String searchText,
                                                         SearchType searchType, PagingRequest pagingRequest) {
        PageViewBuilder<?, Record> builder = PageViewBuilder.fromList(records);
        return builder
            .paging(pagingRequest)
            .map(e -> {

                Record record = new Record();
                String label = (String) tree.getColumnValue(e, 0);
                record.setLabel(label);

                if (e instanceof XClassHistogramRecord) {
                    int objectId = tree.getContext(e).getObjectId();
                    record.setObjectId(objectId);
                    record.setType(RecordType.CLASS);
                } else {
                    record.setObjectId(label.hashCode());
                    record.setType(RecordType.PACKAGE);
                }

                if (label.matches("^int(\\[\\])*") || label.matches("^char(\\[\\])*") ||
                    label.matches("^byte(\\[\\])*") || label.matches("^short(\\[\\])*") ||
                    label.matches("^boolean(\\[\\])*") || label.matches("^double(\\[\\])*") ||
                    label.matches("^float(\\[\\])*") || label.matches("^long(\\[\\])*") ||
                    label.matches("^void(\\[\\])*")) {
                    record.setType(RecordType.CLASS);
                }
                record.setNumberOfObjects((Long) tree.getColumnValue(e, 1));
                record.setShallowSize(((Bytes) tree.getColumnValue(e, 2)).getValue());

                return record;
            })
            .sort(Record.sortBy(sortBy, ascendingOrder))
            .filter(createPredicate(searchText, searchType))
            .done();
    }

    @SuppressWarnings("unused")
    public enum Grouping {

        BY_CLASS(HistogramQuery.Grouping.BY_CLASS),

        BY_SUPERCLASS(HistogramQuery.Grouping.BY_SUPERCLASS),

        BY_CLASSLOADER(HistogramQuery.Grouping.BY_CLASSLOADER),

        BY_PACKAGE(HistogramQuery.Grouping.BY_PACKAGE);

        HistogramQuery.Grouping gb;

        Grouping(HistogramQuery.Grouping groupingBy) {
            this.gb = groupingBy;
        }
    }

}
