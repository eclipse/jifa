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
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;

import static org.eclipse.jifa.hda.api.Model.DominatorTree;
import static org.eclipse.jifa.worker.support.Analyzer.getOrOpenAnalysisContext;
import static org.eclipse.jifa.worker.support.hda.AnalysisEnv.HEAP_DUMP_ANALYZER;

class DominatorTreeRoute extends HeapBaseRoute {

    @RouteMeta(path = "/dominatorTree/roots")
    void roots(Future<PageView<? extends DominatorTree.Item>> future, @ParamKey("file") String file,
               @ParamKey("grouping") DominatorTree.Grouping grouping,
               @ParamKey(value = "sortBy", mandatory = false) String sortBy,
               @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder,
               @ParamKey(value = "searchText", mandatory = false) String searchText,
               @ParamKey(value = "searchType", mandatory = false) SearchType searchType,
               PagingRequest pagingRequest) {
        future.complete(HEAP_DUMP_ANALYZER
                            .getRootsOfDominatorTree(getOrOpenAnalysisContext(file), grouping, sortBy,
                                                     ascendingOrder, searchText, searchType,
                                                     pagingRequest.getPage(),
                                                     pagingRequest.getPageSize()));
    }

    @RouteMeta(path = "/dominatorTree/children")
    void children(Future<PageView<? extends DominatorTree.Item>> future, @ParamKey("file") String file,
                  @ParamKey("grouping") DominatorTree.Grouping grouping,
                  @ParamKey(value = "sortBy", mandatory = false) String sortBy,
                  @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder,
                  PagingRequest pagingRequest,
                  @ParamKey("parentObjectId") int parentObjectId, @ParamKey(value = "idPathInResultTree",
                                                                            mandatory = false) int[] idPathInResultTree) {
        future.complete(HEAP_DUMP_ANALYZER
                            .getChildrenOfDominatorTree(getOrOpenAnalysisContext(file), grouping, sortBy,
                                                        ascendingOrder,
                                                        parentObjectId,
                                                        idPathInResultTree, pagingRequest.getPage(),
                                                        pagingRequest.getPageSize()));
    }
}
