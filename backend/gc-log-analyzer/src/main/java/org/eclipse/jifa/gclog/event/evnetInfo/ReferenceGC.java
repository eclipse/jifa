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
package org.eclipse.jifa.gclog.event.evnetInfo;

import lombok.Data;
import lombok.NoArgsConstructor;

import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_DOUBLE;
import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_INT;

@Data
@NoArgsConstructor
public class ReferenceGC {

    private double softReferenceStartTime = UNKNOWN_DOUBLE;
    private int softReferenceCount = UNKNOWN_INT;
    private double softReferencePauseTime = UNKNOWN_DOUBLE;

    private double weakReferenceStartTime = UNKNOWN_DOUBLE;
    private int weakReferenceCount = UNKNOWN_INT;
    private double weakReferencePauseTime = UNKNOWN_DOUBLE;

    private double finalReferenceStartTime = UNKNOWN_DOUBLE;
    private int finalReferenceCount = UNKNOWN_INT;
    private double finalReferencePauseTime = UNKNOWN_DOUBLE;

    private double phantomReferenceStartTime = UNKNOWN_DOUBLE;
    private int phantomReferenceCount = UNKNOWN_INT;
    private int phantomReferenceFreedCount;
    private double phantomReferencePauseTime = UNKNOWN_DOUBLE;

    private double jniWeakReferenceStartTime = UNKNOWN_DOUBLE;
    private double jniWeakReferencePauseTime = UNKNOWN_DOUBLE;
}
