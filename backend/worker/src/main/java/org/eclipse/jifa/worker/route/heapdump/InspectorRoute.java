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
import org.eclipse.jifa.worker.route.MappingPrefix;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;

import static org.eclipse.jifa.worker.support.Analyzer.getOrOpenAnalysisContext;
import static org.eclipse.jifa.worker.support.hda.AnalysisEnv.HEAP_DUMP_ANALYZER;

@MappingPrefix("/inspector")
class InspectorRoute extends HeapBaseRoute {

    @RouteMeta(path = "/addressToId")
    void addressToId(Future<Integer> future, @ParamKey("file") String file, @ParamKey("objectAddress") long address) {
        future.complete(HEAP_DUMP_ANALYZER.mapAddressToId(getOrOpenAnalysisContext(file), address));
    }

    @RouteMeta(path = "/value")
    void value(Future<String> future, @ParamKey("file") String file, @ParamKey("objectId") int objectId) {
        future.complete(HEAP_DUMP_ANALYZER.getObjectValue(getOrOpenAnalysisContext(file), objectId));
    }

    @RouteMeta(path = "/objectView")
    void objectView(Future<Model.InspectorView> future, @ParamKey("file") String file,
                    @ParamKey("objectId") int objectId) {
        future.complete(HEAP_DUMP_ANALYZER.getInspectorView(getOrOpenAnalysisContext(file), objectId));
    }

    @RouteMeta(path = "/fields")
    void fields(Future<PageView<Model.FieldView>> future, @ParamKey("file") String file,
                @ParamKey("objectId") int objectId, PagingRequest pagingRequest) {
        future.complete(HEAP_DUMP_ANALYZER.getFields(getOrOpenAnalysisContext(file), objectId,
                                                     pagingRequest.getPage(), pagingRequest.getPageSize()));
    }

    @RouteMeta(path = "/staticFields")
    void staticFields(Future<PageView<Model.FieldView>> future, @ParamKey("file") String file,
                      @ParamKey("objectId") int objectId, PagingRequest pagingRequest) {

        future.complete(HEAP_DUMP_ANALYZER.getStaticFields(getOrOpenAnalysisContext(file), objectId,
                                                           pagingRequest.getPage(),
                                                           pagingRequest.getPageSize()));
    }
}
