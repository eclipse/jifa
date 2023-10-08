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
package org.eclipse.jifa.server.domain.converter;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.common.util.GsonHolder;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.domain.dto.AnalysisApiStompResponseMessage;
import org.eclipse.jifa.server.util.ErrorUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;

import static org.eclipse.jifa.server.Constant.STOMP_ANALYSIS_API_REQUEST_ID_KEY;
import static org.eclipse.jifa.server.Constant.STOMP_ANALYSIS_API_RESPONSE_SUCCESS_KEY;

@SuppressWarnings("NullableProblems")
@Slf4j
public class AnalysisApiStompResponseMessageConverter implements MessageConverter {

    private static final String CONTENT_TYPE = new MimeType("application", "json", Constant.CHARSET).toString();

    @Override
    public Object fromMessage(Message<?> message, Class<?> targetClass) {
        return null;
    }

    @Override
    public Message<?> toMessage(Object payload, MessageHeaders headers) {
        if (!(payload instanceof AnalysisApiStompResponseMessage message)) {
            return null;
        }

        byte[] bytes;

        if (message.throwable() == null) {
            bytes = toData(message.result());
        } else {
            bytes = ErrorUtil.toJson(message.throwable());
        }

        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.MESSAGE);

        if (headers != null) {
            headerAccessor.copyHeaders(headers);
        }

        headerAccessor.setNativeHeader(StompHeaders.CONTENT_TYPE, CONTENT_TYPE);

        if (message.requestId() != null && !message.requestId().isEmpty()) {
            headerAccessor.setNativeHeader(STOMP_ANALYSIS_API_REQUEST_ID_KEY, message.requestId());
        }

        headerAccessor.setNativeHeader(STOMP_ANALYSIS_API_RESPONSE_SUCCESS_KEY,
                                       Boolean.toString(message.throwable() == null));

        return MessageBuilder.createMessage(bytes, headerAccessor.getMessageHeaders());
    }

    private static byte[] toData(Object o) {
        if (o == null) {
            return Constant.EMPTY_BYTE_ARRAY;
        }
        try {
            return GsonHolder.GSON.toJson(o).getBytes(Constant.CHARSET);
        } catch (Throwable t) {
            log.error("Error occurred while converting response to data", t);
            return Constant.EMPTY_BYTE_ARRAY;
        }
    }
}
