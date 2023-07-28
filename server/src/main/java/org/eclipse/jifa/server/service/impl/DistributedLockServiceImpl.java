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
package org.eclipse.jifa.server.service.impl;

import org.eclipse.jifa.server.condition.Master;
import org.eclipse.jifa.server.domain.entity.cluster.GlobalLockEntity;
import org.eclipse.jifa.server.repository.GlobalLockRepo;
import org.eclipse.jifa.server.service.DistributedLockService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Master
@Service
public class DistributedLockServiceImpl implements DistributedLockService {

    private final GlobalLockRepo globalLockRepo;

    private final Map<String, LockInfo> activeLocks;

    public DistributedLockServiceImpl(GlobalLockRepo globalLockRepo) {
        this.globalLockRepo = globalLockRepo;
        activeLocks = new ConcurrentHashMap<>();
    }

    @Override
    public boolean lock(String id, int validity) {
        if (validity < MINIMAL_VALIDITY) {
            throw new IllegalArgumentException("Illegal validity");
        }

        try {
            String name = decorate(id);
            globalLockRepo.deleteIfExpired(name, LocalDateTime.now());
            if (globalLockRepo.findByUniqueName(name).isPresent()) {
                return false;
            }

            GlobalLockEntity globalLock = new GlobalLockEntity();
            globalLock.setUniqueName(name);
            globalLock.setExpiryAt(LocalDateTime.now().plus(validity, ChronoUnit.SECONDS));
            globalLock = globalLockRepo.save(globalLock);

            activeLocks.put(name, new LockInfo(globalLock.getId(), validity));
            return true;
        } catch (DataIntegrityViolationException ignored) {
            return false;
        }
    }

    @Override
    public void unlock(String id) {
        String name = decorate(id);
        activeLocks.remove(name);
        globalLockRepo.deleteByUniqueName(name);
    }

    @Scheduled(fixedRate = 2, timeUnit = TimeUnit.SECONDS)
    private void refreshExpiryAt() {
        activeLocks.forEach((name, info) -> {
            try {
                globalLockRepo.updateExpiryAtById(LocalDateTime.now().plus(info.validity, ChronoUnit.SECONDS), info.id);
            } catch (Throwable ignored) {
            }
        });
    }

    private String decorate(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return id + "#LOCK";
    }

    private record LockInfo(long id, int validity) {
    }
}

