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
package org.eclipse.jifa.master.support;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.eclipse.jifa.master.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class WorkerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerClient.class);

    private static String USERNAME;

    private static String PASSWORD;

    private static int PORT;

    private static WebClient client;

    public static void init(JsonObject config, WebClient client) {
        USERNAME = config.getString(Constant.USERNAME);
        PASSWORD = config.getString(Constant.PASSWORD);
        PORT = config.getInteger(Constant.PORT);
        WorkerClient.client = client;
    }

    public static void post(String hostIP, String uri, Map<String, String> params, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
        send(HttpMethod.POST, hostIP, PORT, uri, params, handler);
    }

    public static void post(String hostIP, String uri, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
        send(HttpMethod.POST, hostIP, PORT, uri, (Map<String, String>) null, handler);
    }

    public static void post(String hostIP, String uri, MultiMap params, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
        send(HttpMethod.POST, hostIP, PORT, uri, params, handler);
    }

    public static void get(String hostIP, String uri, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
        send(HttpMethod.GET, hostIP, PORT, uri, (Map<String, String>) null, handler);
    }

    public static void get(String hostIP, String uri, Map<String, String> params, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
        send(HttpMethod.GET, hostIP, PORT, uri, params, handler);
    }

    public static void get(String hostIP, String uri, MultiMap params, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
        send(HttpMethod.GET, hostIP, PORT, uri, params, handler);
    }

    public static void send(HttpServerRequest request, String hostIP, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
        send(request.method(), hostIP, PORT, request.uri(), request.params(), handler);
    }


    private static void send(HttpMethod method, String hostIP, int port, String uri,
                             Map<String, String> params, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
        HttpRequest<Buffer> request = request(method, hostIP, port, uri);
        if (params != null) {
            request.queryParams().addAll(params);
        }
        request.basicAuthentication(USERNAME, PASSWORD).send(handler);
    }

    private static void send(HttpMethod method, String hostIP, int port, String uri,
                             MultiMap params, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
        HttpRequest<Buffer> request = request(method, hostIP, port, uri);
        if (params != null) {
            request.queryParams().addAll(params);
        }
        request.basicAuthentication(USERNAME, PASSWORD).send(handler);
    }

    private static HttpRequest<Buffer> request(HttpMethod method, String hostIP, int port, String uri) {
        if (method == HttpMethod.GET) {
            return client.get(port, hostIP, uri);
        } else if (method == HttpMethod.POST) {
            return client.post(port, hostIP, uri);

        }
        LOGGER.error("Unsupported worker http request method {}", method);
        throw new IllegalArgumentException();
    }
}

