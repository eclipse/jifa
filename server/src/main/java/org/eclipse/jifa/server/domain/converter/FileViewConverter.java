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
package org.eclipse.jifa.server.domain.converter;

import org.eclipse.jifa.server.domain.dto.FileView;
import org.eclipse.jifa.server.domain.entity.shared.file.FileEntity;

public class FileViewConverter {

    public static FileView convert(FileEntity entity) {
        return new FileView(
                entity.getId(),
                entity.getUniqueName(),
                entity.getOriginalName(),
                entity.getType(),
                entity.getSize(),
                entity.getCreatedTime()
        );
    }
}
