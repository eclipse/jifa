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
import org.eclipse.jifa.common.vo.support.SearchType;
import org.eclipse.jifa.hda.api.Model;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;

import static org.eclipse.jifa.worker.support.Analyzer.getOrOpenAnalysisContext;
import static org.eclipse.jifa.worker.support.hda.AnalysisEnv.HEAP_DUMP_ANALYZER;

class HistogramRoute extends HeapBaseRoute {

    @RouteMeta(path = "/histogram")
    void histogram(Future<PageView<Model.Histogram.Item>> future, @ParamKey("file") String file,
                   @ParamKey("groupingBy") Model.Histogram.Grouping groupingBy,
                   @ParamKey(value = "ids", mandatory = false) int[] ids,
                   @ParamKey(value = "sortBy", mandatory = false) String sortBy,
                   @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder,
                   @ParamKey(value = "searchText", mandatory = false) String searchText,
                   @ParamKey(value = "searchType", mandatory = false) SearchType searchType,
                   PagingRequest pagingRequest) {
        future.complete(HEAP_DUMP_ANALYZER.getHistogram(getOrOpenAnalysisContext(file), groupingBy,
                                                        ids, sortBy, ascendingOrder, searchText,
                                                        searchType, pagingRequest.getPage(),
                                                        pagingRequest.getPageSize()));
    }

    @RouteMeta(path = "/histogram/children")
    void children(Future<PageView<Model.Histogram.Item>> future, @ParamKey("file") String file,
                  @ParamKey("groupingBy") Model.Histogram.Grouping groupingBy,
                  @ParamKey(value = "ids", mandatory = false) int[] ids,
                  @ParamKey(value = "sortBy", mandatory = false) String sortBy,
                  @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder,
                  @ParamKey("parentObjectId") int parentObjectId,
                  PagingRequest pagingRequest) {
        future.complete(HEAP_DUMP_ANALYZER
                            .getChildrenOfHistogram(getOrOpenAnalysisContext(file), groupingBy, ids,
                                                    sortBy, ascendingOrder, parentObjectId,
                                                    pagingRequest.getPage(), pagingRequest.getPageSize()));
    }

}
