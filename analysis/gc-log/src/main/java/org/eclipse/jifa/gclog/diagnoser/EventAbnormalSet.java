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
