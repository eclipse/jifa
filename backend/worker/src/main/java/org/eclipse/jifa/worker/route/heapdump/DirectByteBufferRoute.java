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

import io.vertx.core.Promise;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;

import static org.eclipse.jifa.hda.api.Model.DirectByteBuffer;

class DirectByteBufferRoute extends HeapBaseRoute {

    @RouteMeta(path = "/directByteBuffer/summary")
    void summary(Promise<DirectByteBuffer.Summary> promise, @ParamKey("file") String file) {
        promise.complete(analyzerOf(file).getSummaryOfDirectByteBuffers());
    }

    @RouteMeta(path = "/directByteBuffer/records")
    void record(Promise<PageView<DirectByteBuffer.Item>> promise, @ParamKey("file") String file,
                PagingRequest pagingRequest) {
        promise.complete(analyzerOf(file).getDirectByteBuffers(pagingRequest.getPage(),
                                                               pagingRequest.getPageSize()));
    }
}
