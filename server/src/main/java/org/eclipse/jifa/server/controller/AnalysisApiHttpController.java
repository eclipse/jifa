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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.common.util.ExecutorFactory;
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.domain.dto.AnalysisApiRequest;
import org.eclipse.jifa.server.service.AnalysisApiService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;
import static org.eclipse.jifa.common.util.GsonHolder.GSON;

@Slf4j
@RestController
public class AnalysisApiHttpController extends ConfigurationAccessor {

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
    public Object handleRequest(@RequestHeader(name = HttpHeaders.CONTENT_TYPE) String contentType,
                                @RequestHeader(name = Constant.HTTP_HEADER_ENABLE_SSE, required = false, defaultValue = "false") boolean enableSse,
                                @RequestBody byte[] body) {

        JsonObject request = GSON.fromJson(new String(body, ofNullable(MimeTypeUtils.parseMimeType(contentType).getCharset()).orElse(Constant.CHARSET)),
                                           JsonObject.class);
        return postProcess(apiService.invoke(new AnalysisApiRequest(request)), enableSse);
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
                log.error("Error occurred when sending response to SSE emitter", throwable);
            } finally {
                emitter.complete();
            }
        });
        return emitter;
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
                log.error("Failed to cancel heartbeat task");
            }
        }
    }
}
