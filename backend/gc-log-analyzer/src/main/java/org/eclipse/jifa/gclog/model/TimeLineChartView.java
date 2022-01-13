/********************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.gclog.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.jifa.gclog.model.GCModel.*;

@Data
/**
 * data prepared for line chart
 */
public class TimeLineChartView {
    private List<Series> dataByTimes;
    private double startTime;
    private double endTime;

    public TimeLineChartView(List<Series> dataByTimes, double startTime, double endTime) {
        this.dataByTimes = dataByTimes;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @NoArgsConstructor
    public static class TimeLineChartViewBuilder {
        private List<String> labels;
        private List<TimeLineChartEvent> events;
        private boolean averageByTime = false;
        private boolean removeZeroInResult = false;
        private double startTime;
        private double endTime;
        private double bucketInterval;

        public TimeLineChartViewBuilder setBucketInterval(double bucketInterval) {
            this.bucketInterval = bucketInterval;
            return this;
        }

        public TimeLineChartViewBuilder setLabels(List<String> labels) {
            this.labels = labels;
            return this;
        }

        public TimeLineChartViewBuilder setEvents(List<TimeLineChartEvent> events) {
            this.events = events;
            return this;
        }

        public TimeLineChartViewBuilder setAverageByTime(boolean averageByTime) {
            this.averageByTime = averageByTime;
            return this;
        }

        public TimeLineChartViewBuilder setRemoveZeroInResult(boolean removeZeroInResult) {
            this.removeZeroInResult = removeZeroInResult;
            return this;
        }

        public TimeLineChartViewBuilder setStartTime(double startTime) {
            this.startTime = startTime;
            return this;
        }

        public TimeLineChartViewBuilder setEndTime(double endTime) {
            this.endTime = endTime;
            return this;
        }

        private boolean endTimeEqualToStartTime() {
            return Math.abs(endTime - startTime) < EPS;
        }

        public TimeLineChartView createByAggregation() {
            if (events.isEmpty() || endTimeEqualToStartTime()) {
                List<Series> dataByTimes = new ArrayList<>();
                return new TimeLineChartView(dataByTimes, 0, 0);
            }
            // use long rather than double to avoid some float inaccuracy problems
            long beginTime = (long) this.startTime;
            long endTime = (long) this.endTime;
            long interval = (long) this.bucketInterval;
            long bucketCount = (endTime - beginTime) / interval;
            if (bucketCount * interval != (endTime - beginTime)) {
                bucketCount++;
            }
            long lastBucketTime = beginTime + (bucketCount - 1) * interval;
            List<Series> dataByTimes = new ArrayList<>();
            Map<String, Integer> labelIndexes = new HashMap<>();
            for (int i = 0; i < labels.size(); i++) {
                labelIndexes.put(labels.get(i), i);
                Map<Long, Double> map = new HashMap<>();
                dataByTimes.add(new Series(labels.get(i), map));
                for (long time = beginTime; time < endTime; time += interval) {
                    map.put(time, 0.0);
                }
            }
            for (TimeLineChartEvent event : events) {
                long bucketTime = beginTime + (long) Math.floor((event.getTime() - beginTime) / interval) * interval;
                bucketTime = Math.min(bucketTime, lastBucketTime);
                Map<Long, Double> map = dataByTimes.get(labelIndexes.get(event.label)).data;
                double data = event.getData();
                if (averageByTime) {
                    data = data / interval * MS2S;
                }
                map.put(bucketTime, data + map.get(bucketTime));
            }
            if (removeZeroInResult) {
                List<Long> keysToRemove = new ArrayList<>();
                for (Series dataByTime : dataByTimes) {
                    keysToRemove.clear();
                    for (long key : dataByTime.data.keySet()) {
                        if (dataByTime.data.get(key).equals(0.0)) {
                            keysToRemove.add(key);
                        }
                    }
                    for (long key : keysToRemove) {
                        dataByTime.data.remove(key);
                    }
                }
            }
            return new TimeLineChartView(dataByTimes, this.startTime, this.endTime);
        }

        public TimeLineChartView createByPreservingData() {
            if (events.isEmpty() || endTimeEqualToStartTime()) {
                List<Series> dataByTimes = new ArrayList<>();
                return new TimeLineChartView(dataByTimes, 0, 0);
            }
            List<Series> dataByTimes = new ArrayList<>();
            Map<String, Integer> labelIndexes = new HashMap<>();
            for (int i = 0; i < labels.size(); i++) {
                labelIndexes.put(labels.get(i), i);
                Map<Long, Double> map = new HashMap<>();
                dataByTimes.add(new Series(labels.get(i), map));
            }
            for (TimeLineChartEvent event : events) {
                Map<Long, Double> map = dataByTimes.get(labelIndexes.get(event.getLabel())).data;
                map.put((long) event.getTime(), event.getData());
            }
            return new TimeLineChartView(dataByTimes, this.startTime, this.endTime);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class TimeLineChartEvent {
        private String label;// should match i18n name in frontend
        private double time;// in ms
        private double data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Series {
        private String label;
        private Map<Long, Double> data;
    }
}
