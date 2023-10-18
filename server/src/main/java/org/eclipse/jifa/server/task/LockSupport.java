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
package org.eclipse.jifa.server.task;

import jakarta.annotation.Nullable;
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.service.DistributedLockService;
import org.springframework.stereotype.Component;

@Component
public class LockSupport extends ConfigurationAccessor {

    private final DistributedLockService distributedLockService;

    public LockSupport(@Nullable DistributedLockService distributedLockService) {
        this.distributedLockService = distributedLockService;
    }

    void runUnderLock(Runnable runnable, String lockName) {
        assert distributedLockService != null;
        if (!distributedLockService.lock(lockName)) {
            return;
        }
        try {
            runnable.run();
        } finally {
            distributedLockService.unlock(lockName);
        }
    }

    void runUnderLockIfMaster(Runnable runnable, String lockName) {
        boolean shouldLock = isMaster();

        if (shouldLock) {
            assert distributedLockService != null;
            if (!distributedLockService.lock(lockName)) {
                return;
            }
        }

        try {
            runnable.run();
        } finally {
            if (shouldLock) {
                distributedLockService.unlock(lockName);
            }
        }
    }
}
