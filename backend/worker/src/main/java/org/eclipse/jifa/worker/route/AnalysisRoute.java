/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.worker.route;

import org.eclipse.jifa.worker.Constant;
import org.eclipse.jifa.worker.Global;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.common.vo.Progress;
import org.eclipse.jifa.common.vo.Result;
import org.eclipse.jifa.common.util.FileUtil;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.FileSupport;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

@MappingPrefix(value = {"/heap-dump/:file"})
class AnalysisRoute extends BaseRoute {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisRoute.class);
    private Analyzer helper = Analyzer.getInstance();

    // TODO: not good enough
    private FileType typeOf(HttpServerRequest request) {
        String uri = request.uri();
        String apiPrefix = Global.stringConfig(Constant.ConfigKey.API_PREFIX);
        int end = uri.indexOf("/", apiPrefix.length() + 1);
        return FileType.getByTag(uri.substring(apiPrefix.length() + 1, end));
    }

    @RouteMeta(path = "/isFirstAnalysis")
    void isFirstAnalysis(HttpServerRequest request, Future<Result<Boolean>> future, @ParamKey("file") String file) {
        future.complete(new Result<>(helper.isFirstAnalysis(typeOf(request), file)));
    }

    @RouteMeta(path = "/analyze", method = HttpMethod.POST)
    void analyze(HttpServerRequest request, Future<Void> future, @ParamKey("file") String file,
                 @ParamMap(keys = {"keep_unreachable_objects", "heap_layout"},
                           mandatory = {false, false}) Map<String, String> options) {
        helper.analyze(future, typeOf(request), file, options);
    }

    @RouteMeta(path = "/progressOfAnalysis")
    void poll(HttpServerRequest request, Future<Progress> future, @ParamKey("file") String file) {
        future.complete(helper.pollProgress(typeOf(request), file));
    }

    @RouteMeta(path = "/release", method = HttpMethod.POST)
    void release(HttpServerRequest request, Future<Void> future, @ParamKey("file") String file) {
        helper.release(file);
        future.complete();
    }

    @RouteMeta(path = "/clean", method = HttpMethod.POST)
    void clean(HttpServerRequest request, Future<Void> future, @ParamKey("file") String file) {
        helper.clean(typeOf(request), file);
        future.complete();
    }

    @RouteMeta(path = "/errorLog")
    void failedLog(HttpServerRequest request, Future<Result<String>> future, @ParamKey("file") String file) {
        future.complete(new Result<>(FileUtil.content(new File(FileSupport.errorLogPath(typeOf(request), file)))));
    }
}
