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
package org.eclipse.jifa.master.entity;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@DataObject(generateConverter = true)
public class UserWorker extends Entity {

    public static UserWorker NOT_FOUND = notFoundInstance(UserWorker.class);

    private String hostIP;

    private int port;

    private List<String> userIds;

    private boolean enabled;

    private String operator;

    public UserWorker() {
    }

    public UserWorker(JsonObject json) {
        UserWorkerConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject result = new JsonObject();
        UserWorkerConverter.toJson(this, result);
        return result;
    }
}
