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
package org.eclipse.jifa.server.controller;

import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.domain.dto.InstanceView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Health check controller
 */
@RestController
public class HealthCheckController extends ConfigurationAccessor {

    private static final LocalDateTime START_TIME = Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime())
                                                           .atZone(ZoneId.systemDefault())
                                                           .toLocalDateTime();

    /**
     * @return InstanceView
     */
    @GetMapping(Constant.HTTP_HEALTH_CHECK_MAPPING)
    public InstanceView healthCheck() {
        return new InstanceView(getRole(), Duration.between(START_TIME, LocalDateTime.now()).toMinutes());
    }
}
