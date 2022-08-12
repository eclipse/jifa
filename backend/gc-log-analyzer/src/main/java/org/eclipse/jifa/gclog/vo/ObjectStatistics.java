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

package org.eclipse.jifa.gclog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_DOUBLE;
import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_INT;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjectStatistics {
    private double objectCreationSpeed = UNKNOWN_DOUBLE; // B/ms
    private double objectPromotionSpeed = UNKNOWN_DOUBLE; // B/ms
    private long objectPromotionAvg = UNKNOWN_INT; // B
    private long objectPromotionMax = UNKNOWN_INT; // B
}
