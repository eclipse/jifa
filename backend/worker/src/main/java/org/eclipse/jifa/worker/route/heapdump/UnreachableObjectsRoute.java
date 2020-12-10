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
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.worker.route.PageViewBuilder;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.jifa.worker.vo.heapdump.unreachable.Record;
import org.eclipse.jifa.worker.vo.heapdump.unreachable.Summary;
import org.eclipse.mat.query.Bytes;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.UnreachableObjectsHistogram;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class UnreachableObjectsRoute extends HeapBaseRoute {

    @RouteMeta(path = "/unreachableObjects/summary")
    void summary(Future<Summary> future, @ParamKey("file") String file) {
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        UnreachableObjectsHistogram histogram = (UnreachableObjectsHistogram) snapshot.getSnapshotInfo().getProperty(
            UnreachableObjectsHistogram.class.getName());

        Summary summary = new Summary();
        if (histogram != null) {
            summary.setTotalSize(histogram.getRowCount());
            int objects = 0;
            long shallowSize = 0;
            for (Object record : histogram.getRecords()) {
                objects += (Integer) histogram.getColumnValue(record, 1);
                shallowSize += ((Bytes) histogram.getColumnValue(record, 2)).getValue();
            }

            summary.setObjects(objects);
            summary.setShallowSize(shallowSize);
        }
        future.complete(summary);
    }

    @RouteMeta(path = "/unreachableObjects/records")
    void records(Future<PageView<Record>> future, @ParamKey("file") String file, PagingRequest pagingRequest) {

        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        UnreachableObjectsHistogram histogram = (UnreachableObjectsHistogram) snapshot.getSnapshotInfo().getProperty(
            UnreachableObjectsHistogram.class.getName());

        List<?> total = new ArrayList<>(histogram.getRecords());
        total.sort((Comparator<Object>) (o1, o2) -> {
            long v2 = ((Bytes) histogram.getColumnValue(o2, 2)).getValue();
            long v1 = ((Bytes) histogram.getColumnValue(o1, 2)).getValue();
            return Long.compare(v2, v1);
        });

        future.complete(PageViewBuilder.build(total, pagingRequest, record -> {
            Record r = new Record();
            r.setClassName((String) histogram.getColumnValue(record, 0));
            r.setObjectId(HeapDumpSupport.fetchObjectId(histogram.getContext(record)));
            r.setObjects((Integer) histogram.getColumnValue(record, 1));
            r.setShallowSize(((Bytes) histogram.getColumnValue(record, 2)).getValue());
            return r;
        }));

    }
}
