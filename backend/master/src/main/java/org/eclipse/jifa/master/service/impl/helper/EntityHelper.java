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
import org.eclipse.jifa.master.entity.Entity;

class EntityHelper {

    static void fill(Entity entity, JsonObject json) {
        entity.setId(json.getLong("id"));
        entity.setLastModifiedTime(json.getInstant("last_modified_time").toEpochMilli());
        entity.setCreationTime(json.getInstant("creation_time").toEpochMilli());
    }
}
