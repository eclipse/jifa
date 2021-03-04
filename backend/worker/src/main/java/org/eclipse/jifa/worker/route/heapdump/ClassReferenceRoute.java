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
import org.eclipse.jifa.hda.api.Model;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;

import static org.eclipse.jifa.worker.support.Analyzer.getOrOpenAnalysisContext;
import static org.eclipse.jifa.worker.support.hda.AnalysisEnv.HEAP_DUMP_ANALYZER;

class ClassReferenceRoute extends HeapBaseRoute {

    @RouteMeta(path = "/classReference/inbounds/class")
    void inboundsClassInfo(Future<Model.ClassReferrer.Item> future, @ParamKey("file") String file,
                           @ParamKey("objectId") int objectId) {

        future.complete(HEAP_DUMP_ANALYZER.getInboundClassOfClassReference(getOrOpenAnalysisContext(file), objectId));
    }

    @RouteMeta(path = "/classReference/outbounds/class")
    void outboundsClassInfo(Future<Model.ClassReferrer.Item> future, @ParamKey("file") String file,
                            @ParamKey("objectId") int objectId) {

        future.complete(HEAP_DUMP_ANALYZER.getOutboundClassOfClassReference(getOrOpenAnalysisContext(file), objectId));
    }

    @RouteMeta(path = "/classReference/inbounds/children")
    void inboundsChildren(Future<PageView<Model.ClassReferrer.Item>> future, @ParamKey("file") String file,
                          PagingRequest pagingRequest,
                          @ParamKey("objectIds") int[] objectIds) {
        future.complete(
            HEAP_DUMP_ANALYZER.getInboundsOfClassReference(getOrOpenAnalysisContext(file), objectIds,
                                                           pagingRequest.getPage(), pagingRequest.getPageSize()));
    }

    @RouteMeta(path = "/classReference/outbounds/children")
    void outboundsChildren(Future<PageView<Model.ClassReferrer.Item>> future, @ParamKey("file") String file,
                           PagingRequest pagingRequest,
                           @ParamKey("objectIds") int[] objectIds) {
        future.complete(
            HEAP_DUMP_ANALYZER.getOutboundsOfClassReference(getOrOpenAnalysisContext(file), objectIds,
                                                            pagingRequest.getPage(), pagingRequest.getPageSize()));
    }
}
