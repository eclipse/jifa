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
package org.eclipse.jifa.common.util;

import org.eclipse.jifa.common.domain.exception.ErrorCode;
import org.eclipse.jifa.common.domain.exception.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.eclipse.jifa.common.enums.CommonErrorCode.ILLEGAL_ARGUMENT;
import static org.eclipse.jifa.common.enums.CommonErrorCode.VALIDATION_FAILURE;

public class TestValidate {

    @Test
    public void test() throws Throwable {
        testPass(() -> Validate.isTrue(true, ILLEGAL_ARGUMENT, () -> "message"));
        testPass(() -> Validate.isTrue(true, ILLEGAL_ARGUMENT, "message"));
        testPass(() -> Validate.isTrue(true, ILLEGAL_ARGUMENT));
        testPass(() -> Validate.isTrue(true, () -> "message"));
        testPass(() -> Validate.isTrue(true, "message"));
        testPass(() -> Validate.isTrue(true));

        testFailure(() -> Validate.isTrue(false, ILLEGAL_ARGUMENT, () -> "message"),
                    ILLEGAL_ARGUMENT,
                    "message");
        testFailure(() -> Validate.isTrue(false, ILLEGAL_ARGUMENT, "message"),
                    ILLEGAL_ARGUMENT,
                    "message");
        testFailure(() -> Validate.isTrue(false, ILLEGAL_ARGUMENT),
                    ILLEGAL_ARGUMENT,
                    ILLEGAL_ARGUMENT.message());
        testFailure(() -> Validate.isTrue(false, () -> "message"),
                    VALIDATION_FAILURE,
                    "message");
        testFailure(() -> Validate.isTrue(false, "message"),
                    VALIDATION_FAILURE,
                    "message");
        testFailure(() -> Validate.isTrue(false),
                    VALIDATION_FAILURE,
                    VALIDATION_FAILURE.message());

        testPass(() -> Validate.isFalse(false, ILLEGAL_ARGUMENT, () -> "message"));
        testPass(() -> Validate.isFalse(false, ILLEGAL_ARGUMENT, "message"));
        testPass(() -> Validate.isFalse(false, ILLEGAL_ARGUMENT));
        testPass(() -> Validate.isFalse(false, () -> "message"));
        testPass(() -> Validate.isFalse(false, "message"));
        testPass(() -> Validate.isFalse(false));

        testFailure(() -> Validate.isFalse(true, ILLEGAL_ARGUMENT, () -> "message"),
                    ILLEGAL_ARGUMENT,
                    "message");
        testFailure(() -> Validate.isFalse(true, ILLEGAL_ARGUMENT, "message"),
                    ILLEGAL_ARGUMENT,
                    "message");
        testFailure(() -> Validate.isFalse(true, ILLEGAL_ARGUMENT),
                    ILLEGAL_ARGUMENT,
                    ILLEGAL_ARGUMENT.message());
        testFailure(() -> Validate.isFalse(true, () -> "message"),
                    VALIDATION_FAILURE,
                    "message");
        testFailure(() -> Validate.isFalse(true, "message"),
                    VALIDATION_FAILURE,
                    "message");
        testFailure(() -> Validate.isFalse(true),
                    VALIDATION_FAILURE,
                    VALIDATION_FAILURE.message());

        testPass(() -> Validate.equals(1, 1, ILLEGAL_ARGUMENT, () -> "message"));
        testPass(() -> Validate.equals(1, 1, ILLEGAL_ARGUMENT, "message"));
        testPass(() -> Validate.equals(1, 1, ILLEGAL_ARGUMENT));
        testPass(() -> Validate.equals(1, 1, () -> "message"));
        testPass(() -> Validate.equals(1, 1, "message"));
        testPass(() -> Validate.equals(1, 1));

        testFailure(() -> Validate.equals(1, 0, ILLEGAL_ARGUMENT, () -> "message"),
                    ILLEGAL_ARGUMENT,
                    "message");
        testFailure(() -> Validate.equals(1, 0, ILLEGAL_ARGUMENT, "message"),
                    ILLEGAL_ARGUMENT,
                    "message");
        testFailure(() -> Validate.equals(1, 0, ILLEGAL_ARGUMENT),
                    ILLEGAL_ARGUMENT,
                    ILLEGAL_ARGUMENT.message());
        testFailure(() -> Validate.equals(1, 0, () -> "message"),
                    VALIDATION_FAILURE,
                    "message");
        testFailure(() -> Validate.equals(1, 0, "message"),
                    VALIDATION_FAILURE,
                    "message");
        testFailure(() -> Validate.equals(1, 0),
                    VALIDATION_FAILURE,
                    VALIDATION_FAILURE.message());

        testPass(() -> Validate.notNull(new Object(), ILLEGAL_ARGUMENT, () -> "message"));
        testPass(() -> Validate.notNull(new Object(), ILLEGAL_ARGUMENT, "message"));
        testPass(() -> Validate.notNull(new Object(), ILLEGAL_ARGUMENT));
        testPass(() -> Validate.notNull(new Object(), () -> "message"));
        testPass(() -> Validate.notNull(new Object(), "message"));
        testPass(() -> Validate.notNull(new Object()));

        testFailure(() -> Validate.notNull(null, ILLEGAL_ARGUMENT, () -> "message"),
                    ILLEGAL_ARGUMENT,
                    "message");
        testFailure(() -> Validate.notNull(null, ILLEGAL_ARGUMENT, "message"),
                    ILLEGAL_ARGUMENT,
                    "message");
        testFailure(() -> Validate.notNull(null, ILLEGAL_ARGUMENT),
                    ILLEGAL_ARGUMENT,
                    ILLEGAL_ARGUMENT.message());
        testFailure(() -> Validate.notNull(null, () -> "message"),
                    VALIDATION_FAILURE,
                    "message");
        testFailure(() -> Validate.notNull(null, "message"),
                    VALIDATION_FAILURE,
                    "message");
        testFailure(() -> Validate.notNull(null),
                    VALIDATION_FAILURE,
                    VALIDATION_FAILURE.message());

        testPass(() -> Validate.notBlank("1", ILLEGAL_ARGUMENT, () -> "message"));
        testPass(() -> Validate.notBlank("1", ILLEGAL_ARGUMENT, "message"));
        testPass(() -> Validate.notBlank("1", ILLEGAL_ARGUMENT));
        testPass(() -> Validate.notBlank("1", () -> "message"));
        testPass(() -> Validate.notBlank("1", "message"));
        testPass(() -> Validate.notBlank("1"));

        testFailure(() -> Validate.notBlank("  ", ILLEGAL_ARGUMENT, () -> "message"),
                    ILLEGAL_ARGUMENT,
                    "message");
        testFailure(() -> Validate.notBlank("  ", ILLEGAL_ARGUMENT, "message"),
                    ILLEGAL_ARGUMENT,
                    "message");
        testFailure(() -> Validate.notBlank("  ", ILLEGAL_ARGUMENT),
                    ILLEGAL_ARGUMENT,
                    ILLEGAL_ARGUMENT.message());
        testFailure(() -> Validate.notBlank("  ", () -> "message"),
                    VALIDATION_FAILURE,
                    "message");
        testFailure(() -> Validate.notBlank("  ", "message"),
                    VALIDATION_FAILURE,
                    "message");
        testFailure(() -> Validate.notBlank("  "),
                    VALIDATION_FAILURE,
                    VALIDATION_FAILURE.message());

        testFailure(() -> Validate.error(ILLEGAL_ARGUMENT, "message"), ILLEGAL_ARGUMENT, "message");
        testFailure(() -> Validate.error(ILLEGAL_ARGUMENT), ILLEGAL_ARGUMENT, ILLEGAL_ARGUMENT.message());
        testFailure(() -> Validate.error("message"), VALIDATION_FAILURE, "message");
        testFailure(Validate::error, VALIDATION_FAILURE, VALIDATION_FAILURE.message());
    }

    private void testPass(Executable executable) throws Throwable {
        executable.execute();
    }

    private void testFailure(Executable executable, ErrorCode expectedErrorCode, String expectedMessage) {

        Throwable throwable = null;
        try {
            executable.execute();
        } catch (Throwable t) {
            throwable = t;
        }

        Assertions.assertNotNull(throwable);

        Assertions.assertTrue(throwable instanceof ValidationException);
        Assertions.assertEquals(expectedErrorCode, ((ValidationException) throwable).getErrorCode());
        Assertions.assertEquals(expectedMessage, throwable.getMessage());
    }
}
