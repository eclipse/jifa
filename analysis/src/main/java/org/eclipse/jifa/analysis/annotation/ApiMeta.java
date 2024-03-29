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
package org.eclipse.jifa.analysis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Analysis api meta information
 */
@Retention(value = RUNTIME)
@Target(ElementType.METHOD)
public @interface ApiMeta {

    /**
     * The method name will be used if empty
     *
     * @return api name
     */
    String value() default "";

    /**
     * @return aliases of this api
     */
    String[] aliases() default {};
}
