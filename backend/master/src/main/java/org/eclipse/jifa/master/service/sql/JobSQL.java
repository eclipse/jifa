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
package org.eclipse.jifa.master.service.sql;

public interface JobSQL {

    String COUNT_ALL_PENDING = "SELECT COUNT(*) FROM active_job WHERE state = 'PENDING'";

    String COUNT_PENDING_BY_HOST_IP =
        "SELECT COUNT(*) FROM active_job WHERE state = 'PENDING' AND (host_ip IS NULL or host_ip = ?)";

    String SELECT_ALL_PENDING =
        "SELECT * FROM active_job WHERE state = 'PENDING' ORDER BY creation_time ASC";

    String INSERT_ACTIVE =
        "INSERT INTO active_job(user_id, type, state, target, host_ip, attachment, estimated_load, keep_alive) VALUE" +
        "(?, ?, ?, ?, ?, ?, ?, ?)";

    String UPDATE_ACCESS_TIME = "UPDATE active_job SET access_time = now() WHERE type = ? AND target = ?";

    String SELECT_ACTIVE_BY_TYPE_AND_TARGET = "SELECT * FROM active_job WHERE type = ? AND target = ?";

    String SELECT_ACTIVE_BY_TARGET = "SELECT * FROM active_job WHERE target = ?";

    String DELETE_ACTIVE_BY_TYPE_AND_TARGET = "DELETE FROM active_job WHERE type = ? AND target = ?";

    String INSERT_HISTORICAL =
        "INSERT INTO historical_job(user_id, type, target, host_ip, estimated_load) VALUE(?, ?, ?, ?, ?)";

    String SELECT_FRONT_PENDING =
        "SELECT * FROM active_job WHERE state = 'PENDING' AND creation_time < ? ORDER BY creation_time ASC";

    String SELECT_FRONT_PENDING_BY_HOST_IP =
        "SELECT * FROM active_job WHERE state = 'PENDING' AND (host_ip IS NULL or host_ip = ?) AND " +
        "creation_time < ? ORDER BY creation_time ASC";

    String SELECT_TO_RETIRE =
        "SELECT * FROM active_job WHERE state = 'IN_PROGRESS' AND keep_alive = 0 AND access_time <= ?";

    String SELECT_TRANSFER_JOB_TO_FILLING_RESULT =
        "SELECT * FROM active_job WHERE state = 'IN_PROGRESS' AND type = 'FILE_TRANSFER' AND access_time <= ?";

    String UPDATE_TO_IN_PROGRESS =
        "UPDATE active_job SET state = 'IN_PROGRESS', host_ip = ?, access_time = ? WHERE type = ? AND target = ?";

    String SELECT_ALL_ACTIVE_JOBS = "SELECT * FROM active_job";

    String SELECT_TRANSFER_JOB_BY_NAME = "SELECT * FROM active_job WHERE type = 'FILE_TRANSFER' AND target = ?";
}
