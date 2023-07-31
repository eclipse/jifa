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
package org.eclipse.jifa.server.domain.entity.shared.file;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jifa.server.domain.entity.shared.file.BaseFileEntity;

import java.time.LocalDateTime;

@SuppressWarnings("JpaDataSourceORMInspection")
@Table(name = "deleted_files")
@Entity
@Getter
@Setter
public class DeletedFileEntity extends BaseFileEntity {

    @Column(nullable = false, updatable = false)
    private long size;

    @Column(nullable = false, updatable = false)
    private LocalDateTime originalCreatedTime;
}
