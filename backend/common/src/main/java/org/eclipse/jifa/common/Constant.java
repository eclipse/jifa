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
package org.eclipse.jifa.common;

public interface Constant {
    String HEADER_CONTENT_TYPE_KEY = "Content-Type";
    String CONTENT_TYPE_JSON_FORM = "application/json; charset=UTF-8";

    int HTTP_GET_OK_STATUS_CODE = 200;
    int HTTP_POST_CREATED_STATUS_CODE = 201;

    int HTTP_BAD_REQUEST_STATUS_CODE = 400;
    int HTTP_FORBIDDEN_REQUEST_STATUS_CODE = 403;
    int HTTP_INTERNAL_SERVER_ERROR_STATUS_CODE = 500;

    String LINE_SEPARATOR = System.lineSeparator();

    String EMPTY_STRING = "";
}
