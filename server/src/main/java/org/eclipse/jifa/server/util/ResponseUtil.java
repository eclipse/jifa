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
package org.eclipse.jifa.server.util;

import io.netty.handler.timeout.ReadTimeoutException;
import org.eclipse.jifa.common.util.GsonHolder;
import org.eclipse.jifa.server.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.lang.invoke.MethodHandles;

import static org.eclipse.jifa.common.util.GsonHolder.GSON;

public class ResponseUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ResponseUtil() {
    }

    public static byte[] toBytes(Object o) {
        if (o == null) {
            return Constant.EMPTY_BYTE_ARRAY;
        }

        try {
            return GsonHolder.GSON.toJson(o).getBytes(Constant.CHARSET);
        } catch (Throwable t) {
            LOGGER.error("Error occurred while converting response to data", t);
            return null;
        }
    }

    public static byte[] toData(Throwable throwable) {
        if (throwable instanceof WebClientResponseException e) {
            return e.getResponseBodyAsByteArray();
        }

        Throwable cause = throwable;
        String message;
        String fallbackMessage = null;
        do {
            message = map(cause);
            if (cause.getMessage() != null) {
                fallbackMessage = cause.getMessage();
            }
            if (message != null || cause.getCause() == null) {
                break;
            }
            cause = cause.getCause();
        } while (true);

        if (message == null) {
            message = fallbackMessage;
        }

        try {
            if (message != null) {
                return GSON.toJson(message).getBytes(Constant.CHARSET);
            } else {
                return null;
            }
        } catch (Throwable t) {
            LOGGER.error("Error occurred while converting throwable to data", t);
            return null;
        }
    }

    private static String map(Throwable t) {
        if (t instanceof ReadTimeoutException) {
            return "Timeout";
        }
        return null;
    }
}
