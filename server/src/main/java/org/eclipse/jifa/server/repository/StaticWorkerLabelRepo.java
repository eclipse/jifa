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
package org.eclipse.jifa.server.repository;

import org.eclipse.jifa.server.condition.Cluster;
import org.eclipse.jifa.server.domain.entity.cluster.StaticWorkerLabelEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Cluster
@Repository
public interface StaticWorkerLabelRepo extends CrudRepository<StaticWorkerLabelEntity, Long> {
    List<StaticWorkerLabelEntity> findByLabel(String label);
}
