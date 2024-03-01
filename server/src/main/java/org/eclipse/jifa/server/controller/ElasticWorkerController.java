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
package org.eclipse.jifa.server.controller;

import org.eclipse.jifa.server.condition.Master;
import org.eclipse.jifa.server.enums.ElasticWorkerState;
import org.eclipse.jifa.server.service.WorkerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Master
@RestController
@RequestMapping("elastic-workers")
public class ElasticWorkerController {

    private final WorkerService workerService;

    public ElasticWorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @GetMapping("{worker_id}/state")
    public ElasticWorkerState state(@PathVariable("worker_id") long workId) {
        return workerService.getElasticWorkerState(workId);
    }
}
