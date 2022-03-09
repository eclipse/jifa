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
package org.eclipse.jifa.master.http;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiPredicate;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceException;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.JifaException;
import org.eclipse.jifa.common.util.HTTPRespGuarder;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.model.User;
import org.eclipse.jifa.master.service.ProxyDictionary;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.service.reactivex.SupportService;
import org.eclipse.jifa.master.service.reactivex.WorkerService;
import org.eclipse.jifa.master.support.K8SWorkerScheduler;

import java.util.concurrent.TimeUnit;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class WorkerRoute extends BaseRoute {

    private WorkerService workerService;

    private SupportService supportService;

    void init(Vertx vertx, JsonObject config, Router apiRouter) {
        workerService = ProxyDictionary.lookup(WorkerService.class);
        supportService = ProxyDictionary.lookup(SupportService.class);
        apiRouter.get().path(Constant.QUERY_ALL_WORKERS).handler(this::queryAll);
        apiRouter.post().path(Constant.WORKER_DISK_CLEANUP).handler(this::diskCleanup);
        if (!Pivot.getInstance().isDefaultPattern()) {
            assert Pivot.getInstance().getScheduler() instanceof K8SWorkerScheduler : "unexpected scheduler";
            apiRouter.get().path(Constant.HEALTH_CHECK).handler(this::healthCheck);
        }
    }

    private void queryAll(RoutingContext context) {
        User user = context.get(Constant.USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        workerService.rxQueryAll()
            .subscribe(
                workers -> HTTPRespGuarder.ok(context, workers),
                t -> HTTPRespGuarder.fail(context, t)
            );
    }

    private void diskCleanup(RoutingContext context) {
        User user = context.get(Constant.USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        HttpServerRequest request = context.request();
        String hostIP = request.getParam("host_ip");
        workerService.rxDiskCleanup(hostIP).subscribe(
            () -> HTTPRespGuarder.ok(context, "ok"),
            t -> HTTPRespGuarder.fail(context, t));
    }

    private void healthCheck(RoutingContext context) {
        supportService.rxIsDBConnectivity()
            .onErrorReturn(e -> Boolean.FALSE)
            .subscribe(connectivity -> {
                if (!connectivity) {
                    HTTPRespGuarder.fail(context, new JifaException("Can not connect to DB"));
                } else {
                    supportService.rxStartDummyWorker()
                        .retry(
                            // Note, http request has its own timeout mechanism, and we can not re-send health check
                            // since liveliness probe is a one-shot test, it's safe to use stream retry API.
                            new RetryStartingWorker(30))
                        .andThen(supportService.rxStopDummyWorker())
                        .subscribe(() -> HTTPRespGuarder.ok(context, "SUCCESS"),
                            e -> HTTPRespGuarder.fail(context, new JifaException("Can not start testing worker due to " + e)));
                }
            }, e -> HTTPRespGuarder.fail(context, e));

    }

    private static class RetryStartingWorker implements BiPredicate<Integer, Throwable> {
        private final int retryLimit;

        public RetryStartingWorker(int retryLimit) {
            this.retryLimit = retryLimit;
        }

        @Override
        public boolean test(@NonNull Integer integer, @NonNull Throwable ex) throws Exception {
            if (integer < retryLimit) {
                if (ex instanceof ServiceException) {
                    ServiceException se = (ServiceException) ex;
                    int failureCode = se.failureCode();
                    if (failureCode == ErrorCode.RETRY.ordinal()) {
                        TimeUnit.SECONDS.sleep(1);
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
