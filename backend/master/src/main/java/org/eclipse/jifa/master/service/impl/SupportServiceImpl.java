/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.SingleHelper;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.service.SupportService;
import org.eclipse.jifa.master.support.K8SWorkerScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SupportServiceImpl implements SupportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupportServiceImpl.class.getName());

    private final Pivot pivot;

    public SupportServiceImpl(JDBCClient dbClient, Pivot pivot) {
        this.pivot = pivot;
    }

    @Override
    public void isDBConnectivity(Handler<AsyncResult<Boolean>> handler) {
        pivot.isDBConnectivity().subscribe(SingleHelper.toObserver(handler));
    }

    @Override
    public void startDummyWorker(Handler<AsyncResult<Void>> handler) {
        assert pivot.getScheduler() instanceof K8SWorkerScheduler : "unexpected call";

        String testWorkerName = K8SWorkerScheduler.getSpecialWorkerPrefix() + "-health-test";
        Job dummyJob = new Job();
        dummyJob.setTarget(testWorkerName);
        pivot.getScheduler().start(dummyJob).subscribe(CompletableHelper.toObserver(handler));
    }

    @Override
    public void stopDummyWorker(Handler<AsyncResult<Void>> handler) {
        assert pivot.getScheduler() instanceof K8SWorkerScheduler : "unexpected call";

        String testWorkerName = K8SWorkerScheduler.getSpecialWorkerPrefix() + "-health-test";
        Job dummyJob = new Job();
        dummyJob.setTarget(testWorkerName);
        pivot.getScheduler().stop(dummyJob).subscribe(CompletableHelper.toObserver(handler));
    }
}
