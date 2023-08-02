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
package org.eclipse.jifa.server.service.impl;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.domain.dto.HttpRequestToWorker;
import org.eclipse.jifa.server.domain.entity.cluster.WorkerEntity;
import org.eclipse.jifa.server.domain.support.JsonConvertible;
import org.eclipse.jifa.server.service.UserService;
import org.eclipse.jifa.server.service.WorkerService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.eclipse.jifa.common.util.GsonHolder.GSON;
import static org.eclipse.jifa.server.Constant.HTTP_API_PREFIX;

public abstract class AbstractWorkerServiceImpl extends ConfigurationAccessor implements WorkerService {

    protected final UserService userService;
    protected final WebClient webClient;

    protected AbstractWorkerServiceImpl(UserService userService) {
        this.userService = userService;

        HttpClient httpClient = HttpClient.create()
                                          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 16000)
                                          .doOnConnected(conn -> conn
                                                  .addHandlerLast(new ReadTimeoutHandler(Long.MAX_VALUE, TimeUnit.SECONDS))
                                                  .addHandlerLast(new WriteTimeoutHandler(Long.MAX_VALUE, TimeUnit.SECONDS)));
        webClient = WebClient.builder()
                             .clientConnector(new ReactorClientHttpConnector(httpClient))
                             .build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Response> CompletableFuture<Response> sendRequest(WorkerEntity worker, HttpRequestToWorker<Response> request) {
        UriBuilder uriBuilder = new DefaultUriBuilderFactory().builder()
                                                              .scheme("http")
                                                              .host(worker.getHostAddress())
                                                              .port(worker.getPort())
                                                              .path(HTTP_API_PREFIX + "/" + request.uri());

        HttpMethod method = request.method();
        if (method == HttpMethod.GET && request.query() != null) {
            uriBuilder.queryParams(request.query());
        }

        WebClient.RequestBodySpec spec = webClient.method(method)
                                                  .uri(uriBuilder.build())
                                                  .accept(MediaType.APPLICATION_JSON);

        String jwtToken = userService.getCurrentUserJwtTokenOrNull();

        if (jwtToken != null) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken);
        }

        if (request.body() != null) {
            Object body = request.body();
            String bodyJson;
            if (body instanceof JsonConvertible convertible) {
                bodyJson = convertible.toJson();
            } else {
                bodyJson = GSON.toJson(body);
            }
            byte[] bytes = bodyJson.getBytes(Constant.CHARSET);
            spec.contentType(MediaType.APPLICATION_JSON)
                .body((BodyInserter<byte[], ClientHttpRequest>) (message, context) -> {
                    DataBuffer buffer = message.bufferFactory().wrap(bytes);
                    message.getHeaders().setContentLength(bytes.length);
                    return message.writeWith(Mono.just(buffer));
                });
        }

        Class<Response> responseClass = request.responseType();
        return spec.exchangeToMono(response -> {
            if (!response.statusCode().is2xxSuccessful()) {
                return response.createError();
            }

            if (responseClass == Void.class) {
                return Mono.empty();
            }

            if (responseClass == byte[].class) {
                return (Mono<Response>) response.bodyToMono(byte[].class)
                                                .defaultIfEmpty(Constant.EMPTY_BYTE_ARRAY);
            }

            return response.bodyToMono(String.class)
                           .map(s -> GSON.fromJson(s, responseClass));
        }).toFuture();
    }
}
