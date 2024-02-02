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

import lombok.Getter;
import org.eclipse.jifa.profile.lang.java.common.EventConstant;
import org.eclipse.jifa.profile.lang.java.model.jfr.RecordedEvent;
import org.eclipse.jifa.profile.lang.java.model.jfr.RecordedThread;
import org.eclipse.jifa.profile.lang.java.model.JavaThread;
import org.eclipse.jifa.profile.lang.java.request.AnalysisRequest;
import org.eclipse.jifa.profile.lang.java.model.symbol.SymbolBase;
import org.eclipse.jifa.profile.lang.java.model.symbol.SymbolTable;

import java.util.*;

public class JFRAnalysisContext {
    private final Map<String, Long> eventTypeIds = new HashMap<>();
    private final Map<Long, JavaThread> threads = new HashMap<>();
    @Getter
    private final List<RecordedEvent> events = new ArrayList<>();
    @Getter
    private final SymbolTable<SymbolBase> symbols = new SymbolTable<>();
    @Getter
    private final AnalysisRequest request;
    @Getter
    private final Set<Long> executionSampleEventTypeIds = new HashSet<>();

    public JFRAnalysisContext(AnalysisRequest request) {
        this.request = request;
    }

    public Long getEventTypeId(String event) {
        return eventTypeIds.get(event);
    }

    public void putEventTypeId(String key, Long id) {
        eventTypeIds.put(key, id);
        if (EventConstant.EXECUTION_SAMPLE.equals(key)) {
            executionSampleEventTypeIds.add(id);
        }
    }

    public boolean isExecutionSampleEventTypeId(long id) {
        return executionSampleEventTypeIds.contains(id);
    }

    public JavaThread getThread(RecordedThread thread) {
        return threads.computeIfAbsent(thread.getJavaThreadId(), id -> {
            JavaThread javaThread = new JavaThread();
            javaThread.setId(id);
            javaThread.setJavaId(thread.getJavaThreadId());
            javaThread.setOsId(thread.getOSThreadId());
            javaThread.setName(thread.getJavaName());
            return javaThread;
        });
    }

    public void addEvent(RecordedEvent event) {
        this.events.add(event);
    }
}
