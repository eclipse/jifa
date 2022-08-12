package org.eclipse.jifa.gclog.event;

import org.eclipse.jifa.gclog.model.GCEventType;
import org.eclipse.jifa.gclog.util.Constant;

public class Safepoint extends GCEvent {
    private double timeToEnter = Constant.UNKNOWN_DOUBLE;

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
