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
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jifa.common.util.Validate;
import org.eclipse.jifa.server.enums.Role;
import org.eclipse.jifa.server.enums.SchedulingStrategy;
import org.eclipse.jifa.server.util.DefaultRSAKeyPair;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.eclipse.jifa.server.Constant.DEFAULT_WORKER_PORT;

@ConfigurationProperties(prefix = "jifa", ignoreUnknownFields = false)
@Validated
@Getter
@Setter
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
    @PositiveOrZero
    private int port = 0;

    /**
     * The storage path.
     */
    private Path storagePath;

    private SchedulingStrategy schedulingStrategy;

    /**
     * The name of PersistentVolumeClaim. Used by master to schedule an elastic worker
     */
    private String storagePVCName;

    /**
     * The container image of worker. Used by master to schedule an elastic worker
     */
    private String workerImage;

    @PositiveOrZero
    private int elasticWorkerPort;

    /**
     * Idle threshold in minutes of an elastic worker
     */
    @Positive
    @Min(2)
    private int elasticWorkerIdleThreshold = 5;

    /**
     * Public key used by Jifa
     */
    private RSAPublicKey publicKey;

    /**
     * Private key used by Jifa
     */
    private RSAPrivateKey privateKey;

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

    @PostConstruct
    private void init() {
        if (role == Role.MASTER) {
            Validate.notNull(schedulingStrategy,
                             "jifa.scheduling-strategy must be set when role is master");
            if (schedulingStrategy == SchedulingStrategy.ELASTIC) {
                Validate.notBlank(storagePVCName,
                                  "jifa.storage-pvc-name must be set and not blank when role is master and scheduling strategy is elastic");
                Validate.notBlank(workerImage,
                                  "jifa.worker-image name must be set and not blank when role is master and  scheduling strategy is elastic");
                if (elasticWorkerPort == 0) {
                    elasticWorkerPort = DEFAULT_WORKER_PORT;
                }
            }
        }

        if (role != Role.MASTER || schedulingStrategy != SchedulingStrategy.STATIC) {
            Validate.notNull(storagePath, "jifa.storage-path must be set");

            storagePath = storagePath.toAbsolutePath();

            if (Files.exists(storagePath) || storagePath.toFile().mkdirs()) {
                Validate.isTrue(Files.isDirectory(storagePath), "jifa.storage-path must be a directory");
            }
        }

        if (port == 0) {
            port = role == Role.MASTER ? Constant.DEFAULT_MASTER_PORT : DEFAULT_WORKER_PORT;
        }

        if (publicKey == null || privateKey == null) {
            publicKey = DefaultRSAKeyPair.getPublicKey();
            privateKey = DefaultRSAKeyPair.getPrivateKey();
        }
    }
}
