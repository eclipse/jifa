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
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.vo.heapdump.histogram.Record;
import io.vertx.core.Future;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.inspections.HistogramQuery;
import org.eclipse.mat.query.IResult;
import org.eclipse.mat.snapshot.ClassHistogramRecord;
import org.eclipse.mat.snapshot.Histogram;
import org.eclipse.mat.snapshot.ISnapshot;

import java.util.List;

class HistogramRoute extends HeapBaseRoute {

    @RouteMeta(path = "/histogram")
    void histogram(Future<PageView<Record>> future, @ParamKey("file") String file,
                   @ParamKey("groupingBy") Grouping groupingBy, @ParamKey(value = "ids", mandatory = false) int[] ids,
                   PagingRequest pagingRequest) throws Exception {
        try {
            ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
            HistogramQuery query = new HistogramQuery();
            query.snapshot = snapshot;
            if (ids != null) {
                query.objects = HeapDumpSupport.buildHeapObjectArgument(ids);
            }

            query.groupBy = groupingBy.gb;

            IResult result;
            result = query.execute(HeapDumpSupport.VOID_LISTENER);

            if (groupingBy.gb == HistogramQuery.Grouping.BY_CLASS) {

                Histogram histogram = (Histogram) result;
                List<ClassHistogramRecord> records = (List) histogram.getClassHistogramRecords();
                records.forEach(record -> {
                    try {
                        record.calculateRetainedSize(snapshot, true, true, HeapDumpSupport.VOID_LISTENER);
                    } catch (SnapshotException e) {
                        throw new JifaException(e);
                    }
                });
                records.sort((o1, o2) -> (int) (o2.getUsedHeapSize() - o1.getUsedHeapSize()));
                future.complete(PageViewBuilder.build(records, pagingRequest,
                                                      record -> new Record(record.getClassId(), record.getLabel(),
                                                                           Record.Type.CLASS,
                                                                           record.getNumberOfObjects(),
                                                                           record.getUsedHeapSize(),
                                                                           record.getRetainedHeapSize(),
                                                                           record.getNumberOfYoungObjects(),
                                                                           record.getUsedHeapSizeOfYoung(),
                                                                           record.getNumberOfOldObjects(),
                                                                           record.getUsedHeapSizeOfOld())));
            } else {
                // unsupported now.
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new JifaException(e);
        }
    }

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
