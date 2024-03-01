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
package org.eclipse.jifa.server.repository;

import org.eclipse.jifa.server.condition.Cluster;
import org.eclipse.jifa.server.domain.entity.cluster.ElasticWorkerEntity;
import org.eclipse.jifa.server.enums.ElasticWorkerPurpose;
import org.eclipse.jifa.server.enums.ElasticWorkerState;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Cluster
@Repository
public interface ElasticWorkerRepo extends CrudRepository<ElasticWorkerEntity, Long> {

    Optional<ElasticWorkerEntity> findByPurposeAndReferenceId(ElasticWorkerPurpose purpose, long referenceId);

    List<ElasticWorkerEntity> findAllByState(ElasticWorkerState state);
}
