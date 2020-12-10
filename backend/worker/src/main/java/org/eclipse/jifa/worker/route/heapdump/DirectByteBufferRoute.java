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
import org.eclipse.jifa.worker.support.heapdump.SnapshotContext;
import org.eclipse.jifa.worker.vo.heapdump.directbytebuffer.Record;
import org.eclipse.jifa.worker.vo.heapdump.directbytebuffer.Summary;
import org.eclipse.mat.query.IResultTable;

class DirectByteBufferRoute extends HeapBaseRoute {

    @RouteMeta(path = "/directByteBuffer/summary")
    void summary(Future<Summary> future, @ParamKey("file") String file) throws Exception {

        SnapshotContext.DirectByteBuffer data = Analyzer.getOrOpenSnapshotContext(file).directByteBuffer();

        Summary summary = new Summary();
        summary.setTotalSize(data.getTotalSize());
        summary.setPosition(data.getPosition());
        summary.setLimit(data.getLimit());
        summary.setCapacity(data.getCapacity());
        future.complete(summary);
    }

    @RouteMeta(path = "/directByteBuffer/records")
    void record(Future<PageView<Record>> future, @ParamKey("file") String file,
                PagingRequest pagingRequest) throws Exception {

        SnapshotContext.DirectByteBuffer data = Analyzer.getOrOpenSnapshotContext(file).directByteBuffer();
        IResultTable resultContext = data.getResultContext();

        future.complete(PageViewBuilder.build(new PageViewBuilder.Callback<Object>() {
            @Override
            public int totalSize() {
                return data.getTotalSize();
            }

            @Override
            public Object get(int index) {
                return data.getResultContext().getRow(index);
            }
        }, pagingRequest, row -> {
            Record record = new Record();
            record.setObjectId(resultContext.getContext(row).getObjectId());
            record.setLabel(data.label(row));
            record.setPosition(data.position(row));
            record.setLimit(data.limit(row));
            record.setCapacity(data.capacity(row));
            return record;
        }));
    }
}
