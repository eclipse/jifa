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
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.hda.AnalysisEnv;

import java.util.List;

import static org.eclipse.jifa.hda.api.Model.GCRoot;

class GCRootRoute extends HeapBaseRoute {

    @RouteMeta(path = "/GCRoots")
    void roots(Future<List<GCRoot.Item>> future, @ParamKey("file") String file) {
        future.complete(AnalysisEnv.HEAP_DUMP_ANALYZER.getGCRoots(Analyzer.getOrOpenAnalysisContext(file)));
    }

    @RouteMeta(path = "/GCRoots/classes")
    void classes(Future<PageView<GCRoot.Item>> future, @ParamKey("file") String file,
                 @ParamKey("rootTypeIndex") int rootTypeIndex, PagingRequest pagingRequest) {
        future.complete(AnalysisEnv.HEAP_DUMP_ANALYZER.getClassesOfGCRoot(Analyzer.getOrOpenAnalysisContext(file),
                                                                          rootTypeIndex, pagingRequest.getPage(),
                                                                          pagingRequest.getPageSize()));
    }

    @RouteMeta(path = "/GCRoots/class/objects")
    void objects(Future<PageView<Model.JavaObject>> future, @ParamKey("file") String file,
                 @ParamKey("rootTypeIndex") int rootTypeIndex, @ParamKey("classIndex") int classIndex,
                 PagingRequest pagingRequest) {
        future.complete(AnalysisEnv.HEAP_DUMP_ANALYZER.getObjectsOfGCRoot(Analyzer.getOrOpenAnalysisContext(file),
                                                                          rootTypeIndex, classIndex,
                                                                          pagingRequest.getPage(),
                                                                          pagingRequest.getPageSize()));
    }
}
