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
import org.eclipse.jifa.hda.api.Model;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class ObjectListRoute extends HeapBaseRoute {

    @RouteMeta(path = "/outbounds")
    void outbounds(Promise<PageView<Model.JavaObject>> promise, @ParamKey("file") String file,
                   PagingRequest pagingRequest,
                   @ParamKey("objectId") int objectId) {

        ASSERT.isTrue(objectId >= 0, "Object id must be greater than or equal to 0");
        promise.complete(analyzerOf(file).getOutboundOfObject(objectId,
                                                              pagingRequest.getPage(),
                                                              pagingRequest.getPageSize()));
    }

    @RouteMeta(path = "/inbounds")
    void inbounds(Promise<PageView<Model.JavaObject>> promise, @ParamKey("file") String file,
                  PagingRequest pagingRequest,
                  @ParamKey("objectId") int objectId) {

        ASSERT.isTrue(objectId >= 0, "Object id must be greater than or equal to 0");
        promise.complete(analyzerOf(file).getInboundOfObject(objectId,
                                                             pagingRequest.getPage(),
                                                             pagingRequest.getPageSize()));
    }
}
