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

public interface WorkerSQL {

    String SELECT_MOST_IDLE =
        "SELECT * FROM worker ORDER BY (max_load - current_load) DESC, last_modified_time ASC LIMIT 1";

    String SELECT_BY_IP = "SELECT * FROM worker WHERE host_ip = ?";

    String SELECT_ALL = "SELECT * FROM worker";

    String UPDATE_LOAD = "UPDATE worker SET current_load = ? WHERE host_ip = ?";

    String SELECT_FOR_DISK_CLEANUP =
        "SELECT * FROM worker WHERE disk_total > 0 AND disk_used / disk_total >= 0.75 ORDER BY disk_used DESC LIMIT 20";

    String UPDATE_DISK_USAGE = "UPDATE worker SET disk_total= ?, disk_used= ? WHERE host_ip = ?";
}
