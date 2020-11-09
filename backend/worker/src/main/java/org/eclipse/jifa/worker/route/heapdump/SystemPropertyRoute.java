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
package org.eclipse.jifa.worker.route.heapdump;

import io.vertx.core.Future;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport;
import org.eclipse.mat.inspections.SystemPropertiesQuery;
import org.eclipse.mat.query.IResultTable;

import java.util.HashMap;
import java.util.Map;

class SystemPropertyRoute extends HeapBaseRoute {

    @RouteMeta(path = "/systemProperties")
    void systemProperty(Future<Map<String, String>> future, @ParamKey("file") String file) throws Exception {
        SystemPropertiesQuery query = new SystemPropertiesQuery();
        query.snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        IResultTable result = query.execute(HeapDumpSupport.VOID_LISTENER);
        Map<String, String> sp = new HashMap<>();
        int count = result.getRowCount();
        for (int i = 0; i < count; i++) {
            Object row = result.getRow(i);
            sp.put((String) result.getColumnValue(row, 1), (String) result.getColumnValue(row, 2));
        }
        future.complete(sp);
    }
}
