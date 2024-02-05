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
package org.eclipse.jifa.profile.lang.java.model;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jifa.profile.model.*;

import java.util.List;

@Setter
@Getter
public class AnalysisResult {
    private long processingTimeMillis;

    private DimensionResult<TaskCPUTime> cpuTime;

    private DimensionResult<TaskCount> cpuSample;

    private DimensionResult<TaskSum> wallClock;

    private DimensionResult<TaskAllocations> allocations;

    private DimensionResult<TaskAllocatedMemory> allocatedMemory;

    private DimensionResult<TaskCount> nativeExecutionSamples;

    private DimensionResult<TaskSum> fileIOTime;

    private DimensionResult<TaskSum> fileReadSize;

    private DimensionResult<TaskSum> fileWriteSize;

    private DimensionResult<TaskSum> socketReadSize;

    private DimensionResult<TaskSum> socketReadTime;

    private DimensionResult<TaskSum> socketWriteSize;

    private DimensionResult<TaskSum> socketWriteTime;

    private DimensionResult<TaskSum> lockWaitTime;

    private DimensionResult<TaskCount> lockAcquire;

    private DimensionResult<TaskSum> synchronization;

    private DimensionResult<TaskCount> classLoadCount;

    private DimensionResult<TaskSum> classLoadWallTime;

    private DimensionResult<TaskSum> threadSleepTime;

    private List<Problem> problems;
}
