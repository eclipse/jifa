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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.common.domain.exception.ErrorCodeAccessor;
import org.eclipse.jifa.common.domain.exception.ValidationException;
import org.eclipse.jifa.server.domain.exception.ElasticWorkerNotReadyException;
import org.eclipse.jifa.server.enums.ServerErrorCode;
import org.eclipse.jifa.server.util.ErrorUtil;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.nio.file.AccessDeniedException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler
    @ResponseBody
    public void handleHttpRequestException(Throwable throwable, HttpServletRequest request, HttpServletResponse response) throws IOException {
        log(throwable, request);

        if (throwable instanceof WebClientResponseException e) {
            response.setStatus(e.getStatusCode().value());
            response.getOutputStream().write(e.getResponseBodyAsByteArray());
            return;
        }
        response.setStatus(getStatusOf(throwable));
        response.getOutputStream().write(ErrorUtil.toJson(throwable));
    }

    private void log(Throwable throwable, HttpServletRequest request) {
        if (throwable instanceof ElasticWorkerNotReadyException) {
            return;
        }

        if (throwable instanceof MissingServletRequestParameterException ||
            throwable instanceof IllegalArgumentException ||
            throwable instanceof AuthenticationException ||
            throwable instanceof ValidationException ||
            throwable instanceof WebClientResponseException) {
            log.error(throwable.getMessage());
        } else {
            log.error("Error occurred when handling http request '{}'", request.getRequestURI(), throwable);
        }
    }

    private int getStatusOf(Throwable throwable) {
        if (throwable instanceof MissingServletRequestParameterException) {
            return 400;
        }
        if (throwable instanceof AuthenticationException || throwable instanceof AccessDeniedException) {
            return 401;
        }
        if (throwable instanceof ErrorCodeAccessor errorCodeAccessor) {
            if (ServerErrorCode.ACCESS_DENIED == errorCodeAccessor.getErrorCode()) {
                return 401;
            }
        }
        return 500;
    }
}
