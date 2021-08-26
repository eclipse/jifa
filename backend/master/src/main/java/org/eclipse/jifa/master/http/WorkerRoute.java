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
package org.eclipse.jifa.master.http;

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.eclipse.jifa.common.util.HTTPRespGuarder;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.model.TransferWay;
import org.eclipse.jifa.master.model.User;
import org.eclipse.jifa.master.service.ProxyDictionary;
import org.eclipse.jifa.master.service.reactivex.FileService;
import org.eclipse.jifa.master.service.reactivex.WorkerService;

import java.util.Map;

class WorkerRoute extends BaseRoute {
    private FileService fileService;

    private WorkerService workerService;

    void init(Vertx vertx, JsonObject config, Router apiRouter) {
        fileService = ProxyDictionary.lookup(FileService.class);
        workerService = ProxyDictionary.lookup(WorkerService.class);
        apiRouter.post().path(Constant.START_WORKER).handler(this::startWorker);
        apiRouter.post().path(Constant.STOP_WORKER).handler(this::stopWorker);
        apiRouter.get().path(Constant.START_WORKER_DONE).handler(this::startWorkerDone);
    }

    private void stopWorker(RoutingContext context) {
        HttpServerRequest request = context.request();

        String workerName = request.getParam("workerName");
        workerService.rxStopWorker(workerName).subscribe(
                () -> HTTPRespGuarder.ok(context, "ok"),
                t -> HTTPRespGuarder.fail(context, t)
        );
    }

    private void startWorker(RoutingContext context) {
        HttpServerRequest request = context.request();

        String fileName = request.getParam("file");
        if (fileName != null) {
            startWorkerViaFileName(context, fileName);
        } else {
            startWorkerFromScratch(context);
        }
    }

    private void startWorkerViaFileName(RoutingContext context, String fileName) {
        // file name already exists, start worker accordingly
        User user = context.get(USER_INFO_KEY);

        String workerName = "my-worker" + fileName.hashCode();
        Map<String, String> map = Map.of(
                "originName", "",
                "workerName", workerName,
                "fileName", fileName
        );

        fileService.rxFile(fileName)
                .doOnSuccess(this::assertFileAvailable)
                .doOnSuccess(file -> checkPermission(user, file))
                .map(file -> (long) (file.getSize() * 1.65))
                .flatMapCompletable(memSize -> workerService.rxStartWorkerWithSpec(workerName, memSize))
                .subscribe(
                        () -> HTTPRespGuarder.ok(context, map),
                        t -> HTTPRespGuarder.fail(context, t));
    }

    private void startWorkerFromScratch(RoutingContext context) {
        // construct file name
        String userId = context.<User>get(USER_INFO_KEY).getId();
        HttpServerRequest request = context.request();
        String paramWay = request.getParam("way");
        TransferWay way = TransferWay.valueOf(paramWay);

        String[] paths = way.getPathKeys();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.length; i++) {
            sb.append(request.getParam(paths[i]));
            if (i != paths.length - 1) {
                sb.append("_");

            }
        }
        String originName = extractOriginalName(sb.toString());
        String fileName = buildFileName(userId, originName);

        // construct worker name based on file name
        String workerName = "my-worker" + fileName.hashCode();
        Map<String, String> map = Map.of(
                "originName", originName,
                "workerName", workerName,
                "fileName", fileName
        );
        // start worker
        workerService.rxStartWorker(workerName).subscribe(
                () -> HTTPRespGuarder.ok(context, map),
                t -> HTTPRespGuarder.fail(context, t)
        );
    }

    private void startWorkerDone(RoutingContext context) {
        String workerName = context.request().getParam("workerName");
        workerService.rxStartWorkerDone(workerName).subscribe(
                done -> HTTPRespGuarder.ok(context, done),
                t -> HTTPRespGuarder.fail(context, t)
        );
    }
}
