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
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.route.PageViewBuilder;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.SnapshotContext;
import org.eclipse.jifa.worker.vo.heapdump.classloader.Record;
import org.eclipse.jifa.worker.vo.heapdump.classloader.Summary;
import org.eclipse.mat.query.IDecorator;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.query.IStructuredResult;
import org.eclipse.mat.snapshot.model.IClass;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class ClassLoaderRoute extends HeapBaseRoute {

    private static final Field NODE_PARENT_FIELD;
    private static final Field PARENT_NODE_FIELD;

    static {
        try {
            NODE_PARENT_FIELD =
                Class.forName("org.eclipse.mat.inspections.ClassLoaderExplorerQuery$Node").getDeclaredField("parent");
            NODE_PARENT_FIELD.setAccessible(true);

            PARENT_NODE_FIELD =
                Class.forName("org.eclipse.mat.inspections.ClassLoaderExplorerQuery$Parent").getDeclaredField("node");
            PARENT_NODE_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new JifaException(e);
        }
    }

    @RouteMeta(path = "/classLoaderExplorer/summary")
    void summary(Future<Summary> future, @ParamKey("file") String file) throws Exception {

        SnapshotContext.ClassLoaderExplorer explorer = Analyzer.getOrOpenSnapshotContext(file).classLoaderExplorer();
        Summary summary = new Summary();
        summary.setTotalSize(explorer.getRecords().size());
        summary.setDefinedClasses(explorer.getDefinedClasses());
        summary.setNumberOfInstances(explorer.getNumberOfInstances());
        future.complete(summary);
    }

    @RouteMeta(path = "/classLoaderExplorer/classLoader")
    void classLoaders(Future<PageView<Record>> future, @ParamKey("file") String file, @ParamKey(value = "sortBy", mandatory = false) String sortBy,
                      @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder,
                      PagingRequest pagingRequest) throws Exception {
        SnapshotContext.ClassLoaderExplorer explorer = Analyzer.getOrOpenSnapshotContext(file).classLoaderExplorer();
        IStructuredResult resultContext = (IStructuredResult) explorer.getResultContext();
        List<?> elems = explorer.getRecords();

        PageViewBuilder<?, Record> builder = PageViewBuilder.fromList(elems);
        PageView<Record> fut = builder.paging(pagingRequest)
                .map(e -> {
                    try {
                        Record r = new Record();
                        r.setObjectId(resultContext.getContext(e).getObjectId());
                        r.setPrefix(((IDecorator) resultContext).prefix(e));
                        r.setLabel((String) resultContext.getColumnValue(e, 0));
                        r.setDefinedClasses((Integer) resultContext.getColumnValue(e, 1));
                        r.setNumberOfInstances((Integer) resultContext.getColumnValue(e, 2));
                        r.setClassLoader(true);
                        r.setHasParent(NODE_PARENT_FIELD.get(e) != null);
                        return r;
                    } catch (Exception ex) {
                        throw new JifaException(ex);
                    }
                })
                .sort(Record.sortBy(sortBy, ascendingOrder))
                .done();
        future.complete(fut);
    }

    @RouteMeta(path = "/classLoaderExplorer/children")
    void children(Future<PageView<Record>> future, @ParamKey("file") String file,
                  @ParamKey("classLoaderId") int classLoaderId, @ParamKey(value = "sortBy", mandatory = false) String sortBy,
                  @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder, PagingRequest pagingRequest) throws Exception {

        SnapshotContext.ClassLoaderExplorer explorer = Analyzer.getOrOpenSnapshotContext(file).classLoaderExplorer();
        IResultTree resultContext = (IResultTree) explorer.getResultContext();
        Map<Integer, Object> classLoaderIdMap = explorer.getClassLoaderIdMap();
        Object classLoaderNode = classLoaderIdMap.get(classLoaderId);
        ASSERT.notNull(classLoaderNode, "Illegal classLoaderId");

        List<?> elems = resultContext.getChildren(classLoaderNode);

        PageViewBuilder<?, Record> builder = PageViewBuilder.fromList(elems);
        PageView<Record> fut = builder.paging(pagingRequest)
                .map(e -> {
                    try {
                        Record r = new Record();
                        r.setObjectId(resultContext.getContext(e).getObjectId());
                        r.setPrefix(((IDecorator) resultContext).prefix(e));
                        r.setLabel((String) resultContext.getColumnValue(e, 0));
                        r.setNumberOfInstances((Integer) resultContext.getColumnValue(e, 2));
                        if (!(e instanceof IClass)) {
                            r.setClassLoader(true);
                            r.setDefinedClasses((Integer) resultContext.getColumnValue(e, 1));
                            r.setHasParent(NODE_PARENT_FIELD.get(PARENT_NODE_FIELD.get(e)) != null);
                        }
                        return r;
                    } catch (Exception ex) {
                        throw new JifaException(ex);
                    }
                })
                .sort(Record.sortBy(sortBy, ascendingOrder))
                .done();
        future.complete(fut);
    }
}
