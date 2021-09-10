/********************************************************************************
 * Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation
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

public interface FileSQL {
    String SELECT_DATED_FILES =
            "SELECT f.* FROM file f LEFT JOIN active_job aj ON f.name=aj.target " +
                    "WHERE aj.target is null and " +
                    "f.deleted=0 and " +
                    "f.cas_state=0 and " +
                    "f.transfer_state='SUCCESS' and " +
                    "f.last_modified_time < now() - interval 30 day " +
                    "LIMIT 50";

    String INSERT = "INSERT INTO file(user_id, original_name, name, type, size, host_ip, transfer_state, shared, " +
                    "downloadable, in_shared_disk, deleted, cas_state) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    String SELECT_BY_USER_ID_AND_TYPE =
        "SELECT * FROM file WHERE user_id = ? AND type = ? AND deleted = false ORDER BY creation_time DESC LIMIT ?, ?";

    String COUNT_BY_USER_ID_AND_TYPE = "SELECT COUNT(*) FROM file WHERE user_id = ? AND type = ? AND deleted = false";

    String SELECT_BY_USER_ID_AND_TYPE_AND_EXPECTED_NAME =
        "SELECT * FROM file WHERE user_id = ? AND type = ? AND (name like ? OR display_name like ?) AND deleted = " +
        "false ORDER BY creation_time DESC LIMIT ?, ?";

    String COUNT_BY_USER_ID_AND_TYPE_AND_EXPECTED_NAME = "SELECT COUNT(*) FROM file WHERE user_id = ? AND type = ? " +
                                                         "AND (name like ? OR display_name like ?) AND deleted = false";

    String SELECT_FILE_BY_NAME = "SELECT * FROM file WHERE name = ?";

    String UPDATE_TRANSFER_RESULT = "UPDATE file SET transfer_state = ?, size = ? WHERE name = ?";

    String SET_SHARED = "UPDATE file SET shared = 1 WHERE name = ?";

    /**
     * To simultaneously accept users' requests while deleting dated files
     * we can CAS cas_state field to handle these works, its values are as follow:
     * <p>
     * cas_state(0) - This file is not being using by users
     * cas_state(1) - This file is currently being using by users
     * cas_state(2) - This file will be deleted quickly
     */
    String UPDATE_FILE_AS_USED =
        "UPDATE file SET cas_state = 1 WHERE name = ? and deleted = false AND (cas_state = 0 OR cas_state = 1)";

    String UPDATE_FILE_AS_UNUSED =
        "UPDATE file SET cas_state = 0 WHERE name = ? and deleted = false AND (cas_state = 1 OR cas_state = 0)";

    String UPDATE_AS_PENDING_DELETE = "UPDATE file SET cas_state = 2 WHERE host_ip = ? AND deleted = false AND " +
                                      "cas_state = 0 AND transfer_state != 'IN_PROGRESS' ORDER BY creation_time ASC LIMIT 10";

    String SELECT_PENDING_DELETE = "SELECT * FROM file WHERE host_ip = ? AND cas_state = 2 AND deleted = false";

    String DELETE_FILE_BY_NAME =
        "UPDATE file SET deleted = true, deleter = ?, deleted_time = now(), cas_state = 0 WHERE name= ? AND deleted = false";

    String UPDATE_DISPLAY_NAME = "UPDATE file SET display_name = ? WHERE name = ?";

    String SELECT_FILES_FOR_SYNC =
        "SELECT * FROM file  WHERE host_ip = ? AND transfer_state = 'SUCCESS' AND deleted = false AND type != 'JINSIGHT'";
}
