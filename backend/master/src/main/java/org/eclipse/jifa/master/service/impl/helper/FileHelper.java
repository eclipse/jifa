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
import org.eclipse.jifa.common.enums.FileTransferState;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.master.entity.File;

import java.time.Instant;

public class FileHelper {

    public static File fromDBRecord(JsonObject jsonObject) {
        File file = new File();
        EntityHelper.fill(file, jsonObject);
        file.setUserId(jsonObject.getString("user_id"));
        file.setOriginalName(jsonObject.getString("original_name"));
        file.setDisplayName(jsonObject.getString("display_name"));
        file.setName(jsonObject.getString("name"));
        file.setType(FileType.valueOf(jsonObject.getString("type")));
        file.setSize(jsonObject.getLong("size"));
        file.setHostIP(jsonObject.getString("host_ip"));
        file.setTransferState(FileTransferState.valueOf(jsonObject.getString("transfer_state")));
        file.setShared(jsonObject.getInteger("shared") == 1);
        file.setDownloadable(jsonObject.getInteger("downloadable") == 1);
        file.setInSharedDisk(jsonObject.getInteger("in_shared_disk") == 1);
        file.setDeleted(jsonObject.getInteger("deleted") == 1);
        Instant deletedTime = jsonObject.getInstant("deleted_time");
        if (deletedTime != null) {
            file.setDeletedTime(deletedTime.toEpochMilli());
        }
        return file;
    }
}
