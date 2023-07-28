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
package org.eclipse.jifa.analysis;

public class TestArgumentResolverFactory {

//    public void method1() {
//
//    }
//
//    public void method2() {
//
//    }
//
//    public void method3(boolean b, int i, String s) {
//    }
//
//    public void method4(boolean b, int i, String s) {
//    }
//
//    public void method5(boolean b_, int i, String s) {
//    }
//
//    public void method6(long l) {
//    }
//
//    public void method7(long l) {
//    }
//
//    enum E {
//
//    }
//
//    public void method8(E e) {
//    }
//
//    public void method9(E e) {
//    }
//
//    public void method10(byte b) {
//    }
//
//    private ArgumentResolver resolverOf(String name) throws NoSuchMethodException {
//
//        for (Method method : TestArgumentResolverFactory.class.getMethods()) {
//            if (method.getName().equals(name)) {
//                return ArgumentResolverFactory.build(method);
//            }
//        }
//
//        throw new NoSuchMethodException(name);
//    }
//
//    @Test
//    public void test() throws NoSuchMethodException {
//
//        ArgumentResolver resolver1 = resolverOf("method1");
//        assertSame(ArgumentResolver.NO_ARGS, resolver1);
//        ArgumentResolver resolver2 = resolverOf("method2");
//        assertSame(resolver1, resolver2);
//
//        ArgumentResolver resolver3 = resolverOf("method3");
//        ArgumentResolver resolver4 = resolverOf("method4");
//        ArgumentResolver resolver5 = resolverOf("method5");
//        assertSame(resolver3, resolver4);
//        assertNotSame(resolver3, resolver5);
//
//        ArgumentResolver resolver6 = resolverOf("method6");
//        ArgumentResolver resolver7 = resolverOf("method7");
//        assertSame(resolver6, resolver7);
//
//        ArgumentResolver resolver8 = resolverOf("method8");
//        ArgumentResolver resolver9 = resolverOf("method9");
//        assertSame(resolver8, resolver9);
//
//        assertThrowsExactly(IllegalArgumentException.class, () -> resolverOf("method10"));
//    }
}
