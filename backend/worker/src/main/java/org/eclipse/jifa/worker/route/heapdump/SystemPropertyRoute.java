/********************************************************************************
 * Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.worker.route.heapdump;

import io.vertx.core.Promise;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;

import java.util.Map;

class SystemPropertyRoute extends HeapBaseRoute {

    @RouteMeta(path = "/systemProperties")
    void systemProperty(Promise<Map<String, String>> promise, @ParamKey("file") String file) {
        promise.complete(analyzerOf(file).getSystemProperties());
    }
}
