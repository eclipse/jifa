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
import org.eclipse.jifa.gclog.vo.TimeRange;

import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_DOUBLE;

public class TimedEvent {
    // We assume that start time always exists. We will refuse to analyze logs that does not print any time,
    // and will add a suitable start time to events that does not have a start time in log.
    // Unit of all time variables is ms.
    protected double startTime = UNKNOWN_DOUBLE;
    // Real time duration of event. The duration may not exist, and we should always check its existence when using.
    private double duration = UNKNOWN_DOUBLE;

    public double getStartTime() {
        return startTime;
    }

    public double getDuration() {
        return duration;
    }

    public double getEndTime() {
        if (getStartTime() != UNKNOWN_DOUBLE && getDuration() != UNKNOWN_DOUBLE) {
            return getStartTime() + getDuration();
        } else {
            return UNKNOWN_DOUBLE;
        }
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public TimedEvent(double startTime, double duration) {
        this.startTime = startTime;
        this.duration = duration;
    }

    public TimedEvent(double startTime) {
        this.startTime = startTime;
    }

    public TimedEvent() {
    }

    public static TimedEvent fromTimeRange(TimeRange range) {
        return new TimedEvent(range.getStart(), range.length());
    }

    public TimeRange toTimeRange() {
        if (duration != UNKNOWN_DOUBLE) {
            return new TimeRange(getStartTime(), getEndTime());
        } else {
            return new TimeRange(getStartTime(), getStartTime());
        }
    }

    public static TimedEvent newByStartEnd(double start, double end) {
        return new TimedEvent(start, end - start);
    }

    protected void fillInfoToVO(GCModel model, AnalysisConfig config, GCEventVO vo) {
        vo.saveInfo("startTime", getStartTime());
        vo.saveInfo("duration", getDuration());
    }

    public GCEventVO toEventVO(GCModel model, AnalysisConfig config) {
        GCEventVO vo = new GCEventVO();
        fillInfoToVO(model, config, vo);
        return vo;
    }
}
