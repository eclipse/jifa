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
package org.eclipse.jifa.server.domain.entity.shared;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jifa.server.enums.FileType;

@MappedSuperclass
@Getter
@Setter
public class BaseFileEntity extends BaseEntity {

    @Column(unique = true, nullable = false, updatable = false, length = 64)
    private String uniqueName;

    @ManyToOne(optional = false)
    private UserEntity user;

    @Column(nullable = false, updatable = false, length = 256)
    private String originalName;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private FileType type;
}
