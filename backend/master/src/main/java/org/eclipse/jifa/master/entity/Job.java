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
import org.eclipse.jifa.master.entity.enums.JobState;
import org.eclipse.jifa.master.entity.enums.JobType;

@Data
@EqualsAndHashCode(callSuper = false)
@DataObject(generateConverter = true)
public class Job extends Entity {

    public static Job NOT_FOUND = notFoundInstance(Job.class);

    private String userId;

    private JobType type;

    private JobState state;

    private String hostIP;

    private String target;

    private String attachment;

    private long estimatedLoad;

    private boolean immediate = false;

    private boolean keepAlive;

    private long accessTime;

    public Job() {
    }

    public Job(JsonObject json) {
        JobConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject result = new JsonObject();
        JobConverter.toJson(this, result);
        return result;
    }

}
