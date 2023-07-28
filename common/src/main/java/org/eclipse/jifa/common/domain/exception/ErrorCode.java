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
package org.eclipse.jifa.common.domain.exception;

/**
 * An error code is normally an enum instance.
 * Since it is not appropriate to define all error codes in a single enum class (different modules may have different error codes),
 * we define this interface to represent an error code.
 */
public interface ErrorCode {
    /**
     * @return the identifier of this error code
     */
    default String identifier() {
        return name();
    }

    /**
     * @return the name of this error code
     */
    String name();

    /**
     * @return the message of this error code
     */
    String message();
}
