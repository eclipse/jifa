/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.worker.route.threaddump;

import org.eclipse.jifa.worker.route.BaseRoute;
import org.eclipse.jifa.worker.route.MappingPrefix;

import java.util.ArrayList;
import java.util.List;

@MappingPrefix("/thread-dump/:file")
public class ThreadDumpBaseRoute extends BaseRoute {

    private static List<Class<? extends ThreadDumpBaseRoute>> ROUTES = new ArrayList<>();

    static {
        ROUTES.add(ThreadDumpRoute.class);
    }

    public static List<Class<? extends ThreadDumpBaseRoute>> routes() {
        return ROUTES;
    }
}
