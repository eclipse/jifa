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
package org.eclipse.jifa.server.domain.entity.shared.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jifa.server.domain.entity.shared.BaseEntity;
import org.eclipse.jifa.server.enums.ExternalLoginMethod;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "external_login_data",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"method", "provider", "principalName"})
        })
@Getter
@Setter
public class ExternalLoginDataEntity extends BaseEntity {

    @OneToOne(optional = false)
    private UserEntity user;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private ExternalLoginMethod method;

    @Column(nullable = false, updatable = false, length = 32)
    private String provider;

    @Column(nullable = false, updatable = false, length = 64)
    private String principalName;
}
