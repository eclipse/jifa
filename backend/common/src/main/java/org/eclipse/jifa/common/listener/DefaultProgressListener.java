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

import java.io.PrintWriter;
import java.io.StringWriter;

public class DefaultProgressListener implements ProgressListener {

    private final StringBuffer log = new StringBuffer();

    private int total;

    private int done;

    private String lastSubTask;

    private void append(String msg) {
        log.append(msg);
        log.append(System.lineSeparator());
    }

    @Override
    public void beginTask(String s, int i) {
        total += i;
        append(String.format("[Begin task] %s", s));
    }

    @Override
    public void subTask(String s) {
        if (lastSubTask == null || !lastSubTask.equals(s)) {
            lastSubTask = s;
            append(String.format("[Sub task] %s", s));
        }
    }

    @Override
    public void worked(int i) {
        done += i;
    }

    @Override
    public void sendUserMessage(Level level, String s, Throwable throwable) {
        StringWriter sw = new StringWriter();
        switch (level) {
            case INFO:
                sw.append("[INFO] ");
                break;
            case WARNING:
                sw.append("[WARNING] ");
                break;
            case ERROR:
                sw.append("[ERROR] ");
                break;
            default:
                sw.append("[UNKNOWN] ");
        }

        sw.append(s);

        if (throwable != null) {
            sw.append(System.lineSeparator());
            throwable.printStackTrace(new PrintWriter(sw));
        }

        append(sw.toString());
    }

    @Override
    public String log() {
        return log.toString();
    }

    @Override
    public double percent() {
        return total == 0 ? 0 : ((double) done) / ((double) total);
    }
}
