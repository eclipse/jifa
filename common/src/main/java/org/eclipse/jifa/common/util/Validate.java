/********************************************************************************
 * Copyright (c) 2020, 2023 Contributors to the Eclipse Foundation
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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jifa.common.domain.exception.ErrorCode;
import org.eclipse.jifa.common.domain.exception.ValidationException;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class Validate {

    public static void isTrue(boolean expression, ErrorCode errorCode, Supplier<String> message) {
        if (!expression) {
            error(errorCode, message.get());
        }
    }

    public static void isTrue(boolean expression, ErrorCode errorCode, String message) {
        if (!expression) {
            error(errorCode, message);
        }
    }

    public static void isTrue(boolean expression, ErrorCode errorCode) {
        if (!expression) {
            error(errorCode);
        }
    }

    public static void isTrue(boolean expression, Supplier<String> message) {
        if (!expression) {
            error(message.get());
        }
    }

    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            error(message);
        }
    }

    public static void isTrue(boolean expression) {
        if (!expression) {
            error();
        }
    }

    public static void isFalse(boolean expression, ErrorCode errorCode, Supplier<String> message) {
        isTrue(!expression, errorCode, message);
    }

    public static void isFalse(boolean expression, ErrorCode errorCode, String message) {
        isTrue(!expression, errorCode, message);
    }

    public static void isFalse(boolean expression, ErrorCode errorCode) {
        isTrue(!expression, errorCode);
    }

    public static void isFalse(boolean expression, Supplier<String> message) {
        isTrue(!expression, message);
    }

    public static void isFalse(boolean expression, String message) {
        isTrue(!expression, message);
    }

    public static void isFalse(boolean expression) {
        isTrue(!expression);
    }

    public static void equals(Object expected, Object actual, ErrorCode errorCode, Supplier<String> message) {
        isTrue(Objects.equals(expected, actual), errorCode, message);
    }

    public static void equals(Object expected, Object actual, ErrorCode errorCode, String message) {
        isTrue(Objects.equals(expected, actual), errorCode, message);
    }

    public static void equals(Object expected, Object actual, ErrorCode errorCode) {
        isTrue(Objects.equals(expected, actual), errorCode);
    }

    public static void equals(Object expected, Object actual, Supplier<String> message) {
        isTrue(Objects.equals(expected, actual), message);
    }

    public static void equals(Object expected, Object actual, String message) {
        isTrue(Objects.equals(expected, actual), message);
    }

    public static void equals(Object expected, Object actual) {
        isTrue(Objects.equals(expected, actual));
    }

    public static void notNull(Object object, ErrorCode errorCode, Supplier<String> message) {
        isTrue(object != null, errorCode, message);
    }

    public static void notNull(Object object, ErrorCode errorCode, String message) {
        isTrue(object != null, errorCode, message);
    }

    public static void notNull(Object object, ErrorCode errorCode) {
        isTrue(object != null, errorCode);
    }

    public static void notNull(Object object, Supplier<String> message) {
        isTrue(object != null, message);
    }

    public static void notNull(Object object, String message) {
        isTrue(object != null, message);
    }

    public static void notNull(Object object) {
        isTrue(object != null);
    }

    public static void isNull(Object object, ErrorCode errorCode, Supplier<String> message) {
        isTrue(object == null, errorCode, message);
    }

    public static void isNull(Object object, ErrorCode errorCode, String message) {
        isTrue(object == null, errorCode, message);
    }

    public static void isNull(Object object, ErrorCode errorCode) {
        isTrue(object == null, errorCode);
    }

    public static void isNull(Object object, Supplier<String> message) {
        isTrue(object == null, message);
    }

    public static void isNull(Object object, String message) {
        isTrue(object == null, message);
    }

    public static void isNull(Object object) {
        isTrue(object == null);
    }


    public static void notBlank(String string, ErrorCode errorCode, Supplier<String> message) {
        isTrue(StringUtils.isNotBlank(string), errorCode, message);
    }

    public static void notBlank(String string, ErrorCode errorCode, String message) {
        isTrue(StringUtils.isNotBlank(string), errorCode, message);
    }

    public static void notBlank(String string, ErrorCode errorCode) {
        isTrue(StringUtils.isNotBlank(string), errorCode);
    }

    public static void notBlank(String string, Supplier<String> message) {
        isTrue(StringUtils.isNotBlank(string), message);
    }

    public static void notBlank(String string, String message) {
        isTrue(StringUtils.isNotBlank(string), message);
    }

    public static void notBlank(String string) {
        isTrue(StringUtils.isNotBlank(string));
    }

    public static <T> T error(ErrorCode errorCode, String message) {
        throw new ValidationException(errorCode, message);
    }

    public static <T> T error(ErrorCode errorCode) {
        throw new ValidationException(errorCode);
    }

    public static <T> T error(String message) {
        throw new ValidationException(message);
    }

    public static <T> T error() {
        throw new ValidationException();
    }
}
