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
package org.eclipse.jifa.master.service.impl.helper;

import io.vertx.core.json.JsonObject;
import org.eclipse.jifa.master.entity.Worker;

public class WorkerHelper {

    public static Worker fromDBRecord(JsonObject jsonObject) {
        Worker worker = new Worker();
        EntityHelper.fill(worker, jsonObject);
        worker.setHostIP(jsonObject.getString("host_ip"));
        worker.setHostName(jsonObject.getString("host_name"));
        worker.setCurrentLoad(jsonObject.getLong("current_load"));
        worker.setMaxLoad(jsonObject.getLong("max_load"));
        worker.setCpuCount(jsonObject.getLong("cpu_count"));
        worker.setMemoryUsed(jsonObject.getLong("memory_used"));
        worker.setMemoryTotal(jsonObject.getLong("memory_total"));
        worker.setDiskUsed(jsonObject.getLong("disk_used"));
        worker.setDiskTotal(jsonObject.getLong("disk_total"));
        return worker;
    }
}
