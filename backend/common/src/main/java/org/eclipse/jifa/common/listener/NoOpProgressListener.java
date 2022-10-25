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

class NoOpProgressListener implements ProgressListener {

    @Override
    public void beginTask(String s, int i) {}

    @Override
    public void subTask(String s) {}

    @Override
    public void worked(int i) {}

    @Override
    public void sendUserMessage(Level level, String s, Throwable throwable) {}

    @Override
    public String log() {
        return null;
    }

    @Override
    public double percent() {
        return 0;
    }
}
