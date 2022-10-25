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

package org.eclipse.jifa.common.listener;

public interface ProgressListener {

    ProgressListener NoOpProgressListener = new NoOpProgressListener();

    void beginTask(String s, int i);

    void subTask(String s);

    void worked(int i);

    void sendUserMessage(Level level, String s, Throwable throwable);

    default void reset() {}

    String log();

    double percent();

    enum Level {
        INFO,
        WARNING,
        ERROR;
    }
}
