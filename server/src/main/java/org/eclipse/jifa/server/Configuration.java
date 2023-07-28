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
import org.eclipse.jifa.server.util.KeyPairGenerator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static lombok.AccessLevel.PACKAGE;
import static org.eclipse.jifa.server.Constant.DEFAULT_WORKER_PORT;

@ConfigurationProperties(prefix = "jifa", ignoreUnknownFields = false)
@Validated
@Getter
@Setter(PACKAGE)
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

    private RSAPublicKey publicKey;

    private RSAPrivateKey privateKey;

    private boolean allowAnonymousAccess = true;

    @PostConstruct
    private void init() throws NoSuchAlgorithmException {
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
            Validate.notNull(storagePath, "jifa.storage-path must be set when role is master and scheduling strategy is not static");

            storagePath = storagePath.toAbsolutePath();

            if (Files.exists(storagePath) || storagePath.toFile().mkdirs()) {
                Validate.isTrue(Files.isDirectory(storagePath), "jifa.storage-path must be a directory");
            }
        }

        if (port == 0) {
            port = role == Role.MASTER ? Constant.DEFAULT_MASTER_PORT : DEFAULT_WORKER_PORT;
        }

        if (publicKey == null || privateKey == null) {
            KeyPair keyPair = KeyPairGenerator.generateRSAKeyPair();
            publicKey = (RSAPublicKey) keyPair.getPublic();
            privateKey = (RSAPrivateKey) keyPair.getPrivate();
        }
    }
}
