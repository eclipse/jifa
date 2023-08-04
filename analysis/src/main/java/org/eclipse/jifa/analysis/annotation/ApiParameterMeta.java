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
 * The parameter meta information of an analysis api
 */
@Retention(value = RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ApiParameterMeta {

    /**
     * The parameter name will be used if empty
     *
     * @return parameter name
     */
    String value() default "";

    /**
     * @return true if this parameter is required, default is true
     */
    boolean required() default true;

    /**
     * @return true if this parameter is the path of the analysis target
     */
    boolean targetPath() default false;

    /**
     * @return true if this parameter is the path of a comparison analysis target
     */
    boolean comparisonTargetPath() default false;
}
