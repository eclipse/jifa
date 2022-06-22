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

package org.eclipse.jifa.gclog.model;

public class TimedEvent {
    // unit of all time is ms
    protected double startTime = GCEvent.UNKNOWN_DOUBLE;
    // real time duration of event
    private double duration = GCEvent.UNKNOWN_DOUBLE;

    public double getStartTime() {
        return startTime;
    }

    public double getDuration() {
        return duration;
    }

    public double getEndTime() {
        if (getStartTime() != GCEvent.UNKNOWN_DOUBLE && getDuration() != GCEvent.UNKNOWN_DOUBLE) {
            return getStartTime() + getDuration();
        } else {
            return GCEvent.UNKNOWN_DOUBLE;
        }
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }
}
