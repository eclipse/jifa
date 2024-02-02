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
package org.eclipse.jifa.profile.lang;

import org.eclipse.jifa.profile.enums.GraphType;
import org.eclipse.jifa.profile.enums.Language;
import org.eclipse.jifa.profile.enums.Unit;
import org.eclipse.jifa.profile.lang.java.common.ProfileDimension;
import org.eclipse.jifa.profile.model.Filter;
import org.eclipse.jifa.profile.model.PerfDimension;

import java.util.HashMap;
import java.util.Map;

public class PerfDimensionFactory {

    public static final Map<Language, PerfDimension[]> MAP = new HashMap<>();

    static final Filter FILTER_THREAD = Filter.of("Thread", null);
    static final Filter FILTER_CLASS = Filter.of("Class", null);
    static final Filter FILTER_METHOD = Filter.of("Method", null);

    private static final GraphType[] ALL_GRAPHS = new GraphType[] { GraphType.FLAME_GRAPH, GraphType.ACTIVITY_GRAPH };
    private static final GraphType[] FLAME = new GraphType[] { GraphType.FLAME_GRAPH};
    private static final GraphType[] ACTIVITY = new GraphType[] { GraphType.ACTIVITY_GRAPH};

    static final Filter[] FILTERS = new Filter[] {FILTER_THREAD, FILTER_CLASS, FILTER_METHOD};

    static final PerfDimension DIM_CPU_TIME = PerfDimension.of(ProfileDimension.CPU.getKey(), ProfileDimension.CPU.getDesc(), FILTERS, Unit.NANO_SECOND, FLAME);

    static final PerfDimension DIM_CPU_SAMPLE = PerfDimension.of(ProfileDimension.CPU_SAMPLE.getKey(), ProfileDimension.CPU_SAMPLE.getDesc(), FILTERS, Unit.COUNT, FLAME);

    static final PerfDimension DIM_WALL_CLOCK = PerfDimension.of(ProfileDimension.WALL_CLOCK.getKey(), ProfileDimension.WALL_CLOCK.getDesc(), FILTERS, Unit.NANO_SECOND, FLAME);

    static final PerfDimension DIM_NATIVE_EXECUTION_SAMPLES = PerfDimension.of(ProfileDimension.NATIVE_EXECUTION_SAMPLES.getKey(), ProfileDimension.NATIVE_EXECUTION_SAMPLES.getDesc(), FILTERS, FLAME);

    static final PerfDimension DIM_ALLOC_COUNT = PerfDimension.of(ProfileDimension.ALLOC.getKey(), ProfileDimension.ALLOC.getDesc(), FILTERS, Unit.COUNT, FLAME);

    static final PerfDimension DIM_ALLOC_MEMORY = PerfDimension.of(ProfileDimension.MEM.getKey(), ProfileDimension.MEM.getDesc(), FILTERS, Unit.BYTE, FLAME);

    static final PerfDimension DIM_FILE_IO_TIME = PerfDimension.of(ProfileDimension.FILE_IO_TIME.getKey(), ProfileDimension.FILE_IO_TIME.getDesc(), FILTERS, Unit.NANO_SECOND, ALL_GRAPHS);

    static final PerfDimension DIM_FILE_READ_SIZE = PerfDimension.of(ProfileDimension.FILE_READ_SIZE.getKey(), ProfileDimension.FILE_READ_SIZE.getDesc(), FILTERS, Unit.BYTE, ALL_GRAPHS);

    static final PerfDimension DIM_FILE_WRITE_SIZE = PerfDimension.of(ProfileDimension.FILE_WRITE_SIZE.getKey(), ProfileDimension.FILE_WRITE_SIZE.getDesc(), FILTERS, Unit.BYTE, ALL_GRAPHS);

    static final PerfDimension DIM_SOCKET_READ_TIME = PerfDimension.of(ProfileDimension.SOCKET_READ_TIME.getKey(), ProfileDimension.SOCKET_READ_TIME.getDesc(), FILTERS, Unit.NANO_SECOND, ALL_GRAPHS);

