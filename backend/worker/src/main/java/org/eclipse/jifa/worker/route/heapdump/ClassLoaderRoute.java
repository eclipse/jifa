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

class ClassLoaderRoute extends HeapBaseRoute {

    @RouteMeta(path = "/classLoaderExplorer/summary")
    void summary(Future<Model.ClassLoader.Summary> future, @ParamKey("file") String file) {
        future.complete(HEAP_DUMP_ANALYZER.getSummaryOfClassLoaders(getOrOpenAnalysisContext(file)));
    }

    @RouteMeta(path = "/classLoaderExplorer/classLoader")
    void classLoaders(Future<PageView<Model.ClassLoader.Item>> future, @ParamKey("file") String file, PagingRequest pagingRequest) {
        future.complete(HEAP_DUMP_ANALYZER.getClassLoaders(getOrOpenAnalysisContext(file),
                                                           pagingRequest.getPage(),
                                                           pagingRequest.getPageSize()));
    }

    @RouteMeta(path = "/classLoaderExplorer/children")
    void children(Future<PageView<Model.ClassLoader.Item>> future, @ParamKey("file") String file,
                  @ParamKey("classLoaderId") int classLoaderId, PagingRequest pagingRequest) {
        future.complete(HEAP_DUMP_ANALYZER.getChildrenOfClassLoader(getOrOpenAnalysisContext(file),
                                                                    classLoaderId,
                                                                    pagingRequest.getPage(),
                                                                    pagingRequest.getPageSize()));
    }
}
