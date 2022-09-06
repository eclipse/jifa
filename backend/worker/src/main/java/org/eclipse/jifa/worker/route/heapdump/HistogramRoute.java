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

class HistogramRoute extends HeapBaseRoute {

    @RouteMeta(path = "/histogram")
    void histogram(Promise<PageView<Model.Histogram.Item>> promise, @ParamKey("file") String file,
                   @ParamKey("groupingBy") Model.Histogram.Grouping groupingBy,
                   @ParamKey(value = "ids", mandatory = false) int[] ids,
                   @ParamKey(value = "sortBy", mandatory = false) String sortBy,
                   @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder,
                   @ParamKey(value = "searchText", mandatory = false) String searchText,
                   @ParamKey(value = "searchType", mandatory = false) SearchType searchType,
                   PagingRequest pagingRequest) {
        promise.complete(analyzerOf(file).getHistogram(groupingBy,
                                                       ids, sortBy, ascendingOrder, searchText,
                                                       searchType, pagingRequest.getPage(),
                                                       pagingRequest.getPageSize()));
    }

    @RouteMeta(path = "/histogram/children")
    void children(Promise<PageView<Model.Histogram.Item>> promise, @ParamKey("file") String file,
                  @ParamKey("groupingBy") Model.Histogram.Grouping groupingBy,
                  @ParamKey(value = "ids", mandatory = false) int[] ids,
                  @ParamKey(value = "sortBy", mandatory = false) String sortBy,
                  @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder,
                  @ParamKey("parentObjectId") int parentObjectId,
                  PagingRequest pagingRequest) {
        promise.complete(analyzerOf(file).getChildrenOfHistogram(groupingBy, ids,
                                                                 sortBy, ascendingOrder, parentObjectId,
                                                                 pagingRequest.getPage(), pagingRequest.getPageSize()));
    }

    @RouteMeta(path = "/histogram/objects")
    void objects(Promise<PageView<Model.JavaObject>> promise, @ParamKey("file") String file,
                   @ParamKey("classId") int classId, PagingRequest pagingRequest) {
        promise.complete(analyzerOf(file).getHistogramObjects(classId,
                                                              pagingRequest.getPage(),
                                                              pagingRequest.getPageSize()));
    }
}
