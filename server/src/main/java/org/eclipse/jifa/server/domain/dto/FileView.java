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
package org.eclipse.jifa.server.domain.dto;

import org.eclipse.jifa.server.enums.FileType;

import java.time.LocalDateTime;

public record FileView(long id,
                       String uniqueName,
                       String originalName,
                       FileType type,
                       long size,
                       LocalDateTime createdTime) {
}
