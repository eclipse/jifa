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
import io.vertx.reactivex.ext.web.multipart.MultipartForm;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.master.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

import static org.eclipse.jifa.master.Constant.*;

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
        return send(HttpMethod.POST, hostIP, PORT, uri, (MultiMap) null);
    }

    public static Single<HttpResponse<Buffer>> post(String hostIP, String uri, MultiMap params) {
        return send(HttpMethod.POST, hostIP, PORT, uri, params);
    }

    public static Single<HttpResponse<Buffer>> get(String hostIP, String uri) {
        return send(HttpMethod.GET, hostIP, PORT, uri, (MultiMap) null);
    }

    public static Single<HttpResponse<Buffer>> get(String hostIP, String uri, Map<String, String> params) {
        return send(HttpMethod.GET, hostIP, PORT, uri, params);
    }

    public static Single<HttpResponse<Buffer>> get(String hostIP, String uri, MultiMap params) {
        return send(HttpMethod.GET, hostIP, PORT, uri, params);
    }

    public static Single<HttpResponse<Buffer>> send(HttpServerRequest request, String hostIP) {
        return send(request, hostIP, PORT);
    }

    public static Single<HttpResponse<Buffer>> send(HttpServerRequest request, String hostIP, int port) {
        return send(request.method(), hostIP, port, request.uri(), request.params());
    }

    private static Single<HttpResponse<Buffer>> send(HttpMethod method, String hostIP, int port, String uri,
                                                     Map<String, String> params) {
        return send(method, hostIP, port, uri, MultiMap.caseInsensitiveMultiMap().addAll(params));
    }

    private static Single<HttpResponse<Buffer>> send(HttpMethod method, String hostIP, int port, String uri,
                                                     MultiMap params) {
        return send(request(method, hostIP, port, uri), method == HttpMethod.POST, params);
    }

    public static Single<HttpResponse<Buffer>> send(HttpRequest<Buffer> request, boolean post, MultiMap params) {
        request.basicAuthentication(USERNAME, PASSWORD);
        if (post) {
            if (params == null) {
                return request.rxSend();
            }
            return request.rxSendForm(params);
        }
        if (params != null) {
            request.queryParams().addAll(params);
        }
        return request.rxSend();
    }

    public static Single<HttpResponse<Buffer>> uploadFile(String hostIp, File file, String name, FileType type) {
        HttpRequest<Buffer> request = request(HttpMethod.POST, hostIp, PORT, uri(FILE_UPLOAD));
        MultipartForm formDataParts = MultipartForm.create();
        formDataParts.attribute("fileName", name)
                     .attribute("type", type.name())
                     .binaryFileUpload(file.getName(), file.getName(), file.getPath(), "application/octet-stream");
        return request.rxSendMultipartForm(formDataParts);
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

    public static HttpRequest<Buffer> request(HttpMethod method, String hostIP, String uri) {
        return request(method, hostIP, PORT, uri);
    }

}

