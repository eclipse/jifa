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

package org.eclipse.jifa.hda.api;

import org.eclipse.jifa.common.listener.ProgressListener;

public class FilterProgressListener implements ProgressListener {

    ProgressListener listener;

    public FilterProgressListener(ProgressListener listener) {
        assert listener != null;
        this.listener = listener;
    }

    @Override
    public void beginTask(String s, int i) {
        listener.beginTask(s, i);
    }

    @Override
    public void subTask(String s) {
        listener.subTask(s);
    }

    @Override
    public void worked(int i) {
        listener.worked(i);
    }

    @Override
    public void sendUserMessage(Level level, String s, Throwable throwable) {
        listener.sendUserMessage(level, s, throwable);
    }

    @Override
    public String log() {
        return listener.log();
    }

    @Override
    public double percent() {
        return listener.percent();
    }
}
