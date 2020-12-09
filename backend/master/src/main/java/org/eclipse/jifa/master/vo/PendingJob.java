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
package org.eclipse.jifa.master.vo;

import lombok.Data;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.enums.JobType;

@Data
public class PendingJob {

    private String userId;

    private JobType type;

    private String target;

    private long creationTime;

    public PendingJob(Job job) {
        this.userId = job.getUserId();
        this.type = job.getType();
        this.target = job.getTarget();
        this.creationTime = job.getCreationTime();
    }
}
