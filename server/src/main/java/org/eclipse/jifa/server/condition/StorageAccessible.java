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
package org.eclipse.jifa.server.condition;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;

public class StorageAccessible extends AnyNestedCondition {

    public StorageAccessible() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }

    @SuppressWarnings("unused")
    @Master
    @ElasticSchedulingStrategy
    static class MasterWithElasticSchedulingStrategy {
    }

    @SuppressWarnings("unused")
    @org.eclipse.jifa.server.condition.Worker
    static class Worker {
    }
}
