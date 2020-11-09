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
package org.eclipse.jifa.worker;

import io.vertx.core.json.JsonObject;
import org.eclipse.jifa.common.JifaHooks;

import java.util.concurrent.atomic.AtomicInteger;

public class FakeHooks implements JifaHooks {
    static AtomicInteger initTriggered = new AtomicInteger(0);

    public FakeHooks() {
    }

    public static void reset() {
        initTriggered.set(0);
    }

    public static int countInitTriggered() {
        return initTriggered.get();
    }

    @Override
    public void init(JsonObject config) {
        initTriggered.incrementAndGet();
    }
}
