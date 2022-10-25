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

package org.eclipse.jifa.tda.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.jifa.tda.enums.JavaThreadState;
import org.eclipse.jifa.tda.enums.OSTreadState;

import java.util.HashMap;
import java.util.Map;

@Data
public class Overview {

    private long timestamp;

    private String vmInfo;

    private int jniRefs;

    private int jniWeakRefs;

    private int deadLockCount;

    private int errorCount;

    private ThreadStat threadStat = new ThreadStat();

    private JavaThreadStat javaThreadStat = new JavaThreadStat();

    private ThreadStat jitThreadStat = new ThreadStat();

    private ThreadStat gcThreadStat = new ThreadStat();

    private ThreadStat otherThreadStat = new ThreadStat();

    private Map<String, ThreadStat> threadGroupStat = new HashMap<>();

    private final OSTreadState[] states = OSTreadState.values();

    private final JavaThreadState[] javaStates = JavaThreadState.values();

    @Data
    public static class ThreadStat {

        private final int[] counts = new int[OSTreadState.COUNT];

        public void inc(OSTreadState state) {
            counts[state.ordinal()]++;
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class JavaThreadStat extends ThreadStat {

        private final int[] javaCounts = new int[JavaThreadState.COUNT];

        private int daemonCount;

        public void inc(JavaThreadState state) {
            javaCounts[state.ordinal()]++;
        }

        public void incDaemon() {
            daemonCount++;
        }
    }
}
