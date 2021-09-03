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

import lombok.Data;
import org.eclipse.jifa.common.JifaException;

import java.lang.reflect.Constructor;

@Data
public abstract class Entity {

    private static long NOT_FOUND_RECORD_ID = -1;

    private long id;

    private long lastModifiedTime;

    private long creationTime;

    static <R extends Entity> R notFoundInstance(Class<R> clazz) {
        try {
            Constructor<R> constructor = clazz.getConstructor();
            R record = constructor.newInstance();
            record.setId(NOT_FOUND_RECORD_ID);
            return record;
        } catch (Throwable t) {
            throw new JifaException(t);
        }
    }

    public boolean found() {
        return getId() != NOT_FOUND_RECORD_ID;
    }

    public boolean notFound() {
        return !found();
    }
}
