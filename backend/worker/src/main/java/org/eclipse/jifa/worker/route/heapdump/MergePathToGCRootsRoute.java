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
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.hda.AnalysisEnv;

import static org.eclipse.jifa.hda.api.Model.GCRootPath;

class MergePathToGCRootsRoute extends HeapBaseRoute {

    @RouteMeta(path = "/mergePathToGCRoots/roots/byClassId")
    void rootsByClassId(Promise<PageView<GCRootPath.MergePathToGCRootsTreeNode>> promise,
                        @ParamKey("file") String file,
                        @ParamKey("classId") int classId,
                        @ParamKey("grouping") GCRootPath.Grouping grouping,
                        PagingRequest pagingRequest) {
        promise.complete(AnalysisEnv.HEAP_DUMP_ANALYZER
                            .getRootsOfMergePathToGCRootsByClassId(Analyzer.getOrOpenAnalysisContext(file), classId,
                                                                   grouping, pagingRequest.getPage(),
                                                                   pagingRequest.getPageSize()));
    }

    @RouteMeta(path = "/mergePathToGCRoots/children/byClassId")
    void childrenByClassId(Promise<PageView<GCRootPath.MergePathToGCRootsTreeNode>> promise,
                           @ParamKey("file") String file,
                           @ParamKey("grouping") GCRootPath.Grouping grouping,
                           PagingRequest pagingRequest,
                           @ParamKey("classId") int classId,
                           @ParamKey("objectIdPathInGCPathTree") int[] objectIdPathInGCPathTree) {
        promise.complete(AnalysisEnv.HEAP_DUMP_ANALYZER
                            .getChildrenOfMergePathToGCRootsByClassId(Analyzer.getOrOpenAnalysisContext(file), classId,
                                                                      objectIdPathInGCPathTree,
                                                                      grouping, pagingRequest.getPage(),
                                                                      pagingRequest.getPageSize()));
    }
}
