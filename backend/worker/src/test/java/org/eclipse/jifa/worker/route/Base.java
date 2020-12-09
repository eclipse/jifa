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
package org.eclipse.jifa.worker.route;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import org.eclipse.jifa.worker.Constant;
import org.eclipse.jifa.worker.WorkerGlobal;

public class Base {

    static WebClient CLIENT = WebClient.create(Vertx.vertx());

    static String TEST_HEAP_DUMP_FILENAME;

    public static String uri(String uri) {
        return WorkerGlobal.stringConfig(Constant.ConfigKey.API_PREFIX) + uri;
    }

}
