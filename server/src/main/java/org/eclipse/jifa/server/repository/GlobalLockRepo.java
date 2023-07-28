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
package org.eclipse.jifa.server.repository;

import org.eclipse.jifa.server.condition.Master;
import org.eclipse.jifa.server.domain.entity.cluster.GlobalLockEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Master
@Repository
public interface GlobalLockRepo extends CrudRepository<GlobalLockEntity, Long> {

    Optional<GlobalLockEntity> findByUniqueName(String uniqueName);

    @Transactional
    @Modifying
    @Query("update GlobalLockEntity lock set lock.expiryAt= ?1 where lock.id = ?2")
    void updateExpiryAtById(LocalDateTime expiryAt, Long id);

    @Transactional
    @Modifying
    @Query("delete GlobalLockEntity lock where lock.uniqueName = ?1 and lock.expiryAt <= ?2")
    void deleteIfExpired(String uniqueName, LocalDateTime now);

    @Transactional
    @Modifying
    void deleteByUniqueName(String uniqueName);
}
