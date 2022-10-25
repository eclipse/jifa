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
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.enums.JobState;
import org.eclipse.jifa.master.entity.enums.JobType;

import java.time.Instant;

public class JobHelper {

    public static Job fromDBRecord(JsonObject jsonObject) {
        Job job = new Job();
        EntityHelper.fill(job, jsonObject);
        job.setUserId(jsonObject.getString("user_id"));
        job.setType(JobType.valueOf(jsonObject.getString("type")));
        job.setTarget(jsonObject.getString("target"));
        job.setState(JobState.valueOf(jsonObject.getString("state")));
        job.setHostIP(jsonObject.getString("host_ip"));
        job.setEstimatedLoad(jsonObject.getLong("estimated_load"));
        job.setAttachment(jsonObject.getString("attachment"));
        Instant accessTime = jsonObject.getInstant("access_time");
        if (accessTime != null) {
            job.setAccessTime(accessTime.toEpochMilli());
        }
        return job;
    }

}
