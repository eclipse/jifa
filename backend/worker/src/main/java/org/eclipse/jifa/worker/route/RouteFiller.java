/********************************************************************************
 * Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation
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

import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.JifaException;
import org.eclipse.jifa.common.util.HTTPRespGuarder;
import org.eclipse.jifa.worker.route.gclog.GCLogBaseRoute;
import org.eclipse.jifa.worker.Constant;
import org.eclipse.jifa.worker.WorkerGlobal;
import org.eclipse.jifa.worker.route.heapdump.HeapBaseRoute;
import org.eclipse.jifa.worker.route.threaddump.ThreadDumpBaseRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RouteFiller {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteFiller.class);

    private Router router;

    public RouteFiller(Router router) {
        this.router = router;
    }

    public void fill() {
        try {
            register(FileRoute.class);
            register(AnalysisRoute.class);
            register(SystemRoute.class);

            for (Class<? extends HeapBaseRoute> route : HeapBaseRoute.routes()) {
                register(route);
            }
            for (Class<? extends GCLogBaseRoute> route: GCLogBaseRoute.routes()){
                register(route);
            }
            for (Class<? extends ThreadDumpBaseRoute> route: ThreadDumpBaseRoute.routes()){
                register(route);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String[] buildPrefixes(Class<?> clazz) {
        ArrayList<String> prefixes = new ArrayList<>();
        buildPrefix(prefixes, "", clazz);
        return prefixes.toArray(new String[0]);
    }

    private void buildPrefix(ArrayList<String> prefixes, String prevPrefix, Class<?> clazz) {
        if (clazz == null) {
            String rootPrefix = WorkerGlobal.stringConfig(Constant.ConfigKey.API_PREFIX);
            prefixes.add(rootPrefix + prevPrefix);
            return;
        }

        MappingPrefix anno = clazz.getDeclaredAnnotation(MappingPrefix.class);
        if (anno == null) {
            buildPrefix(prefixes, prevPrefix, clazz.getSuperclass());
        } else {
            for (int i = 0; i < anno.value().length; i++) {
                buildPrefix(prefixes, anno.value()[i] + prevPrefix, clazz.getSuperclass());
            }
        }
    }

    private void register(Class<? extends BaseRoute> clazz) throws Exception {
        Constructor<? extends BaseRoute> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        BaseRoute thisObject = constructor.newInstance();

        String[] prefixes = buildPrefixes(clazz);
        Method[] methods = clazz.getDeclaredMethods();
        for (String prefix : prefixes) {
            for (Method method : methods) {
                registerMethodRoute(thisObject, prefix, method);
            }
        }
    }

    private void registerMethodRoute(BaseRoute thisObject, String prefix, Method method) {
        RouteMeta meta = method.getAnnotation(RouteMeta.class);
        if (meta == null) {
            return;
        }

        String fullPath = prefix + meta.path();
        Route route = router.route(meta.method().toVertx(), fullPath);
        Arrays.stream(meta.contentType()).forEach(route::produces);
        method.setAccessible(true);

        LOGGER.debug("Route: path = {}, method = {}", fullPath, method.toGenericString());

        route.blockingHandler(rc -> {
            try {
                // pre-process
                if (meta.contentType().length > 0) {
                    rc.response().putHeader("content-type", String.join(";", meta.contentType()));
                }

                List<Object> arguments = new ArrayList<>();
                Parameter[] params = method.getParameters();
                for (Parameter param : params) {
                    if (!RouterAnnotationProcessor.processParamKey(arguments, rc, method, param) &&
                        !RouterAnnotationProcessor.processParamMap(arguments, rc, method, param) &&
                        !RouterAnnotationProcessor.processPagingRequest(arguments, rc, method, param) &&
                        !RouterAnnotationProcessor.processHttpServletRequest(arguments, rc, method, param) &&
                        !RouterAnnotationProcessor.processHttpServletResponse(arguments, rc, method, param) &&
                        !RouterAnnotationProcessor.processPromise(arguments, rc, method, param) &&
                        !RouterAnnotationProcessor.processRoutingContext(arguments, rc, method, param)
                    ) {
                        throw new JifaException(ErrorCode.ILLEGAL_ARGUMENT,
                                                "Illegal parameter meta, method = " + method);
                    }

                }
                method.invoke(thisObject, arguments.toArray());
            } catch (Throwable t) {
                HTTPRespGuarder.fail(rc, t);
            }
        }, false);
    }
}
