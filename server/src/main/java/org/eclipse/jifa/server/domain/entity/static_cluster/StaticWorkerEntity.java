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
package org.eclipse.jifa.server.domain.entity.static_cluster;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jifa.server.domain.entity.cluster.WorkerEntity;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@SuppressWarnings("JpaDataSourceORMInspection")
@Table(name = "static_workers")
@Entity
@Getter
@Setter
public class StaticWorkerEntity extends WorkerEntity {

    @Column(nullable = false)
    private long availableSpace;

    @Column(nullable = false)
    private long totalSpace;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime lastModifiedTime;
}
