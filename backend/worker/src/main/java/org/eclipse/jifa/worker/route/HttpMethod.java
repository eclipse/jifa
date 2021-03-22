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

public enum HttpMethod {
    GET(io.vertx.core.http.HttpMethod.GET),
    POST(io.vertx.core.http.HttpMethod.POST);

    private final io.vertx.core.http.HttpMethod method;

    HttpMethod(io.vertx.core.http.HttpMethod method) {
        this.method = method;
    }

    public io.vertx.core.http.HttpMethod toVertx() {
        return method;
    }
}