    static final PerfDimension DIM_SOCKET_READ_SIZE = PerfDimension.of(ProfileDimension.SOCKET_READ_SIZE.getKey(), ProfileDimension.SOCKET_READ_SIZE.getDesc(), FILTERS, Unit.BYTE, ALL_GRAPHS);

    static final PerfDimension DIM_SOCKET_WRITE_TIME = PerfDimension.of(ProfileDimension.SOCKET_WRITE_TIME.getKey(), ProfileDimension.SOCKET_WRITE_TIME.getDesc(), FILTERS, Unit.NANO_SECOND, ALL_GRAPHS);

    static final PerfDimension DIM_SOCKET_WRITE_SIZE = PerfDimension.of(ProfileDimension.SOCKET_WRITE_SIZE.getKey(), ProfileDimension.SOCKET_WRITE_SIZE.getDesc(), FILTERS, Unit.BYTE, ALL_GRAPHS);

    static final PerfDimension DIM_LOCK_ACQUIRE = PerfDimension.of(ProfileDimension.LOCK_ACQUIRE.getKey(), ProfileDimension.LOCK_ACQUIRE.getDesc(), FILTERS, ALL_GRAPHS);

    static final PerfDimension DIM_LOCK_WAIT_TIME = PerfDimension.of(ProfileDimension.LOCK_WAIT_TIME.getKey(), ProfileDimension.LOCK_WAIT_TIME.getDesc(), FILTERS, Unit.NANO_SECOND, ALL_GRAPHS);

    static final PerfDimension DIM_SYNCHRONIZATION = PerfDimension.of(ProfileDimension.SYNCHRONIZATION.getKey(), ProfileDimension.SYNCHRONIZATION.getDesc(), FILTERS, Unit.NANO_SECOND, ALL_GRAPHS);

    static final PerfDimension DIM_CLASS_LOAD_WALL_TIME = PerfDimension.of(ProfileDimension.CLASS_LOAD_WALL_TIME.getKey(), ProfileDimension.CLASS_LOAD_WALL_TIME.getDesc(), FILTERS, Unit.NANO_SECOND, ALL_GRAPHS);

    static final PerfDimension DIM_CLASS_LOAD_COUNT = PerfDimension.of(ProfileDimension.CLASS_LOAD_COUNT.getKey(), ProfileDimension.CLASS_LOAD_COUNT.getDesc(), FILTERS, Unit.COUNT, ALL_GRAPHS);

    static final PerfDimension DIM_THREAD_SLEEP_TIME = PerfDimension.of(ProfileDimension.THREAD_SLEEP.getKey(), ProfileDimension.THREAD_SLEEP.getDesc(), FILTERS, Unit.NANO_SECOND, FLAME);

    static {
        // Java
        MAP.put(Language.JAVA, new PerfDimension[]{
                DIM_CPU_TIME,
                DIM_CPU_SAMPLE,
                DIM_WALL_CLOCK,
                DIM_NATIVE_EXECUTION_SAMPLES,
                DIM_ALLOC_COUNT,
                DIM_ALLOC_MEMORY,
                DIM_FILE_IO_TIME,
                DIM_FILE_READ_SIZE,
                DIM_FILE_WRITE_SIZE,
                DIM_SOCKET_READ_TIME,
                DIM_SOCKET_READ_SIZE,
                DIM_SOCKET_WRITE_TIME,
                DIM_SOCKET_WRITE_SIZE,
                DIM_LOCK_ACQUIRE,
                DIM_LOCK_WAIT_TIME,
                DIM_SYNCHRONIZATION,
                DIM_CLASS_LOAD_WALL_TIME,
                DIM_CLASS_LOAD_COUNT,
                DIM_THREAD_SLEEP_TIME,
        });
    }
}
