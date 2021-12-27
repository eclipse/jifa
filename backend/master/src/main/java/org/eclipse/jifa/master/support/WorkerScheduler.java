/********************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.master.support;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.sql.SQLConnection;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.Worker;
import org.eclipse.jifa.master.service.impl.Pivot;

import java.util.Map;

public interface WorkerScheduler {

    void initialize(Pivot pivot, Vertx vertx, Map<String, String> configs);


    Single<Worker> decide(Job job, SQLConnection conn);


    boolean supportPendingJob();

    Completable start(Job job);

    /**
     * stop the worker identified by id
     *
     * @param id worker id
     */
    Completable stop(Job job);
}
