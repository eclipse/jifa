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
import org.eclipse.jifa.worker.route.PageViewBuilder;
import org.eclipse.jifa.worker.vo.feature.SearchPredicate;
import org.eclipse.jifa.worker.vo.feature.SearchType;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.jifa.worker.vo.heapdump.duplicatedclass.ClassLoaderRecord;
import org.eclipse.jifa.worker.vo.heapdump.duplicatedclass.ClassRecord;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.inspections.DuplicatedClassesQuery;
import org.eclipse.mat.query.IResultTree;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.GCRootInfo;
import org.eclipse.mat.snapshot.model.IClass;

import java.util.List;

class DuplicatedClassesRoute extends HeapBaseRoute {

    @RouteMeta(path = "/duplicatedClasses/classes")
    void classRecords(Future<PageView<ClassRecord>> future, @ParamKey("file") String file,
                      @ParamKey(value = "searchText", mandatory = false) String searchText,
                      @ParamKey(value = "searchType", mandatory = false) SearchType searchType,
                      PagingRequest pagingRequest) throws Exception {

        DuplicatedClassesQuery query = new DuplicatedClassesQuery();
        query.snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        IResultTree result = (IResultTree) query.execute(HeapDumpSupport.VOID_LISTENER);

        List<?> classes = result.getElements();
        classes.sort((o1, o2) -> ((List) o2).size() - ((List) o1).size());
        PageViewBuilder<?, ClassRecord> builder = PageViewBuilder.fromList(classes);
        PageView<ClassRecord> fut = builder.paging(pagingRequest)
                .map(r -> {
                    ClassRecord record = new ClassRecord();
                    record.setLabel((String) result.getColumnValue(r, 0));
                    record.setCount((Integer) result.getColumnValue(r, 1));
                    return record;
                })
                .filter(SearchPredicate.createPredicate(searchText, searchType))
                .done();

        future.complete(fut);
    }

    @RouteMeta(path = "/duplicatedClasses/classLoaders")
    void classLoaderRecords(Future<PageView<ClassLoaderRecord>> future, @ParamKey("file") String file,
                            @ParamKey("index") int index,
                            PagingRequest pagingRequest) throws Exception {
        DuplicatedClassesQuery query = new DuplicatedClassesQuery();
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        query.snapshot = snapshot;
        IResultTree result = (IResultTree) query.execute(HeapDumpSupport.VOID_LISTENER);

        List<?> classes = result.getElements();
        classes.sort((o1, o2) -> ((List) o2).size() - ((List) o1).size());
        List<?> classLoaders = (List<?>) classes.get(index);
        future.complete(PageViewBuilder.build(classLoaders, pagingRequest, r -> {
            ClassLoaderRecord record = new ClassLoaderRecord();
            record.setLabel((String) result.getColumnValue(r, 0));
            record.setDefinedClassesCount((Integer) result.getColumnValue(r, 2));
            record.setInstantiatedObjectsCount((Integer) result.getColumnValue(r, 3));
            GCRootInfo[] roots;
            try {
                roots = ((IClass) r).getGCRootInfo();
            } catch (SnapshotException e) {
                throw new JifaException(e);
            }
            int id = ((IClass) r).getClassLoaderId();
            record.setObjectId(id);
            record.setGCRoot(snapshot.isGCRoot(id));
            record.setSuffix(roots != null ? GCRootInfo.getTypeSetAsString(roots) : null);
            return record;
        }));
    }
}
