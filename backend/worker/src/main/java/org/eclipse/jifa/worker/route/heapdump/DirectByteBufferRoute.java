/********************************************************************************
 * Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation
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
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;

import static org.eclipse.jifa.hda.api.Model.DirectByteBuffer;
import static org.eclipse.jifa.worker.support.Analyzer.getOrOpenAnalysisContext;
import static org.eclipse.jifa.worker.support.hda.AnalysisEnv.HEAP_DUMP_ANALYZER;

class DirectByteBufferRoute extends HeapBaseRoute {

    @RouteMeta(path = "/directByteBuffer/summary")
    void summary(Future<DirectByteBuffer.Summary> future, @ParamKey("file") String file) {
        future.complete(HEAP_DUMP_ANALYZER.getSummaryOfDirectByteBuffers(getOrOpenAnalysisContext(file)));
    }

    @RouteMeta(path = "/directByteBuffer/records")
    void record(Future<PageView<DirectByteBuffer.Item>> future, @ParamKey("file") String file,
                PagingRequest pagingRequest) {
        future.complete(HEAP_DUMP_ANALYZER.getDirectByteBuffers(getOrOpenAnalysisContext(file), pagingRequest.getPage()
            , pagingRequest.getPageSize()));
    }
}
