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
package org.eclipse.jifa.gclog.event;

import org.eclipse.jifa.gclog.diagnoser.AnalysisConfig;
import org.eclipse.jifa.gclog.model.GCModel;

public class ThreadEvent extends GCEvent {
    private String threadName;

    public ThreadEvent() {
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    @Override
    protected void appendClassSpecificInfo(StringBuilder sb) {
        sb.append(threadName);
    }

    @Override
    protected void fillInfoToVO(GCModel model, AnalysisConfig config, GCEventVO vo) {
        super.fillInfoToVO(model, config, vo);
        vo.saveInfo("threadName", threadName);
    }
}
