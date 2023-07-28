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
package org.eclipse.jifa.server.domain.dto;

import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;

public record HttpRequestToWorker<Response>(HttpMethod method,
                                            String uri,
                                            MultiValueMap<String, String> query,
                                            Object body,
                                            Class<Response> responseType) {

    public static <T> HttpRequestToWorker<T> createPostRequest(String uri, Object body, Class<T> responseType) {
        return new HttpRequestToWorker<>(HttpMethod.POST, uri, null, body, responseType);
    }

    public static <T> HttpRequestToWorker<T> createDeleteRequest(String uri, Object body, Class<T> responseType) {
        return new HttpRequestToWorker<>(HttpMethod.DELETE, uri, null, body, responseType);
    }

    public static <T> HttpRequestToWorker<T> createGetRequest(String uri, MultiValueMap<String, String> query, Class<T> responseType) {
        return new HttpRequestToWorker<>(HttpMethod.GET, uri, query, null, responseType);
    }
}
