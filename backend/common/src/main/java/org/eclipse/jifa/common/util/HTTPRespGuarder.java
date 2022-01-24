/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.common.util;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceException;
import org.eclipse.jifa.common.Constant;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.JifaException;
import org.eclipse.jifa.common.vo.ErrorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

import static org.eclipse.jifa.common.util.GsonHolder.GSON;

public class HTTPRespGuarder implements Constant {

    private static final Logger LOGGER = LoggerFactory.getLogger(HTTPRespGuarder.class);

    public static void ok(io.vertx.reactivex.ext.web.RoutingContext context) {
        ok(context.getDelegate());
    }

    public static void ok(io.vertx.reactivex.ext.web.RoutingContext context, int statusCode, Object content) {
        ok(context.getDelegate(), statusCode, content);
    }

    public static void ok(io.vertx.reactivex.ext.web.RoutingContext context, Object content) {
        ok(context.getDelegate(), content);
    }

    public static void fail(io.vertx.reactivex.ext.web.RoutingContext context, Throwable t) {
        fail(context.getDelegate(), t);
    }

    public static void ok(RoutingContext context) {
        ok(context, commonStatusCodeOf(context.request().method()), null);
    }

    public static void ok(RoutingContext context, Object content) {
        ok(context, commonStatusCodeOf(context.request().method()), content);
    }

    private static void ok(RoutingContext context, int statusCode, Object content) {
        HttpServerResponse response = context.response();
        response.putHeader(Constant.HEADER_CONTENT_TYPE_KEY, Constant.CONTENT_TYPE_JSON_FORM).setStatusCode(statusCode);
        if (content != null) {
            response.end((content instanceof String) ? (String) content : GSON.toJson(content));
        } else {
            response.end();
        }
    }

    public static void fail(RoutingContext context, Throwable t) {
        if (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        log(t);
        context.response()
               .putHeader(Constant.HEADER_CONTENT_TYPE_KEY, Constant.CONTENT_TYPE_JSON_FORM)
               .setStatusCode(statusCodeOf(t))
               .end(GSON.toJson(new ErrorResult(t)));
    }

    private static int statusCodeOf(Throwable t) {
        ErrorCode errorCode = ErrorCode.UNKNOWN_ERROR;
        if (t instanceof JifaException) {
            errorCode = ((JifaException) t).getCode();
        }

        if (t instanceof IllegalArgumentException) {
            errorCode = ErrorCode.ILLEGAL_ARGUMENT;
        }

        if (errorCode == ErrorCode.ILLEGAL_ARGUMENT) {
            return HTTP_BAD_REQUEST_STATUS_CODE;
        }
        return HTTP_INTERNAL_SERVER_ERROR_STATUS_CODE;
    }

    private static void log(Throwable t) {
        boolean shouldLogError = true;

        if (t instanceof JifaException) {
            shouldLogError = ((JifaException) t).getCode().isFatal();
        }

        if ( t instanceof  ServiceException) {
            // FIXME: should we use ServiceException.failureCode?
            ServiceException se = (ServiceException) t;
            shouldLogError = se.failureCode() != ErrorCode.RETRY.ordinal();
            LOGGER.debug("Starting worker for target {}", se.getMessage());
        }

        if (shouldLogError) {
            LOGGER.error("Handle http request failed", t);
        }
    }

    private static int commonStatusCodeOf(HttpMethod method) {
        if (method == HttpMethod.POST) {
            return Constant.HTTP_POST_CREATED_STATUS_CODE;
        }
        return Constant.HTTP_GET_OK_STATUS_CODE;
    }
}
