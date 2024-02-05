/********************************************************************************
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.server.domain.entity.cluster;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jifa.server.enums.ElasticWorkerPurpose;
import org.eclipse.jifa.server.enums.ElasticWorkerState;

@SuppressWarnings("JpaDataSourceORMInspection")
@Table(name = "elastic_workers",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"purpose", "referenceId"})})
@Entity
@Getter
@Setter
public class ElasticWorkerEntity extends WorkerEntity {

    public static final int MAX_FAILURE_MESSAGE_LENGTH = 1024;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private ElasticWorkerPurpose purpose;

    @Column(nullable = false, updatable = false)
    private long referenceId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ElasticWorkerState state;

    @Column(length = 1024)
    private String failureMessage;
}
