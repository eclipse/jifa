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
package org.eclipse.jifa.server.service;

public interface DistributedLockService {

    int MINIMAL_VALIDITY = 5;

    /**
     * @see #lock(String, int)
     */
    default boolean lock(String id) {
        return lock(id, MINIMAL_VALIDITY);
    }

    /**
     * Performs a distributed lock action by id.
     *
     * @param id       the identifier of the lock
     * @param validity the validity in seconds of this lock
     * @return true if locked
     */
    boolean lock(String id, int validity);

    /**
     * Performs a distributed unlock action.
     *
     * @param id the identifier of the lock
     */
    void unlock(String id);
}
