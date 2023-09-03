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

package org.eclipse.jifa.tda.diagnoser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ThreadDumpAnalysisConfig {

    /** issue a warning if the dump contains at least that many threads */
    private int highThreadsThreshold = 3000;

    /** issue a warning if at least that many threads are blocked */
    private int highBlockedThreadsThreshold = 3;
    
    /** issue a warning if a thread has at least that large a stack */
    private int highStackSizeThreshold = 200;

    /** issue a warning if the ratio between cpu time and elapsed time is at least this value */
    private double highCpuConsumedRatio = 0.5;

    /** issue a warning if a thread is currently throwing an exception */
    boolean reportThrowingException = true;
}
