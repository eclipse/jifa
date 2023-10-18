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
package org.eclipse.jifa.server;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.common.util.Validate;
import org.eclipse.jifa.server.enums.Role;
import org.eclipse.jifa.server.enums.SchedulingStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.eclipse.jifa.server.Constant.DEFAULT_PORT;

@ConfigurationProperties(prefix = "jifa", ignoreUnknownFields = false)
@Validated
@Getter
@Setter
@Slf4j
public class Configuration {

    /**
     * The role of a jifa instance.
     *
     * @see Role
     */
    @NotNull
    private Role role;

    /**
     * Server port.
     * This config is
     *
     * @see #init()
     */
    @Positive
    private int port = DEFAULT_PORT;

    /**
     * The storage path.
     */
    private Path storagePath;

    /**
     * The database host
     */
    private String databaseHost;

    /**
     * The database name
     */
    private String databaseName;

    /**
     * The database username
     */
    private String databaseUser;

    /**
     * The database password
     */
    private String databasePassword;

    /**
     * The scheduling strategy.
     */
    private SchedulingStrategy schedulingStrategy;

    /**
     * The name of PersistentVolumeClaim.
     */
    private String storagePVCName;

    /**
     * The name of ServiceAccount
     */
    private String serviceAccountName;

    /**
     * The container image of elastic workers.
     */
    private String elasticWorkerImage;

    /**
     * The JVM options of elastic workers.
     */
    private String elasticWorkerJVMOptions;

    /**
     * The port of elastic workers.
     */
    @Positive
    private int elasticWorkerPort = DEFAULT_PORT;

    /**
     * Idle threshold in minutes of elastic workers
     */
    @Positive
    @Min(2)
    private int elasticWorkerIdleThreshold = 5;

    /**
     * Whether to allow anonymous access, default is true
     */
    private boolean allowAnonymousAccess = true;

    /**
     * Whether to allow registration, default is true
     */
    private boolean allowRegistration = true;

    /**
     * default root username
     */
    private String rootUsername = "root";

    /**
     * default root password
     */
    private String rootPassword = "password";

    /**
     * Input files is some specified files that will be added automatically when starting.
     */
    private Path[] inputFiles;

    @PostConstruct
    private void init() {
        if (role == Role.MASTER) {
            Validate.notNull(schedulingStrategy,
                             "jifa.scheduling-strategy must be set when role is master");
            if (schedulingStrategy == SchedulingStrategy.ELASTIC) {
                Validate.notBlank(storagePVCName,
                                  "jifa.storage-pvc-name must be set and not blank when role is master and scheduling strategy is elastic");
                Validate.notBlank(serviceAccountName,
                                  "jifa.service-account-name must be set and not blank when role is master and scheduling strategy is elastic");
                Validate.notBlank(elasticWorkerImage,
                                  "jifa.elastic-worker-image name must be set and not blank when role is master and  scheduling strategy is elastic");
            }
        }

        if (role != Role.MASTER || schedulingStrategy != SchedulingStrategy.STATIC) {
            Validate.notNull(storagePath, "jifa.storage-path must be set");

            storagePath = storagePath.toAbsolutePath();

            if (Files.exists(storagePath) || storagePath.toFile().mkdirs()) {
                Validate.isTrue(Files.isDirectory(storagePath), "jifa.storage-path must be a directory");
            }
        }
    }
}
