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
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.common.enums.ProgressState;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.util.PageViewBuilder;
import org.eclipse.jifa.common.vo.FileInfo;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.FileSupport;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.jifa.worker.vo.heapdump.compare.Record;
import org.eclipse.jifa.worker.vo.heapdump.compare.Summary;
import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.snapshot.Histogram;
import org.eclipse.mat.snapshot.ISnapshot;

import java.util.List;
import java.util.stream.Collectors;

class CompareRoute extends HeapBaseRoute {

    @RouteMeta(path = "/compare/files")
    void files(Future<PageView<FileInfo>> future, @ParamKey("file") String source,
               @ParamKey(value = "expected", mandatory = false) String expected, PagingRequest pagingRequest) {
        future.complete(PageViewBuilder.build(FileSupport.info(FileType.HEAP_DUMP).stream().filter(
            fileInfo -> !fileInfo.getName().equals(source) && fileInfo.getTransferState() == ProgressState.SUCCESS)
                                                         .sorted((i1, i2) -> Long
                                                             .compare(i2.getCreationTime(), i1.getCreationTime()))
                                                         .collect(Collectors.toList()), pagingRequest));
    }

    @RouteMeta(path = "/compare/summary")
    void summary(Future<Summary> future, @ParamKey("file") String target,
                 @ParamKey("baseline") String baseline) throws Exception {

        ISnapshot targetSnapshot = Analyzer.getOrOpenSnapshotContext(target).getSnapshot();
        ISnapshot baselineSnapshot = Analyzer.getOrOpenSnapshotContext(baseline).getSnapshot();
        Histogram targetHistogram = targetSnapshot.getHistogram(HeapDumpSupport.VOID_LISTENER);
        Histogram baselineHistogram = baselineSnapshot.getHistogram(HeapDumpSupport.VOID_LISTENER);
        final Histogram delta = targetHistogram.diffWithBaseline(baselineHistogram);

        long totalObjects = 0;
        long totalShallowHeap = 0;
        for (Object r : delta.getClassHistogramRecords()) {
            totalObjects += (long) delta.getColumnValue(r, 1);
            totalShallowHeap += ((Bytes) delta.getColumnValue(r, 2)).getValue();
        }

        Summary summary = new Summary();
        summary.setTotalSize(delta.getClassHistogramRecords().size());
        summary.setObjects(totalObjects);
        summary.setShallowSize(totalShallowHeap);
        future.complete(summary);
    }

    @SuppressWarnings("unchecked")
    @RouteMeta(path = "/compare/records")
    void record(Future<PageView<Record>> future, @ParamKey("file") String target, @ParamKey("baseline") String baseline,
                PagingRequest pagingRequest) throws Exception {

        ISnapshot targetSnapshot = Analyzer.getOrOpenSnapshotContext(target).getSnapshot();
        ISnapshot baselineSnapshot = Analyzer.getOrOpenSnapshotContext(baseline).getSnapshot();
        Histogram targetHistogram = targetSnapshot.getHistogram(HeapDumpSupport.VOID_LISTENER);
        Histogram baselineHistogram = baselineSnapshot.getHistogram(HeapDumpSupport.VOID_LISTENER);
        final Histogram delta = targetHistogram.diffWithBaseline(baselineHistogram);
        ((List) delta.getClassHistogramRecords()).sort((o1, o2) -> Long
            .compare(((Bytes) delta.getColumnValue(o2, 2)).getValue(),
                     ((Bytes) delta.getColumnValue(o1, 2)).getValue()));

        future.complete(PageViewBuilder.build(delta.getClassHistogramRecords(), pagingRequest, r -> {
            Record record = new Record();
            record.setClassName((String) delta.getColumnValue(r, 0));
            record.setObjects((Long) delta.getColumnValue(r, 1));
            record.setShallowSize(((Bytes) delta.getColumnValue(r, 2)).getValue());
            return record;
        }));
    }
}
