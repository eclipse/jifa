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
package org.eclipse.jifa.worker.route;

import com.google.gson.Gson;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.JifaException;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.util.HTTPRespGuarder;
import org.eclipse.jifa.gclog.diagnoser.AnalysisConfig;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class RouterAnnotationProcessor {
    private static Map<Class<?>, Function<String, ?>> converter = new HashMap<>();

    static {
        converter.put(String.class, s -> s);
        converter.put(Integer.class, Integer::parseInt);
        converter.put(int.class, Integer::parseInt);
        converter.put(Long.class, Long::parseLong);
        converter.put(long.class, Long::parseLong);
        converter.put(Float.class, Float::parseFloat);
        converter.put(float.class, Float::parseFloat);
        converter.put(Double.class, Double::parseDouble);
        converter.put(double.class, Double::parseDouble);
        converter.put(Boolean.class, Boolean::parseBoolean);
        converter.put(boolean.class, Boolean::parseBoolean);
        converter.put(int[].class, s -> new Gson().fromJson(s, int[].class));
        converter.put(String[].class, s -> new Gson().fromJson(s, String[].class));
        converter.put(AnalysisConfig.class, s -> new Gson().fromJson(s, AnalysisConfig.class));
    }

    static boolean processParamKey(List<Object> arguments, RoutingContext context, Method method, Parameter param) {
        // param key
        ParamKey paramKey = param.getAnnotation(ParamKey.class);
        if (paramKey != null) {
            String value = context.request().getParam(paramKey.value());
            ASSERT.isTrue(!paramKey.mandatory() || value != null, ErrorCode.ILLEGAL_ARGUMENT,
                          () -> "Miss request parameter, key = " + paramKey.value());
            arguments.add(value != null ? convert(method, param, context.request().getParam(paramKey.value())) : null);
            return true;
        }
        return false;
    }

    static boolean processParamMap(List<Object> arguments, RoutingContext context, Method method, Parameter param) {
        ParamMap paramMap = param.getAnnotation(ParamMap.class);
        if (paramMap != null) {
            Map<String, String> map = new HashMap<>();
            String[] keys = paramMap.keys();
            boolean[] mandatory = paramMap.mandatory();
            for (int j = 0; j < keys.length; j++) {
                String key = keys[j];
                String value = context.request().getParam(key);
                ASSERT.isTrue(!mandatory[j] || value != null, ErrorCode.ILLEGAL_ARGUMENT,
                              () -> "Miss request parameter, key = " + key);
                if (value != null) {
                    map.put(key, value);
                }
            }
            arguments.add(map);
            return true;
        }
        return false;
    }

    static boolean processPagingRequest(List<Object> arguments, RoutingContext context, Method method,
                                        Parameter param) {
        if (param.getType() == PagingRequest.class) {
            int page;
            int pageSize;
            try {
                page = Integer.parseInt(context.request().getParam("page"));
                pageSize = Integer.parseInt(context.request().getParam("pageSize"));
                ASSERT.isTrue(page >= 1 && pageSize >= 1, ErrorCode.ILLEGAL_ARGUMENT,
                              "must greater than 1");
            } catch (Exception e) {
                throw new JifaException(ErrorCode.ILLEGAL_ARGUMENT, "Paging parameter(page or pageSize) is illegal, " +
                                                                    e.getMessage());
            }
            arguments.add(new PagingRequest(page, pageSize));
            return true;
        }
        return false;
    }

    static boolean processHttpServletRequest(List<Object> arguments, RoutingContext context, Method method,
                                             Parameter param) {
        if (param.getType().equals(HttpServerRequest.class)) {
            arguments.add(context.request());
            return true;
        }
        return false;
    }

    static boolean processHttpServletResponse(List<Object> arguments, RoutingContext context, Method method,
                                              Parameter param) {
        if (param.getType().equals(HttpServerResponse.class)) {
            arguments.add(context.response());
            return true;
        }
        return false;
    }

    static boolean processPromise(List<Object> arguments, RoutingContext context, Method method, Parameter param) {
        if (param.getType().equals(Promise.class)) {
            arguments.add(newPromise(context));
            return true;
        }
        return false;
    }

    static boolean processRoutingContext(List<Object> arguments, RoutingContext context, Method method,
                                         Parameter param) {
        if (param.getType().equals(RoutingContext.class)) {
            arguments.add(context);
            return true;
        }
        return false;
    }

    private static Object convert(Method m, Parameter p, String value) {
        Class<?> type = p.getType();
        Function<String, ?> f;
        if (type.isEnum()) {
            f = s -> {
                for (Object e : type.getEnumConstants()) {
                    if (((Enum) e).name().equalsIgnoreCase(value)) {
                        return e;
                    }
                }
                throw new JifaException(ErrorCode.ILLEGAL_ARGUMENT,
                                        "Illegal parameter value, parameter = " + p + ", value = " + value);
            };
        } else {
            f = converter.get(type);
        }
        ASSERT.notNull(f, () -> "Unsupported parameter type, method = " + m + ", parameter = " + p);
        return f.apply(value);
    }


    private static <T> Promise<T> newPromise(io.vertx.ext.web.RoutingContext rc) {
        Promise<T> promise = Promise.promise();
        promise.future().onComplete(
            event -> {
                if (event.succeeded()) {
                    HTTPRespGuarder.ok(rc, event.result());
                } else {
                    HTTPRespGuarder.fail(rc, event.cause());
                }
            }
        );
        return promise;
    }
}