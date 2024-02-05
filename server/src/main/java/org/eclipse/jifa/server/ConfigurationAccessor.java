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
package org.eclipse.jifa.server;

import org.eclipse.jifa.common.util.Validate;
import org.eclipse.jifa.server.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ConfigurationAccessor {

    @Autowired
    protected Configuration config;

    protected final Role getRole() {
        return config.getRole();
    }

    protected final boolean isStandaloneWorker() {
        return getRole() == Role.STANDALONE_WORKER;
    }

    protected final boolean isStaticWorker() {
        return getRole() == Role.STATIC_WORKER;
    }

    protected final boolean isElasticWorker() {
        return getRole() == Role.ELASTIC_WORKER;
    }

    protected final boolean isWorker() {
        return isStandaloneWorker() || isStaticWorker() || isElasticWorker();
    }

    protected final boolean isMaster() {
        return getRole() == Role.MASTER;
    }

    protected final void mustBe(Role... roles) {
        boolean matched = false;
        for (Role role : roles) {
            if (role == getRole()) {
                matched = true;
                break;
            }
        }
        Validate.isTrue(matched);
    }

    protected final void mustNotBe(Role... roles) {
        boolean matched = false;
        for (Role role : roles) {
            if (role == getRole()) {
                matched = true;
                break;
            }
        }
        Validate.isFalse(matched);
    }
}
