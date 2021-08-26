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

import io.reactivex.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
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

    public static Single<HttpResponse<Buffer>> post(String hostIP, String uri, Map<String, String> params) {
        return send(HttpMethod.POST, hostIP, PORT, uri, params);
    }

    public static Single<HttpResponse<Buffer>> post(String hostIP, String uri) {
        return send(HttpMethod.POST, hostIP, PORT, uri, (Map<String, String>) null);
    }

    public static Single<HttpResponse<Buffer>> post(String hostIP, String uri, MultiMap params) {
        return send(HttpMethod.POST, hostIP, PORT, uri, params);
    }

    public static Single<HttpResponse<Buffer>> get(String hostIP, String uri) {
        return send(HttpMethod.GET, hostIP, PORT, uri, (Map<String, String>) null);
    }

    public static Single<HttpResponse<Buffer>> get(String hostIP, String uri, Map<String, String> params) {
        return send(HttpMethod.GET, hostIP, PORT, uri, params);
    }

    public static Single<HttpResponse<Buffer>> get(String hostIP, String uri, MultiMap params) {
        return send(HttpMethod.GET, hostIP, PORT, uri, params);
    }

    public static Single<HttpResponse<Buffer>> get(String hostIP, String uri, long timeout) {
        return send(HttpMethod.GET, hostIP, PORT, uri, null, timeout);
    }

    private static Single<HttpResponse<Buffer>> send(HttpMethod method, String hostIP, int port, String uri,
                                                     Map<String, String> params, long timeout) {
        HttpRequest<Buffer> request = request(method, hostIP, port, uri);
        if (params != null) {
            request.queryParams().addAll(params);
        }
        return request.basicAuthentication(USERNAME, PASSWORD).timeout(timeout).rxSend();
    }

    public static Single<HttpResponse<Buffer>> send(HttpServerRequest request, String hostIP) {
        return send(request, hostIP, PORT);
    }

    public static Single<HttpResponse<Buffer>> send(HttpServerRequest request, String hostIP, int port) {
        return send(request.method(), hostIP, port, request.uri(), request.params());
    }

    private static Single<HttpResponse<Buffer>> send(HttpMethod method, String hostIP, int port, String uri,
                                                     Map<String, String> params) {
        HttpRequest<Buffer> request = request(method, hostIP, port, uri);
        if (params != null) {
            request.queryParams().addAll(params);
        }
        return request.basicAuthentication(USERNAME, PASSWORD).rxSend();
    }

    private static Single<HttpResponse<Buffer>> send(HttpMethod method, String hostIP, int port, String uri,
                                                     MultiMap params) {
        HttpRequest<Buffer> request = request(method, hostIP, port, uri);
        if (params != null) {
            request.queryParams().addAll(params);
        }
        return request.basicAuthentication(USERNAME, PASSWORD).rxSend();
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

