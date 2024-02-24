/********************************************************************************
 * Copyright (c) 2021, 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.analysis.listener;

/**
 * Progress listener of the analysis.
 * Currently, it is only used for the first analysis.
 */
public interface ProgressListener {

    /**
     * NoOp
     */
    ProgressListener NoOpProgressListener = new NoOpProgressListener();

    /**
     * begin task
     *
     * @param name     task name
     * @param workload the workload
     */
    void beginTask(String name, int workload);

    /**
     * @param name the name of the sub-task
     */
    void subTask(String name);

    /**
     * @param workload the finished workload
     */
    void worked(int workload);

    /**
     * @param level     the level
     * @param message   the message
     * @param throwable the exception
     */
    void sendUserMessage(Level level, String message, Throwable throwable);

    /**
     * reset the listener
     */
    default void reset() {
    }

    /**
     * @return the log
     */
    String log();

    /**
     * @return current progress
     */
    double percent();

    enum Level {
        INFO,
        WARNING,
        ERROR;
    }
}
