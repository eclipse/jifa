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
package org.eclipse.jifa.master.service.impl;

import io.reactivex.Completable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.SingleHelper;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import org.eclipse.jifa.common.enums.StartStatus;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.service.WorkerService;
import org.eclipse.jifa.master.support.K8SWorkerScheduler;
import org.eclipse.jifa.master.support.WorkerClient;
import org.eclipse.jifa.master.support.WorkerScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.eclipse.jifa.master.Constant.uri;

public class WorkerServiceImpl implements WorkerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerServiceImpl.class);

    private final WorkerScheduler workerScheduler;

    public WorkerServiceImpl(JDBCClient dbClient, Pivot pivot) {
        // Use default K8S worker scheduler
        workerScheduler = pivot.getWorkerScheduler();
    }

    @Override
    public void startWorker(String workerName, Handler<AsyncResult<Void>> handler) {
        Completable.fromAction(() -> workerScheduler.startWorker(workerName, Map.of("requestMemSize", "0"))).subscribe(CompletableHelper.toObserver(handler));
    }

    @Override
    public void startWorkerWithSpec(String workerName, long requestMemSize, Handler<AsyncResult<Void>> handler) {
        Completable.fromAction(() -> workerScheduler.startWorker(workerName, Map.of("requestMemSize", Long.toString(requestMemSize)))).subscribe(CompletableHelper.toObserver(handler));
    }

    @Override
    public void stopWorker(String workerName, Handler<AsyncResult<Void>> handler) {
        Completable.fromAction(() -> workerScheduler.stopWorker(workerName)).subscribe(CompletableHelper.toObserver(handler));
    }

    @Override
    public void startWorkerDone(String workerName, Handler<AsyncResult<StartStatus>> handler) {
        String workerIp = workerScheduler.getWorkerInfo(workerName).getIp();
        if (workerIp == null) {
            LOGGER.info("Trying to start worker " + workerIp + "...");
        } else {
            LOGGER.info("Accessing worker " + workerIp);
        }
        WorkerClient.get(workerIp, uri(Constant.PING), 4000/*timeout*/)
                .map(resp -> resp.bodyAsJson(StartStatus.class))
                .onErrorReturn(err -> {
                    if (err instanceof ConnectException) {
                        // This is what we expect since we can not connect to worker if its
                        // functionalities are not available
                        return StartStatus.STARTING;
                    } else if (err instanceof TimeoutException) {
                        return StartStatus.TIMEOUT;
                    } else {
                        // For debugging purpose, use slf4j later
                        err.printStackTrace();

                        return StartStatus.ERROR;
                    }
                })
                .subscribe(SingleHelper.toObserver(handler));
    }
}
