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
package org.eclipse.jifa.profile.lang.java.extractor;

import org.eclipse.jifa.profile.lang.java.common.EventConstant;
import org.eclipse.jifa.profile.lang.java.model.jfr.RecordedEvent;
import org.eclipse.jifa.profile.lang.java.model.AnalysisResult;
import org.eclipse.jifa.profile.model.DimensionResult;
import org.eclipse.jifa.profile.model.TaskCount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LockAcquireExtractor extends CountExtractor {
    protected static final List<String> INTERESTED = Collections.unmodifiableList(new ArrayList<String>() {
        {
            add(EventConstant.JAVA_MONITOR_ENTER);
        }
    });

    public LockAcquireExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitJavaMonitorEnter(RecordedEvent event) {
        visitEvent(event);
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskCount> tsResult = new DimensionResult<>();
        tsResult.setList(buildTaskCounts());
        result.setLockAcquire(tsResult);
    }
}
