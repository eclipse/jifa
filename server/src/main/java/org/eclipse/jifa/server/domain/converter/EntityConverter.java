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

import org.eclipse.jifa.server.domain.entity.shared.DeletedFileEntity;
import org.eclipse.jifa.server.domain.entity.shared.FileEntity;
import org.eclipse.jifa.server.domain.entity.shared.TransferringFileEntity;

public abstract class EntityConverter {

    public static DeletedFileEntity convert(FileEntity file) {
        DeletedFileEntity deletedFile = new DeletedFileEntity();
        deletedFile.setUniqueName(file.getUniqueName());
        deletedFile.setUser(file.getUser());
        deletedFile.setOriginalName(file.getOriginalName());
        deletedFile.setType(file.getType());
        deletedFile.setOriginalCreatedTime(file.getCreatedTime());

        return deletedFile;
    }

    public static FileEntity convert(TransferringFileEntity transferringFile) {
        FileEntity file = new FileEntity();
        file.setUniqueName(transferringFile.getUniqueName());
        file.setUser(transferringFile.getUser());
        file.setOriginalName(transferringFile.getOriginalName());
        file.setType(transferringFile.getType());
        file.setSize(transferringFile.getTotalSize());
        return file;
    }

}
