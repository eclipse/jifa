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
package org.eclipse.jifa.server.controller;

import com.google.gson.JsonObject;
import org.eclipse.jifa.common.domain.exception.ShouldNotReachHereException;
import org.eclipse.jifa.common.util.ExecutorFactory;
import org.eclipse.jifa.common.util.Validate;
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.domain.dto.AnalysisApiRequest;
import org.eclipse.jifa.server.service.AnalysisApiService;
import org.eclipse.jifa.server.util.ControllerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;
import static org.eclipse.jifa.common.util.GsonHolder.GSON;
import static org.eclipse.jifa.server.Constant.ANALYSIS_API_REQUEST_API_KEY;
import static org.eclipse.jifa.server.Constant.ANALYSIS_API_REQUEST_NAMESPACE_KEY;
import static org.eclipse.jifa.server.Constant.ANALYSIS_API_REQUEST_TARGET_KEY;

@RestController
public class AnalysisApiHttpController extends ConfigurationAccessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AnalysisApiService apiService;

    private final ScheduledExecutorService scheduledExecutorServiceForSseHeartbeat;

    public AnalysisApiHttpController(AnalysisApiService apiService) {
        this.apiService = apiService;
        this.scheduledExecutorServiceForSseHeartbeat = ExecutorFactory.newScheduledExecutorService("SSE Heartbeat Sender", 1);
    }

    @RequestMapping(path = Constant.HTTP_ANALYSIS_API_MAPPING,
            method = {RequestMethod.POST},
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = Constant.APPLICATION_JSON)
    public Object handleHttpRequest(@RequestHeader(name = HttpHeaders.CONTENT_TYPE) String contentType,
                                    @RequestHeader(name = Constant.HTTP_HEADER_ENABLE_SSE, required = false, defaultValue = "false") boolean enableSse,
                                    @RequestBody byte[] body) throws Throwable {

        JsonObject request = GSON.fromJson(new String(body, ofNullable(MimeTypeUtils.parseMimeType(contentType).getCharset()).orElse(Constant.CHARSET)),
                                           JsonObject.class);
        return postProcess(apiService.invoke(new AnalysisApiRequest(request)), enableSse);
    }

    @Deprecated
    @RequestMapping(path = "/{*api}",
            method = {RequestMethod.POST, RequestMethod.GET},
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = Constant.APPLICATION_JSON)
    public Object handleHttpRequest(
            @PathVariable String api,
            HttpMethod method,
            @RequestHeader(name = HttpHeaders.CONTENT_TYPE, required = false, defaultValue = Constant.APPLICATION_JSON) String contentType,
            @RequestHeader(name = Constant.HTTP_HEADER_ENABLE_SSE, required = false, defaultValue = "false") boolean enableSse,
            @RequestParam(required = false) MultiValueMap<String, String> params,
            @RequestBody(required = false) byte[] body) throws Throwable {

        if (api == null || !api.startsWith("/")) {
            throw new IllegalArgumentException("Unsupported api: " + api);
        }

        api = api.substring(1);

        JsonObject args = null;
        if (method == HttpMethod.GET) {
            if (params != null && params.size() > 0) {
                args = ControllerUtil.convertMultiValueMapToJsonObject(params);
            }
        } else if (method == HttpMethod.POST) {
            MimeType mimeType = ControllerUtil.checkMimeTypeForPostRequest(contentType);
            args = ControllerUtil.parseArgs(mimeType, body);
        } else {
            throw new ShouldNotReachHereException();
        }

        Validate.isTrue(isDeprecatedFormat(api));

        return postProcess(apiService.invoke(new AnalysisApiRequest(processDeprecatedFormat(api, args))), enableSse);
    }

    private Object postProcess(CompletableFuture<?> future, boolean enableSse) {
        if (!enableSse) {
            return future;
        }

        ExtendedSseEmitter emitter = new ExtendedSseEmitter();
        emitter.enableHeartbeat();
        future.whenComplete((r, t) -> {
            try {
                emitter.disableHeartbeat();
                if (t == null) {
                    emitter.send(SseEmitter.event().name(Constant.SSE_EVENT_SUCCESS_RESPONSE).data(r, MediaType.APPLICATION_JSON));
                } else {
                    emitter.send(SseEmitter.event().name(Constant.SSE_EVENT_ERROR_RESPONSE).data(r, MediaType.APPLICATION_JSON));
                }
            } catch (Throwable throwable) {
                LOGGER.error("Error occurred when sending response to SSE emitter", throwable);
            } finally {
                emitter.complete();
            }
        });
        return emitter;
    }

    private boolean isDeprecatedFormat(String api) {
        return api.startsWith("heap-dump/") || api.startsWith("gc-log/") || api.startsWith("thread-dump/");
    }

    private JsonObject processDeprecatedFormat(String api, JsonObject args) {
        JsonObject request = new JsonObject();

        String[] elements = api.split("/");
        Validate.isTrue(elements.length > 2);
        request.addProperty(ANALYSIS_API_REQUEST_NAMESPACE_KEY, elements[0]);
        request.addProperty(ANALYSIS_API_REQUEST_TARGET_KEY, elements[1]);
        StringBuilder realApi = new StringBuilder(elements[2]);
        for (int i = 3; i < elements.length; i++) {
            realApi.append(".").append(elements[i]);
        }
        request.addProperty(ANALYSIS_API_REQUEST_API_KEY, realApi.toString());

        if (args != null) {
            request.add(Constant.ANALYSIS_API_REQUEST_PARAMETERS_KEY, args);
        }
        return request;
    }

    private class ExtendedSseEmitter extends SseEmitter implements Runnable {

        public ExtendedSseEmitter() {
            super(Long.MAX_VALUE);
        }

        private ScheduledFuture<?> future;

        private boolean shouldHeartbeat;

        @Override
        public synchronized void run() {
            if (shouldHeartbeat) {
                try {
                    send(SseEmitter.event().name(Constant.SSE_EVENT_PING));
                } catch (Throwable t) {
                    future.cancel(true);
                }
            }
        }

        void enableHeartbeat() {
            shouldHeartbeat = true;
            future = scheduledExecutorServiceForSseHeartbeat.scheduleAtFixedRate(this, 30, 30, TimeUnit.SECONDS);
        }

        synchronized void disableHeartbeat() {
            shouldHeartbeat = false;
            if (!future.cancel(true)) {
                LOGGER.error("Failed to cancel heartbeat task");
            }
        }
    }
}
