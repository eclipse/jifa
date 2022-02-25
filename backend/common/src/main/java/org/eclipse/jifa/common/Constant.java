/********************************************************************************
 * Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation
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

import java.util.concurrent.TimeUnit;

public interface Constant {
    String HEADER_CONTENT_TYPE_KEY = "Content-Type";
    String HEADER_CONTENT_LENGTH_KEY = "Content-Length";
    String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    String HEADER_AUTHORIZATION = "authorization";
    String CONTENT_TYPE_JSON_FORM = "application/json; charset=UTF-8";
    String CONTENT_TYPE_FILE_FORM = "application/octet-stream";

    String COOKIE_AUTHORIZATION = "jifa-authorization";
    String HEADER_AUTHORIZATION_PREFIX = "Bearer ";

    int HTTP_GET_OK_STATUS_CODE = 200;
    int HTTP_POST_CREATED_STATUS_CODE = 201;

    int HTTP_BAD_REQUEST_STATUS_CODE = 400;
    int HTTP_POST_CREATED_STATUS = 201;
    int HTTP_INTERNAL_SERVER_ERROR_STATUS_CODE = 500;

    String LINE_SEPARATOR = System.lineSeparator();

    String EMPTY_STRING = "";

    String FILE_TYPE = "type";
    String PAGE = "page";
    String PAGE_SIZE = "pageSize";

    String UNKNOWN_STRING = "UNKNOWN";
    String DEFAULT_WORKSPACE = System.getProperty("user.home") + java.io.File.separator + "jifa_workspace";

    long STALE_THRESHOLD = TimeUnit.HOURS.toMillis(6);
}
