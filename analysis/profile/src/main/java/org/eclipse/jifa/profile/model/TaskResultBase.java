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
package org.eclipse.jifa.profile.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class TaskResultBase {
    private Task task;
    private Map<StackTrace, Long> samples;

    public TaskResultBase(Task task) {
        this.task = task;
        samples = new HashMap<>();
    }

    public TaskResultBase() {
    }

    public void merge(StackTrace st, long value) {
        if (samples == null) {
            samples = new HashMap<>();
        }
        if (st == null || value <= 0) {
            return;
        }
        samples.put(st, samples.containsKey(st) ? samples.get(st) + value : value);
    }
}
