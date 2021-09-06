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
package org.eclipse.jifa.master.task;

import io.vertx.core.Vertx;
import org.eclipse.jifa.master.support.Pivot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseTask {
    public static final Logger LOGGER = LoggerFactory.getLogger(BaseTask.class);
    private static ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(5);
    protected final Pivot pivot;
    private final AtomicBoolean PROCESSING = new AtomicBoolean(false);

    BaseTask(Pivot pivot, Vertx vertx) {
        this.pivot = pivot;
        init(vertx);
    }

    public abstract String name();

    public abstract long interval();

    void end() {
        doEnd();
        LOGGER.info("{} success", name());
        PROCESSING.set(false);
    }

    public void doEnd() {
    }

    public void doInit() {
    }

    public abstract void doPeriodic();

    private void init(Vertx vertx) {
        try {
            threadPool.scheduleWithFixedDelay(this::periodic, 10 * 1000, interval(), TimeUnit.MILLISECONDS);
            doInit();
            LOGGER.info("Init {} successfully", name());
        } catch (Throwable t) {
            LOGGER.error("Init {} error", name(), t);
            System.exit(-1);
        }
    }

    private void periodic() {
        if (PROCESSING.get() || !PROCESSING.compareAndSet(false, true)) {
            return;
        }

        LOGGER.info("Start {}", name());
        doPeriodic();
    }

    public void trigger() {
        periodic();
    }
}
