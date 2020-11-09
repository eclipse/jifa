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

import com.alibaba.fastjson.JSON;
import io.vertx.core.json.JsonObject;
import org.eclipse.jifa.master.entity.UserWorker;

import java.util.List;

public class UserWorkerHelper {
    public static UserWorker fromDBRecord(JsonObject jsonObject) {
        UserWorker worker = new UserWorker();
        EntityHelper.fill(worker, jsonObject);
        worker.setHostIP(jsonObject.getString("host_ip"));
        worker.setPort(jsonObject.getInteger("port"));
        worker.setUserIds(JSON.<List<String>>parseObject(jsonObject.getString("user_ids"), List.class));
        worker.setEnabled(jsonObject.getInteger("enabled") == 1);
        worker.setOperator(jsonObject.getString("operator"));
        return worker;
    }
}
