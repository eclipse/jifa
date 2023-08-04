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

import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.condition.ConditionalOnRole;
import org.eclipse.jifa.server.domain.dto.AnalysisApiRequest;
import org.eclipse.jifa.server.domain.dto.AnalysisApiStompResponseMessage;
import org.eclipse.jifa.server.enums.Role;
import org.eclipse.jifa.server.service.AnalysisApiService;
import org.eclipse.jifa.server.util.ControllerUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeType;

import java.util.concurrent.CompletableFuture;

import static org.eclipse.jifa.server.Constant.STOMP_ANALYSIS_API_MAPPING;

@ConditionalOnRole({Role.MASTER, Role.STANDALONE_WORKER})
@Controller
public class AnalysisApiStompController {

    private final AnalysisApiService apiService;

    public AnalysisApiStompController(AnalysisApiService apiService) {
        this.apiService = apiService;
    }

    @MessageMapping(STOMP_ANALYSIS_API_MAPPING)
    @SendToUser(destinations = STOMP_ANALYSIS_API_MAPPING, broadcast = false)
    public CompletableFuture<AnalysisApiStompResponseMessage>
    handleStompRequest(@Header(name = StompHeaders.CONTENT_TYPE, required = false, defaultValue = Constant.APPLICATION_JSON) String contentType,
                       @Header(name = Constant.STOMP_ANALYSIS_API_REQUEST_ID_KEY, required = false, defaultValue = "") String requestId,
                       Message<byte[]> message) throws Throwable {
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
    }

    @MessageExceptionHandler
    @SendToUser(destinations = STOMP_ANALYSIS_API_MAPPING, broadcast = false)
    public AnalysisApiStompResponseMessage
    handleStompRequestException(Throwable throwable, @Header(name = Constant.STOMP_ANALYSIS_API_REQUEST_ID_KEY, required = false, defaultValue = "") String requestId) {
        throwable.printStackTrace();
        return new AnalysisApiStompResponseMessage(requestId, null, throwable);
    }
}
