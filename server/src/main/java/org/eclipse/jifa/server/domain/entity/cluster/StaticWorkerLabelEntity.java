/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jifa.server.domain.entity.shared.BaseEntity;

@SuppressWarnings("JpaDataSourceORMInspection")
@Table(name = "static_worker_labels",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"static_worker_id", "label"})})
@Entity
@Getter
@Setter
public class StaticWorkerLabelEntity extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "static_worker_id")
    private StaticWorkerEntity staticWorker;

    @Column(nullable = false, length = 64)
    private String label;
}
