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

import org.eclipse.jifa.profile.lang.java.model.jfr.RecordedEvent;
import org.eclipse.jifa.profile.lang.java.model.jfr.RecordedStackTrace;
import org.eclipse.jifa.profile.lang.java.util.StackTraceUtil;
import org.eclipse.jifa.profile.lang.java.model.AnalysisResult;
import org.eclipse.jifa.profile.model.DimensionResult;
import org.eclipse.jifa.profile.model.Task;
import org.eclipse.jifa.profile.model.TaskAllocatedMemory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AllocatedMemoryExtractor extends AllocationsExtractor {
    public AllocatedMemoryExtractor(JFRAnalysisContext context) {
        super(context);
    }

    @Override
    void visitObjectAllocationInNewTLAB(RecordedEvent event) {
        RecordedStackTrace stackTrace = event.getStackTrace();
        if (stackTrace == null) {
            return;
        }

        AllocationsExtractor.AllocTaskData allocThreadData = getThreadData(event.getThread());
        if (allocThreadData.getSamples() == null) {
            allocThreadData.setSamples(new HashMap<>());
        }

        long eventTotal = event.getLong("tlabSize");

        allocThreadData.getSamples().compute(stackTrace, (k, temp) -> temp == null ? eventTotal : temp + eventTotal);
        allocThreadData.allocatedMemory += eventTotal;
    }

    @Override
    void visitObjectAllocationOutsideTLAB(RecordedEvent event) {
        RecordedStackTrace stackTrace = event.getStackTrace();
        if (stackTrace == null) {
            return;
        }

        AllocTaskData allocThreadData = getThreadData(event.getThread());
        if (allocThreadData.getSamples() == null) {
            allocThreadData.setSamples(new HashMap<>());
        }

        long eventTotal = event.getLong("allocationSize");

        allocThreadData.getSamples().compute(stackTrace, (k, temp) -> temp == null ? eventTotal : temp + eventTotal);
        allocThreadData.allocatedMemory += eventTotal;
    }

    private List<TaskAllocatedMemory> buildThreadAllocatedMemory() {
        List<TaskAllocatedMemory> taskAllocatedMemoryList = new ArrayList<>();

        for (AllocTaskData data : this.data.values()) {
            if (data.allocatedMemory == 0) {
                continue;
            }

            TaskAllocatedMemory taskAllocatedMemory = new TaskAllocatedMemory();
            Task ta = new Task();
            ta.setId(data.getThread().getJavaThreadId());
            ta.setName(data.getThread().getJavaName());
            taskAllocatedMemory.setTask(ta);

            if (data.getSamples() != null) {
                taskAllocatedMemory.setAllocatedMemory(data.allocatedMemory);
                taskAllocatedMemory.setSamples(data.getSamples().entrySet().stream().collect(
                        Collectors.toMap(
                                e -> StackTraceUtil.build(e.getKey(), context.getSymbols()),
                                Map.Entry::getValue,
                                Long::sum)
                ));
            }

            taskAllocatedMemoryList.add(taskAllocatedMemory);
        }

        taskAllocatedMemoryList.sort((o1, o2) -> {
            long delta = o2.getAllocatedMemory() - o1.getAllocatedMemory();
            return delta > 0 ? 1 : (delta == 0 ? 0 : -1);
        });

        return taskAllocatedMemoryList;
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskAllocatedMemory> memResult = new DimensionResult<>();
        memResult.setList(buildThreadAllocatedMemory());
        result.setAllocatedMemory(memResult);
    }
}
