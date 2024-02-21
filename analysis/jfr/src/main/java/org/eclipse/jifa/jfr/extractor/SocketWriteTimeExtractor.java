/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.jfr.extractor;

import org.eclipse.jifa.jfr.common.EventConstant;
import org.eclipse.jifa.jfr.model.jfr.RecordedEvent;
import org.eclipse.jifa.jfr.model.DimensionResult;
import org.eclipse.jifa.jfr.model.AnalysisResult;
import org.eclipse.jifa.jfr.model.TaskSum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SocketWriteTimeExtractor extends SumExtractor {
    protected static final List<String> INTERESTED = Collections.unmodifiableList(new ArrayList<>() {
        {
            add(EventConstant.SOCKET_WRITE);
        }
    });

    public SocketWriteTimeExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitSocketWrite(RecordedEvent event) {
        visitEvent(event);
    }

    private void visitEvent(RecordedEvent event) {
        long eventValue = event.getDurationNano();
        visitEvent(event, eventValue);
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskSum> tsResult = new DimensionResult<>();
        tsResult.setList(buildTaskSums());
        result.setSocketWriteTime(tsResult);
    }
}
