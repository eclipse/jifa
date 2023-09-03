/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;


public class RouterAnnotationProcessorTest {
 
    private RoutingContext context;
    private HttpServerRequest request;
    private MultiMap params;

    @Before
    public void setup() {
        context = mock(RoutingContext.class);
        request = mock(HttpServerRequest.class);
        when(context.request()).thenReturn(request);
        params = mock(MultiMap.class);
        when(request.params()).thenReturn(params);
    }

    @Test
    public void testProcessListParam() throws Exception {
        List<String> paramList = Arrays.asList("a","b","c");
        when(params.getAll("testKey")).thenReturn(paramList);

        List<Object> arguments = new ArrayList<>();
        Method method = getClass().getMethod("testMethodString", List.class);
        Parameter parameter = method.getParameters()[0];
        
        RouterAnnotationProcessor.processListParam(arguments , context, method, parameter, parameter.getAnnotation(ParamKey.class));
        assertEquals(paramList, arguments.get(0));
    }

    @Test
    public void testProcessListInt() throws Exception {
        List<String> paramList = Arrays.asList("1","2","3");
        when(params.getAll("testKey")).thenReturn(paramList);

        List<Object> arguments = new ArrayList<>();
        Method method = getClass().getMethod("testMethodInt", List.class);
        Parameter parameter = method.getParameters()[0];
        
        RouterAnnotationProcessor.processListParam(arguments , context, method, parameter, parameter.getAnnotation(ParamKey.class));
        assertEquals(Arrays.asList(1,2,3), arguments.get(0));
    }

    @Test
    public void testProcessDefaultInt() throws Exception {
        List<Object> arguments = new ArrayList<>();
        Method method = getClass().getMethod("testMethodDefaultParam", Integer.TYPE);
        Parameter parameter = method.getParameters()[0];
        
        RouterAnnotationProcessor.processParamKey(arguments , context, method, parameter);
        assertEquals(100, arguments.get(0));
    }

    @Test
    public void testProcessDefaultIntSet() throws Exception {
        when(request.getParam("testKey")).thenReturn("5");
        List<Object> arguments = new ArrayList<>();
        Method method = getClass().getMethod("testMethodDefaultParam", Integer.TYPE);
        Parameter parameter = method.getParameters()[0];
        
        RouterAnnotationProcessor.processParamKey(arguments , context, method, parameter);
        assertEquals(5, arguments.get(0));
    }

    public static void testMethodString(@ParamKey("testKey") List<String> param) {

    }

    public static void testMethodInt(@ParamKey("testKey") List<Integer> param) {

    }

    public static void testMethodDefaultParam(@ParamKey(value = "testKey", mandatory = false, defaultValue = "100") int key) {

    }
}
