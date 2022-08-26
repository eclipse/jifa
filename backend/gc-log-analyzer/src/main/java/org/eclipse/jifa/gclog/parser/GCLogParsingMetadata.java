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

package org.eclipse.jifa.gclog.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.eclipse.jifa.gclog.model.modeInfo.GCCollectorType;
import org.eclipse.jifa.gclog.model.modeInfo.GCLogStyle;

@Data
@AllArgsConstructor
public class GCLogParsingMetadata {
    private GCCollectorType collector;
    private GCLogStyle style;
}
