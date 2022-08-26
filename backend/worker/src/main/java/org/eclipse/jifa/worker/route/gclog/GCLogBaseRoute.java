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

package org.eclipse.jifa.worker.route.gclog;

import org.eclipse.jifa.worker.route.BaseRoute;
import org.eclipse.jifa.worker.route.MappingPrefix;

import java.util.ArrayList;
import java.util.List;

@MappingPrefix("/gc-log/:file")
public class GCLogBaseRoute extends BaseRoute {
    private static List<Class<? extends GCLogBaseRoute>> ROUTES = new ArrayList<>();

    static {
        ROUTES.add(GCLogRoute.class);
    }

    public static List<Class<? extends GCLogBaseRoute>> routes() {
        return ROUTES;
    }
}
