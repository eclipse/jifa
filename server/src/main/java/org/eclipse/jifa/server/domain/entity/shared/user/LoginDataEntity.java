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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jifa.server.domain.entity.shared.BaseEntity;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "login_data")
@Getter
@Setter
public class LoginDataEntity extends BaseEntity {

    @OneToOne(optional = false)
    private UserEntity user;

    @Column(unique = true, nullable = false, updatable = false, length = 128)
    private String username;

    @Column(length = 64)
    private String passwordHash;
}
