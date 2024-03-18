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

import lombok.Getter;
import org.eclipse.jifa.jfr.common.EventConstant;
import org.eclipse.jifa.jfr.model.jfr.RecordedEvent;
import org.eclipse.jifa.jfr.model.jfr.RecordedThread;
import org.eclipse.jifa.jfr.model.JavaThread;
import org.eclipse.jifa.jfr.request.AnalysisRequest;
import org.eclipse.jifa.jfr.model.symbol.SymbolBase;
import org.eclipse.jifa.jfr.model.symbol.SymbolTable;

import java.util.*;

import static org.eclipse.jifa.jfr.common.EventConstant.OBJECT_ALLOCATION_SAMPLE;

public class JFRAnalysisContext {
    private final Map<String, Long> eventTypeIds = new HashMap<>();
    private final Map<RecordedEvent.ActiveSetting, String> activeSettings = new HashMap<>();
    private final Map<Long, JavaThread> threads = new HashMap<>();
    private final Map<String, Long> threadNameMap = new HashMap<>();
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

    public synchronized Long getEventTypeId(String event) {
        return eventTypeIds.get(event);
    }

    public synchronized void putEventTypeId(String key, Long id) {
        eventTypeIds.put(key, id);
        if (EventConstant.EXECUTION_SAMPLE.equals(key)) {
            executionSampleEventTypeIds.add(id);
        }
    }

    public synchronized void putActiveSetting(RecordedEvent.ActiveSetting activeSetting, RecordedEvent event) {
        this.activeSettings.put(activeSetting, event.getString("value"));
    }

    public synchronized boolean getActiveSettingBool(String eventName, String settingName) {
        Long eventId = this.getEventTypeId(OBJECT_ALLOCATION_SAMPLE);
        RecordedEvent.ActiveSetting setting = new RecordedEvent.ActiveSetting(eventName, eventId, settingName);
        String v = this.activeSettings.get(setting);
        if (v != null) {
            return Boolean.parseBoolean(v);
        }
        throw new RuntimeException("should not reach here");
    }

    public synchronized boolean isExecutionSampleEventTypeId(long id) {
        return executionSampleEventTypeIds.contains(id);
    }

    public synchronized JavaThread getThread(RecordedThread thread) {
        return threads.computeIfAbsent(thread.getJavaThreadId(), id -> {
            JavaThread javaThread = new JavaThread();
            javaThread.setId(id);
            javaThread.setJavaId(thread.getJavaThreadId());
            javaThread.setOsId(thread.getOSThreadId());

            String name = thread.getJavaName();
            if (id < 0) {
                Long sequence = threadNameMap.compute(thread.getJavaName(), (k, v) -> v == null ? 0 : v + 1);
                if (sequence > 0) {
                    name += "-" + sequence;
                }
            }
            javaThread.setName(name);
            return javaThread;
        });
    }

    public synchronized void addEvent(RecordedEvent event) {
        this.events.add(event);
    }
}
