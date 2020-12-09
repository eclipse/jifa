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

public interface UserWorkerSQL {

    String INSERT = "INSERT INTO user_worker(host_ip, port, user_ids, enabled, operator) VALUES(?, ?, ?, ?, ?)";

    String DELETE = "DELETE FROM user_worker WHERE host_ip = ? AND port = ?";

    String UPDATE_USER_IDS = "UPDATE user_worker SET user_ids = ?, operator = ? WHERE host_ip = ? AND port = ?";

    String SET_ENABLED = "UPDATE user_worker SET enabled = ?, operator = ? WHERE host_ip = ? AND port = ? ";

    String SELECT_BY_HOST_IP_AND_PORT = "SELECT * FROM user_worker WHERE host_ip = ? AND port = ?";

    String UPDATE_USER_IDS_AND_STATE =
        "UPDATE user_worker SET user_ids = ?, enabled = ?, operator = ? WHERE host_ip = ? AND port = ?";

    String SELECT_ALL = "SELECT * FROM user_worker ORDER BY last_modified_time DESC";
}
