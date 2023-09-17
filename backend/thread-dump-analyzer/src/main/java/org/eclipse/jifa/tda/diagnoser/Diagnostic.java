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
import java.util.Map;

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

    private Severity severity;
    private Type type;
    private Map<String, Object> params;
    private List<VThread> threads;

    public enum Severity {
        OK, INFO, WARNING, ERROR;
    }

    public enum Type {
        HIGH_THREAD_COUNT,
        DEADLOCK,
        HIGH_BLOCKED_THREAD_COUNT,
        HIGH_STACK_SIZE,
        HIGH_CPU_RATIO,
        THREAD_THROWING_EXCEPTION;
    }

}
