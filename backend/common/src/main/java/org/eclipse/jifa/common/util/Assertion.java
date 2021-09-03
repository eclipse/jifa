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

import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.JifaException;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class Assertion {

    public static final Assertion ASSERT = new Assertion() {
    };

    protected Assertion() {
    }

    public Assertion isTrue(boolean expression, ErrorCode errorCode, Supplier<String> message) {
        if (!expression) {
            throwEx(errorCode, message.get());
        }
        return self();
    }

    public Assertion isTrue(boolean expression, ErrorCode errorCode, String message) {
        return isTrue(expression, errorCode, () -> message);
    }

    public Assertion isTrue(boolean expression, Supplier<String> message) {
        return isTrue(expression, ErrorCode.SANITY_CHECK, message);
    }

    public Assertion isTrue(boolean expression, String message) {
        return isTrue(expression, ErrorCode.SANITY_CHECK, message);
    }

    public Assertion isTrue(boolean expression, ErrorCode errorCode) {
        return isTrue(expression, errorCode, errorCode.name());
    }

    public Assertion isTrue(boolean expression) {
        return isTrue(expression, ErrorCode.SANITY_CHECK);
    }

    public Assertion equals(Object expected, Object actual, ErrorCode errorCode, String message) {
        return isTrue(Objects.equals(expected, actual), errorCode, message);
    }

    public Assertion equals(Object expected, Object actual, ErrorCode errorCode) {
        return equals(expected, actual, errorCode, errorCode.name());
    }

    public Assertion equals(Object expected, Object actual, String message) {
        return equals(expected, actual, ErrorCode.SANITY_CHECK, message);
    }

    public Assertion equals(Object expected, Object actual) {
        return equals(expected, actual, ErrorCode.SANITY_CHECK);
    }

    public Assertion notNull(Object object, ErrorCode errorCode, Supplier<String> message) {
        return isTrue(object != null, errorCode, message);
    }

    public Assertion notNull(Object object, Supplier<String> message) {
        return notNull(object, ErrorCode.SANITY_CHECK, message);
    }

    public Assertion notNull(Object object, String message) {
        return notNull(object, ErrorCode.SANITY_CHECK, () -> message);
    }

    public Assertion notNull(Object object) {
        return notNull(object, ErrorCode.SANITY_CHECK, ErrorCode.SANITY_CHECK::name);
    }

    private Assertion self() {
        return this;
    }

    protected void throwEx(ErrorCode errorCode, String message) {
        throw new JifaException(errorCode, message);
    }
}
