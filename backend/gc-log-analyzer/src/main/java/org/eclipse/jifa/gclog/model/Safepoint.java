package org.eclipse.jifa.gclog.model;

import org.eclipse.jifa.gclog.model.GCEvent;
import org.eclipse.jifa.gclog.model.GCEventType;

public class Safepoint extends GCEvent {
    private double timeToEnter = UNKNOWN_DOUBLE;

    public Safepoint() {
        this.setEventType(GCEventType.SAFEPOINT);
    }

    public double getTimeToEnter() {
        return timeToEnter;
    }

    public void setTimeToEnter(double timeToEnter) {
        this.timeToEnter = timeToEnter;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendStartTime(sb);
        sb.append(String.format("Total time for which application threads were stopped: " +
                "%.3f seconds, Stopping threads took: %.3f seconds", getDuration(), getTimeToEnter()));
        return sb.toString();
    }
}
