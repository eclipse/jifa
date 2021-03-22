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

import static org.eclipse.jifa.worker.support.Analyzer.getOrOpenAnalysisContext;
import static org.eclipse.jifa.worker.support.hda.AnalysisEnv.HEAP_DUMP_ANALYZER;

class ClassLoaderRoute extends HeapBaseRoute {

    @RouteMeta(path = "/classLoaderExplorer/summary")
    void summary(Promise<Model.ClassLoader.Summary> promise, @ParamKey("file") String file) {
        promise.complete(HEAP_DUMP_ANALYZER.getSummaryOfClassLoaders(getOrOpenAnalysisContext(file)));
    }

    @RouteMeta(path = "/classLoaderExplorer/classLoader")
    void classLoaders(Promise<PageView<Model.ClassLoader.Item>> promise, @ParamKey("file") String file,
                      PagingRequest pagingRequest) {
        promise.complete(HEAP_DUMP_ANALYZER.getClassLoaders(getOrOpenAnalysisContext(file),
                                                           pagingRequest.getPage(),
                                                           pagingRequest.getPageSize()));
    }

    @RouteMeta(path = "/classLoaderExplorer/children")
    void children(Promise<PageView<Model.ClassLoader.Item>> promise, @ParamKey("file") String file,
                  @ParamKey("classLoaderId") int classLoaderId, PagingRequest pagingRequest) {
        promise.complete(HEAP_DUMP_ANALYZER.getChildrenOfClassLoader(getOrOpenAnalysisContext(file),
                                                                    classLoaderId,
                                                                    pagingRequest.getPage(),
                                                                    pagingRequest.getPageSize()));
    }
}
