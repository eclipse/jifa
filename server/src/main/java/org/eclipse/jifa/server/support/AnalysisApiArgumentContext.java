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
package org.eclipse.jifa.server.support;

import com.google.gson.JsonObject;
import org.eclipse.jifa.server.enums.FileType;
import org.eclipse.jifa.server.service.FileService;
import org.eclipse.jifa.server.service.StorageService;

import java.nio.file.Path;

public record AnalysisApiArgumentContext(FileType type,
                                         Path target,
                                         JsonObject paramJson,
                                         FileService fileService,
                                         StorageService storageService) {

}
