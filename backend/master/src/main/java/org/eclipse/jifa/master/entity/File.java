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
import org.eclipse.jifa.common.enums.FileTransferState;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.master.entity.enums.Deleter;
import org.eclipse.jifa.master.model.TransferWay;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@DataObject(generateConverter = true)
public class File extends Entity {

    public static File NOT_FOUND = notFoundInstance(File.class);

    private String userId;

    private String originalName;

    private String name;

    private String displayName;

    private FileType type;

    private long size;

    private FileTransferState transferState;

    private boolean shared;

    private boolean downloadable;

    private boolean inSharedDisk;

    private String hostIP;

    private boolean deleted;

    private Deleter deleter;

    private long deletedTime;

    private TransferWay transferWay;

    private Map<String, String> transferInfo;

    public File() {
    }

    public File(JsonObject json) {
        FileConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject result = new JsonObject();
        FileConverter.toJson(this, result);
        return result;
    }

    public boolean transferred() {
        return transferState == FileTransferState.SUCCESS;
    }
}
