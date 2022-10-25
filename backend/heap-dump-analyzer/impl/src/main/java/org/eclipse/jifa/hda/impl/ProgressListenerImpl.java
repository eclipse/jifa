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

package org.eclipse.jifa.hda.impl;

import org.eclipse.jifa.hda.api.FilterProgressListener;
import org.eclipse.jifa.common.listener.ProgressListener;
import org.eclipse.mat.util.IProgressListener;

public class ProgressListenerImpl extends FilterProgressListener implements IProgressListener {
    private boolean cancelled = false;

    public ProgressListenerImpl(ProgressListener listener) {
        super(listener);
    }

    @Override
    public void done() {
    }

    @Override
    public boolean isCanceled() {
        return cancelled;
    }

    @Override
    public void setCanceled(boolean b) {
        cancelled = b;
    }

    @Override
    public void sendUserMessage(Severity severity, String s, Throwable throwable) {
        sendUserMessage(Level.valueOf(severity.name()), s, throwable);
    }
}
