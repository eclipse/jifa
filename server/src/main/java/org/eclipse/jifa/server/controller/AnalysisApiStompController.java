/********************************************************************************
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.condition.ConditionalOnRole;
import org.eclipse.jifa.server.domain.dto.AnalysisApiRequest;
import org.eclipse.jifa.server.domain.dto.AnalysisApiStompResponseMessage;
import org.eclipse.jifa.server.domain.exception.ElasticWorkerNotReadyException;
import org.eclipse.jifa.server.enums.Role;
import org.eclipse.jifa.server.service.AnalysisApiService;
import org.eclipse.jifa.server.util.ControllerUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeType;

import java.util.concurrent.CompletableFuture;

import static org.eclipse.jifa.server.Constant.STOMP_ANALYSIS_API_MAPPING;

@ConditionalOnRole({Role.MASTER, Role.STANDALONE_WORKER})
@Controller
@Slf4j
public class AnalysisApiStompController {

    private final AnonymousAuthenticationToken ANONYMOUS = new AnonymousAuthenticationToken(Constant.ANONYMOUS_KEY,
                                                                                            Constant.ANONYMOUS_USERNAME,
                                                                                            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

    private final AnalysisApiService apiService;

    public AnalysisApiStompController(AnalysisApiService apiService) {
        this.apiService = apiService;
    }

    @MessageMapping(STOMP_ANALYSIS_API_MAPPING)
    @SendToUser(destinations = STOMP_ANALYSIS_API_MAPPING, broadcast = false)
    public CompletableFuture<AnalysisApiStompResponseMessage>
    handleRequest(@Header(name = StompHeaders.CONTENT_TYPE, required = false, defaultValue = Constant.APPLICATION_JSON) String contentType,
                  @Header(name = Constant.STOMP_ANALYSIS_API_REQUEST_ID_KEY, required = false, defaultValue = "") String requestId,
                  Message<byte[]> message) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        assert accessor != null;
        Authentication auth = (Authentication) accessor.getUser();
        SecurityContextHolder.getContext().setAuthentication(auth != null ? auth : ANONYMOUS);

        try {
            MimeType mimeType = ControllerUtil.checkMimeTypeForStompMessage(contentType);
            CompletableFuture<AnalysisApiStompResponseMessage> responseMessage = new CompletableFuture<>();
            apiService.invoke(new AnalysisApiRequest(ControllerUtil.parseArgs(mimeType, message.getPayload())))
                      .whenComplete((r, t) -> {
                          if (t == null) {
                              responseMessage.complete(new AnalysisApiStompResponseMessage(requestId, r, null));
                          } else {
                              responseMessage.complete(new AnalysisApiStompResponseMessage(requestId, null, t));
                          }
                      });
            return responseMessage;
        } finally {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }

    @MessageExceptionHandler
    @SendToUser(destinations = STOMP_ANALYSIS_API_MAPPING, broadcast = false)
    public AnalysisApiStompResponseMessage
    handleRequestException(Throwable throwable, @Header(name = Constant.STOMP_ANALYSIS_API_REQUEST_ID_KEY, required = false, defaultValue = "") String requestId) {
        log(throwable);
        return new AnalysisApiStompResponseMessage(requestId, null, throwable);
    }

    private void log(Throwable throwable) {
        if (throwable instanceof ElasticWorkerNotReadyException) {
            return;
        }
        log.error("Error occurred when handling stomp request", throwable);
    }
}
