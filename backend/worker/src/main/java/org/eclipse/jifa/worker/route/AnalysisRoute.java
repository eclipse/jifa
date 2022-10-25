/********************************************************************************
 * Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation
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

import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.common.util.FileUtil;
import org.eclipse.jifa.common.vo.Progress;
import org.eclipse.jifa.common.vo.Result;
import org.eclipse.jifa.worker.Constant;
import org.eclipse.jifa.worker.WorkerGlobal;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.FileSupport;

import java.io.File;
import java.util.Map;

@MappingPrefix(
        value = {
                "/heap-dump/:file",
                "/gc-log/:file",
                "/thread-dump/:file"}
)
class AnalysisRoute extends BaseRoute {
    private final Analyzer helper = Analyzer.getInstance();

    // TODO: not good enough
    private FileType typeOf(HttpServerRequest request) {
        String uri = request.uri();
        String apiPrefix = WorkerGlobal.stringConfig(Constant.ConfigKey.API_PREFIX);
        int end = uri.indexOf("/", apiPrefix.length() + 1);
        return FileType.getByTag(uri.substring(apiPrefix.length() + 1, end));
    }

    @RouteMeta(path = "/isFirstAnalysis")
    void isFirstAnalysis(HttpServerRequest request, Promise<Result<Boolean>> promise, @ParamKey("file") String file) {
        promise.complete(new Result<>(helper.isFirstAnalysis(typeOf(request), file)));
    }

    @RouteMeta(path = "/analyze", method = HttpMethod.POST)
    void analyze(HttpServerRequest request, Promise<Void> promise, @ParamKey("file") String file,
                 @ParamMap(keys = {"keep_unreachable_objects", "strictness"},
                           mandatory = {false, false, false}) Map<String, String> options) {
        helper.analyze(promise, typeOf(request), file, options);
    }

    @RouteMeta(path = "/progressOfAnalysis")
    void poll(HttpServerRequest request, Promise<Progress> promise, @ParamKey("file") String file) {
        promise.complete(helper.pollProgress(typeOf(request), file));
    }

    @RouteMeta(path = "/release", method = HttpMethod.POST)
    void release(HttpServerRequest request, Promise<Void> promise, @ParamKey("file") String file) {
        helper.release(file);
        promise.complete();
    }

    @RouteMeta(path = "/clean", method = HttpMethod.POST)
    void clean(HttpServerRequest request, Promise<Void> promise, @ParamKey("file") String file) {
        helper.clean(typeOf(request), file);
        promise.complete();
    }

    @RouteMeta(path = "/errorLog")
    void failedLog(HttpServerRequest request, Promise<Result<String>> promise, @ParamKey("file") String file) {
        promise.complete(new Result<>(FileUtil.content(new File(FileSupport.errorLogPath(typeOf(request), file)))));
    }
}
