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

import java.util.List;

import org.eclipse.jifa.tda.vo.VThread;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Diagnostic {

    public static final int CODE_HIGH_THREAD_COUNT = 10;
    public static final int CODE_DEADLOCK = 20;
    public static final int CODE_HIGH_BLOCKED_THREAD_COUNT = 30;
    public static final int CODE_HIGH_STACK_SIZE = 40;
    public static final int CODE_HIGH_CPU_RATIO = 50;
    public static final int CODE_THREAD_THROWING_EXCEPTION = 60;  

    private Severity severity;
    private int code;
    private String message;
    private String suggestion;
    private List<VThread> threads;

    public enum Severity {
        OK, INFO, WARNING, ERROR;
    }
    
}
