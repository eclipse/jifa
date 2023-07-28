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

import com.google.gson.JsonObject;
import org.eclipse.jifa.common.domain.exception.ShouldNotReachHereException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.eclipse.jifa.common.util.GsonHolder.GSON;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

public class ControllerUtil {

    public static MimeType checkMimeTypeForPostRequest(String contentType) {
        MimeType mimeType = MimeTypeUtils.parseMimeType(contentType);

        if (!APPLICATION_JSON.equalsTypeAndSubtype(mimeType) && !APPLICATION_FORM_URLENCODED.equalsTypeAndSubtype(mimeType)) {
            throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }

        return mimeType;
    }

    public static MimeType checkMimeTypeForStompMessage(String contentType) {
        MimeType mimeType = MimeTypeUtils.parseMimeType(contentType);

        if (!APPLICATION_JSON.equalsTypeAndSubtype(mimeType)) {
            throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }
        return mimeType;
    }

    public static JsonObject parseArgs(MimeType mimeType, byte[] content) {
        if (content == null || content.length == 0) {
            return null;
        }

        if (APPLICATION_JSON.equalsTypeAndSubtype(mimeType)) {
            return GSON.fromJson(new String(content, getCharset(mimeType)), JsonObject.class);
        } else if (APPLICATION_FORM_URLENCODED.equalsTypeAndSubtype(mimeType)) {
            Charset charset = getCharset(mimeType);
            String str = new String(content, charset);
            String[] pairs = StringUtils.tokenizeToStringArray(str, "&");
            MultiValueMap<String, String> result = new LinkedMultiValueMap<>(pairs.length);
            for (String pair : pairs) {
                int idx = pair.indexOf('=');
                if (idx == -1) {
                    result.add(URLDecoder.decode(pair, charset), null);
                } else {
                    String name = URLDecoder.decode(pair.substring(0, idx), charset);
                    String value = URLDecoder.decode(pair.substring(idx + 1), charset);
                    result.add(name, value);
                }
            }
            return GSON.fromJson(GSON.toJson(result), JsonObject.class);
        } else {
            throw new ShouldNotReachHereException();
        }
    }

    public static JsonObject convertMultiValueMapToJsonObject(MultiValueMap<String, String> params) {
        JsonObject args = new JsonObject();
        params.forEach((key, values) -> {
            if (values.size() == 1) {
                args.addProperty(key, values.get(0));
            } else {
                args.add(key, GSON.toJsonTree(values));
            }
        });
        return args;
    }

    public static Charset getCharset(MimeType mimeType) {
        Charset charset = mimeType.getCharset();
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }
        return charset;
    }
}
