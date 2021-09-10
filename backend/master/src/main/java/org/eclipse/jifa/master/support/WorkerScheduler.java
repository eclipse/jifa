/********************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.master.support;

import org.eclipse.jifa.master.model.WorkerInfo;

import java.util.Map;

public interface WorkerScheduler {

    /**
     * Initialize worker scheduler
     * @param params can be something for initialization stage, e.g.
     *               configuration items.
     */
    default void initialize(Map<String, String> params){

    }

    /**
     * Start a worker by given parameters, parameters can be something
     * for worker initialization, e.g. configuration items.
     *
     * @param id unique worker id
     * @param params configuration items
     */
    void startWorker(String id, Map<String, String> params);

    /**
     * Stop the specific worker
     * @param id unique worker id
     */
    void stopWorker(String id);


    /**
     * Get current specific worker information, e.g. starting status,
     * internal IP, etc.
     * @param id unique worker id
     * @return aggregated worker information
     */
    WorkerInfo getWorkerInfo(String id);
}
