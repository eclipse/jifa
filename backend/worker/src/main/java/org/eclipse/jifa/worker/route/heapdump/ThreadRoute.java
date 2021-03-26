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
import org.eclipse.jifa.common.vo.support.SearchType;
import org.eclipse.jifa.hda.api.Model;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;

import java.util.List;

import static org.eclipse.jifa.worker.support.Analyzer.getOrOpenAnalysisContext;
import static org.eclipse.jifa.worker.support.hda.AnalysisEnv.HEAP_DUMP_ANALYZER;

class ThreadRoute extends HeapBaseRoute {

    @RouteMeta(path = "/threadsSummary")
    void threadsSummary(Promise<Model.Thread.Summary> promise, @ParamKey("file") String file,
                        @ParamKey(value = "searchText", mandatory = false) String searchText,
                        @ParamKey(value = "searchType", mandatory = false) SearchType searchType) {
        promise.complete(HEAP_DUMP_ANALYZER.getSummaryOfThreads(getOrOpenAnalysisContext(file), searchText, searchType));
    }

    @RouteMeta(path = "/threads")
    void threads(Promise<PageView<Model.Thread.Item>> promise, @ParamKey("file") String file,
                 @ParamKey(value = "sortBy", mandatory = false) String sortBy,
                 @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder,
                 @ParamKey(value = "searchText", mandatory = false) String searchText,
                 @ParamKey(value = "searchType", mandatory = false) SearchType searchType,
                 PagingRequest paging) {
        promise.complete(HEAP_DUMP_ANALYZER.getThreads(getOrOpenAnalysisContext(file), sortBy, ascendingOrder,
                                                      searchText, searchType, paging.getPage(), paging.getPageSize()));
    }

    @RouteMeta(path = "/stackTrace")
    void stackTrace(Promise<List<Model.Thread.StackFrame>> promise, @ParamKey("file") String file,
                    @ParamKey("objectId") int objectId) {
        promise.complete(HEAP_DUMP_ANALYZER.




                getStackTrace(getOrOpenAnalysisContext(file), objectId));
    }

    @RouteMeta(path = "/locals")
    void locals(Promise<List<Model.Thread.LocalVariable>> promise, @ParamKey("file") String file,
                @ParamKey("objectId") int objectId, @ParamKey("depth") int depth,
                @ParamKey("firstNonNativeFrame") boolean firstNonNativeFrame) {
        promise.complete(HEAP_DUMP_ANALYZER.getLocalVariables(getOrOpenAnalysisContext(file), objectId, depth,
                                                             firstNonNativeFrame));
    }
}
