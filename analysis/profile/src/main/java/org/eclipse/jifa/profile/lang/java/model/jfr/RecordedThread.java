/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.profile.lang.java.model.jfr;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.unit.IQuantity;

import java.lang.reflect.Field;

@Slf4j
public class RecordedThread {
    @Setter
    @Getter
    private long javaThreadId;
    @Getter
    private String javaName;
    @Setter
    private long osThreadId;

    public RecordedThread(long id, String javaName, long javaThreadId, long osThreadId) {
        this.javaName = javaName;
        this.javaThreadId = javaThreadId;
        this.osThreadId = osThreadId;
    }

    public RecordedThread(IMCThread imcThread) {
        this.javaThreadId = imcThread.getThreadId();
        this.javaName = imcThread.getThreadName();
        try {
            Field f = imcThread.getClass().getDeclaredField("osThreadId");
            f.setAccessible(true);
            Object value = f.get(imcThread);
            if (value instanceof IQuantity) {
                this.osThreadId = ((IQuantity)value).longValue();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        if (this.javaThreadId == 0 && this.osThreadId > 0) {
            this.javaThreadId = -this.osThreadId;
        }
    }

    public long getOSThreadId() {
        return osThreadId;
    }
}
