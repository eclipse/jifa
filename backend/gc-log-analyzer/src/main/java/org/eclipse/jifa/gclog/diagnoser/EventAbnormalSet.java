package org.eclipse.jifa.gclog.diagnoser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EventAbnormalSet {
    private List<AbnormalPoint> abnormals = null;

    public void add(AbnormalPoint ab) {
        if (abnormals == null) {
            abnormals = new ArrayList<>();
        }
        abnormals.add(ab);
    }

    public AbnormalPoint get(AbnormalType type) {
        if (abnormals != null) {
            for (AbnormalPoint abnormal : abnormals) {
                if (abnormal.getType() == type) {
                    return abnormal;
                }
            }
        }
        return null;
    }

    public int size() {
        if (abnormals == null) {
            return 0;
        }
        return abnormals.size();
    }

    public boolean contains(AbnormalType type) {
        return get(type) != null;
    }

    public void iterate(Consumer<AbnormalPoint> consumer) {
        if (abnormals == null) {
            return;
        }
        for (AbnormalPoint abnormal : abnormals) {
            consumer.accept(abnormal);
        }
    }

    public boolean isEmpty() {
        if (abnormals == null) {
            return true;
        }
        return abnormals.isEmpty();
    }

    public List<AbnormalPoint.AbnormalPointVO> toVO() {
        List<AbnormalPoint.AbnormalPointVO> result = new ArrayList<>();
        this.iterate(ab -> {
            result.add(ab.toVO());
        });
        return result;
    }

    @Override
    public String toString() {
        return "EventAbnormalSet{" +
                "abnormals=" + abnormals +
                '}';
    }
}
