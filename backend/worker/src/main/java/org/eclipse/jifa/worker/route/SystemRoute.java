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
package org.eclipse.jifa.worker.route;

import io.vertx.core.Promise;
import org.eclipse.jifa.common.enums.StartStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
class SystemRoute extends BaseRoute {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemRoute.class);

    @RouteMeta(path = "/system/ping")
    void ping(Promise<StartStatus> promise) {
        promise.complete(StartStatus.SUCCESS);
    }
}
