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
package org.eclipse.jifa.master.task;

import io.vertx.reactivex.core.Vertx;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseTask {
    public static final Logger LOGGER = LoggerFactory.getLogger(BaseTask.class);
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
        LOGGER.info("{} end", name());
        PROCESSING.set(false);
    }

    public void doEnd() {
    }

    public void doInit() {
    }

    public abstract void doPeriodic();

    private void init(Vertx vertx) {
        try {
            vertx.setPeriodic(interval(), this::periodic);
            doInit();
            LOGGER.info("Init {} successfully", name());
        } catch (Throwable t) {
            LOGGER.error("Init {} error", name(), t);
            System.exit(-1);
        }
    }

    private void periodic(Long ignored) {
        if (PROCESSING.get() || !PROCESSING.compareAndSet(false, true)) {
            return;
        }

        LOGGER.info("Start {}", name());
        doPeriodic();
    }

    public void trigger() {
        periodic(0L);
    }
}
