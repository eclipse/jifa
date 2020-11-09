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

@Data
@EqualsAndHashCode(callSuper = false)
@DataObject(generateConverter = true)
public class Worker extends Entity {
    public static Worker NOT_FOUND = notFoundInstance(Worker.class);

    private String hostIP;

    private String hostName;

    private long currentLoad;

    private long maxLoad;

    private long cpuCount;

    private long memoryUsed;

    private long memoryTotal;

    private long diskUsed;

    private long diskTotal;

    public Worker() {
    }

    public Worker(JsonObject json) {
        WorkerConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject result = new JsonObject();
        WorkerConverter.toJson(this, result);
        return result;
    }
}
