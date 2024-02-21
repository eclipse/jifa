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
import org.eclipse.jifa.jfr.util.StackTraceUtil;
import org.eclipse.jifa.jfr.model.AnalysisResult;
import org.eclipse.jifa.jfr.model.DimensionResult;
import org.eclipse.jifa.jfr.model.Task;
import org.eclipse.jifa.jfr.model.TaskCount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NativeExecutionExtractor extends CountExtractor {

    protected static final List<String> INTERESTED = Collections.unmodifiableList(new ArrayList<>() {
        {
            add(EventConstant.NATIVE_EXECUTION_SAMPLE);
        }
    });

    public NativeExecutionExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitNativeExecutionSample(RecordedEvent event) {
        visitEvent(event);
    }

    private List<TaskCount> buildTaskExecutionSamples() {
        List<TaskCount> nativeSamples = new ArrayList<>();

        for (TaskCountData data : this.data.values()) {
            if (data.count == 0) {
                continue;
            }

            TaskCount threadSamples = new TaskCount();
            Task ta = new Task();
            ta.setId(data.getThread().getJavaThreadId());
            ta.setName(data.getThread().getJavaName());
            threadSamples.setTask(ta);

            if (data.getSamples() != null) {
                threadSamples.setCount(data.count);
                threadSamples.setSamples(data.getSamples().entrySet().stream().collect(
                        Collectors.toMap(
                                e -> StackTraceUtil.build(e.getKey(), context.getSymbols()),
                                Map.Entry::getValue,
                                Long::sum
                        )
                ));
            }

            nativeSamples.add(threadSamples);
        }

        nativeSamples.sort((o1, o2) -> {
            long delta = o2.getCount() - o1.getCount();
            return delta > 0 ? 1 : (delta == 0 ? 0 : -1);
        });

        return nativeSamples;
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskCount> nativeResult = new DimensionResult<>();
        nativeResult.setList(buildTaskExecutionSamples());
        result.setNativeExecutionSamples(nativeResult);
    }
}
