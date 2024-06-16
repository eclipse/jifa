/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.gclog.fragment;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.gclog.event.GCEvent;
import org.eclipse.jifa.gclog.event.eventInfo.MemoryArea;
import org.eclipse.jifa.gclog.model.GCModel;

import org.eclipse.jifa.gclog.util.Constant;

import java.util.*;

@Slf4j
public class GCModelConverter {
    public List<Metric> toMetrics(GCModel gcModel, Map<String, String> instanceId, long startTime, long endTime) {
        List<Metric> result = new ArrayList<>();
        Map<String, String> sharedLabels = new HashMap<>(instanceId);
        sharedLabels.put("gc_type", gcModel.getCollectorType().getName());
        for (GCEvent gcEvent : gcModel.getGcEvents()) {
            long timestamp = (long)(gcModel.getReferenceTimestamp() + gcEvent.getStartTime());
            if (timestamp >= startTime && timestamp < endTime) {
                result.addAll(new GCEventConverter().toMetrics(gcEvent, timestamp, sharedLabels));
            }
        }
        return result;
    }

    private class GCEventConverter {
        private GCEvent gcEvent;
        private long timestamp;
        private Map<String, String> sharedLabels;
        private List<Metric> result = new ArrayList<>();

        public List<Metric> toMetrics(GCEvent gcEvent, long timestamp, Map<String, String> sharedLabels) {
            this.gcEvent = gcEvent;
            this.timestamp = timestamp;
            this.sharedLabels = sharedLabels;
            addMetricCpuTime();
            addMetricDuration();
            addMetricMemoryUsage();
            addMetricPause();
            addMetricPromotion();
            addMetricSubphase();
            return result;
        }

        private Map<String, String> buildLabel() {
            return sharedLabels;
        }
        private Map<String, String> buildLabel(String key, String value) {
            Map<String, String> label = new HashMap<>(sharedLabels);
            label.put(key, value);
            return label;
        }
        private Map<String, String> buildLabel(String key1, String value1, String key2, String value2) {
            Map<String, String> label = new HashMap<>(sharedLabels);
            label.put(key1, value1);
            label.put(key2, value2);
            return label;
        }

        private void addMetricDuration() {
            if (gcEvent.getDuration() == Constant.UNKNOWN_DOUBLE) {
                return;
            }
            result.add(new Metric(timestamp, buildLabel("type", gcEvent.getEventType().getName()), "GC_COST_TIME", gcEvent.getDuration()));
        }

        private void addMetricPause() {
            if (gcEvent.getPause() == Constant.UNKNOWN_DOUBLE) {
                return;
            }
            result.add(new Metric(timestamp, buildLabel("type", gcEvent.getEventType().getName()), "GC_PAUSE_TIME", gcEvent.getPause()));
        }

        private void addMetricPromotion() {
            if (gcEvent.getPromotion() == Constant.UNKNOWN_INT) {
                return;
            }
            result.add(new Metric(timestamp, buildLabel(), "GC_PROMOTION", gcEvent.getPromotion()));
        }

        private void addMetricCpuTime() {
            if (gcEvent.getCpuTime() == null) {
                return;
            }
            final Set<String> typeSet = new ImmutableSet.Builder<String>().add("USER", "SYS", "REAL").build();
            typeSet.forEach(type ->
                    result.add(new Metric(timestamp, buildLabel("type", type), "GC_CPU_USED", gcEvent.getCpuTime().getValue(type)))
            );
        }

        private void addMetricMemoryUsage() {
            final Map<String, MemoryArea> memoryAreaMap = new ImmutableMap.Builder<String, MemoryArea>()
                    .put("Young", MemoryArea.YOUNG)
                    .put("Old", MemoryArea.OLD)
                    .put("Humongous", MemoryArea.HUMONGOUS)
                    .put("Heap", MemoryArea.HEAP)
                    .put("Metaspace", MemoryArea.METASPACE)
                    .build();
            memoryAreaMap.forEach((key, value) -> {
                if (gcEvent.getMemoryItem(value) != null) {
                    result.add(new Metric(timestamp, buildLabel("type", key), "BEFORE_GC_REGION_SIZE", gcEvent.getMemoryItem(value).getPreUsed()));
                    result.add(new Metric(timestamp, buildLabel("type", key), "AFTER_GC_REGION_SIZE", gcEvent.getMemoryItem(value).getPostUsed()));
                }
            });
        }

        private void addMetricSubphase() {
            if (gcEvent.getPhases() == null) {
                return;
            }
            gcEvent.getPhases().forEach(subphase ->
                    result.add(new Metric(timestamp,
                            buildLabel("subphase", subphase.getEventType().getName(), "type", gcEvent.getEventType().getName()),
                            "GC_SUBPHASE_TIME", subphase.getDuration())));
        }
    }
}
