/********************************************************************************
 * Copyright (c) 2020,2021 Contributors to the Eclipse Foundation
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
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@DataObject(generateConverter = true)
@Builder
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

    public Worker(String hostIP, String hostName, long currentLoad, long maxLoad, long cpuCount, long memoryUsed, long memoryTotal, long diskUsed, long diskTotal) {
        this.hostIP = hostIP;
        this.hostName = hostName;
        this.currentLoad = currentLoad;
        this.maxLoad = maxLoad;
        this.cpuCount = cpuCount;
        this.memoryUsed = memoryUsed;
        this.memoryTotal = memoryTotal;
        this.diskUsed = diskUsed;
        this.diskTotal = diskTotal;
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
