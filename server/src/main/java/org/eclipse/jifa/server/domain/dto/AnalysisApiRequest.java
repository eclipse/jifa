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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.eclipse.jifa.common.util.Validate;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.domain.support.JsonConvertible;

import static org.eclipse.jifa.server.Constant.ANALYSIS_API_REQUEST_API_KEY;
import static org.eclipse.jifa.server.Constant.ANALYSIS_API_REQUEST_NAMESPACE_KEY;
import static org.eclipse.jifa.server.Constant.ANALYSIS_API_REQUEST_PARAMETERS_KEY;
import static org.eclipse.jifa.server.Constant.ANALYSIS_API_REQUEST_TARGET_KEY;

public record AnalysisApiRequest(JsonObject json) implements JsonConvertible {

    public AnalysisApiRequest {
        JsonElement namespace = json.get(ANALYSIS_API_REQUEST_NAMESPACE_KEY);
        Validate.isTrue(namespace != null && namespace.isJsonPrimitive() && namespace.getAsJsonPrimitive().isString(),
                        "namespace is required and must be a string");

        JsonElement api = json.get(ANALYSIS_API_REQUEST_API_KEY);
        Validate.isTrue(api != null && api.isJsonPrimitive() && api.getAsJsonPrimitive().isString(),
                        "api is required and must be a string");

        JsonElement target = json.get(ANALYSIS_API_REQUEST_TARGET_KEY);
        Validate.isTrue(target != null && target.isJsonPrimitive() && target.getAsJsonPrimitive().isString(),
                        "target is required and must be a string");

        JsonElement parameters = json.get(ANALYSIS_API_REQUEST_PARAMETERS_KEY);
        Validate.isTrue(parameters == null || parameters.isJsonObject(),
                        "parameter must be an object");

        if (parameters == null) {
            json.add(ANALYSIS_API_REQUEST_PARAMETERS_KEY, new JsonObject());
        }
    }

    public String namespace() {
        return json.get(Constant.ANALYSIS_API_REQUEST_NAMESPACE_KEY).getAsString();
    }

    public String api() {
        return json.get(Constant.ANALYSIS_API_REQUEST_API_KEY).getAsString();
    }

    public String target() {
        return json.get(Constant.ANALYSIS_API_REQUEST_TARGET_KEY).getAsString();
    }

    public JsonObject parameters() {
        return json.getAsJsonObject(Constant.ANALYSIS_API_REQUEST_PARAMETERS_KEY);
    }

    @Override
    public String toJson() {
        return json.toString();
    }
}
