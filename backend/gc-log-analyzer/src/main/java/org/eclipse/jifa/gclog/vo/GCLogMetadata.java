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

import java.util.List;

import static org.eclipse.jifa.gclog.model.GCEvent.UNKNOWN_DOUBLE;


/**
 * This class provides some necessary information to the frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GCLogMetadata {
    private String collector;
    private String logStyle;
    private double startTime = UNKNOWN_DOUBLE;
    private double endTime = UNKNOWN_DOUBLE;
    private double timestamp = UNKNOWN_DOUBLE;
    private boolean generational = true;
    private boolean pauseless = false;
    private boolean metaspaceCapacityReliable = false;
    private List<String> parentEventTypes;
    private List<String> importantEventTypes;
    private List<String> pauseEventTypes;
    private List<String> mainPauseEventTypes;
    private List<String> allEventTypes;
    private List<String> causes;
}
