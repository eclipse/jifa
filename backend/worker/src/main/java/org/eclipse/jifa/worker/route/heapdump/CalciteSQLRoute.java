/********************************************************************************
 * Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation
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

import io.vertx.core.Promise;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.hda.api.Model;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;

import static org.eclipse.jifa.worker.support.Analyzer.HEAP_DUMP_ANALYZER;
import static org.eclipse.jifa.worker.support.Analyzer.getOrOpenAnalysisContext;

class CalciteSQLRoute extends HeapBaseRoute {

    @RouteMeta(path = "/sql")
    void calciteSql(Promise<Model.CalciteSQLResult> promise, @ParamKey("file") String file,
             @ParamKey("sql") String oql, @ParamKey(value = "sortBy", mandatory = false) String sortBy,
             @ParamKey(value = "ascendingOrder", mandatory = false) boolean ascendingOrder,
             PagingRequest pagingRequest) {
        promise.complete(HEAP_DUMP_ANALYZER.getCalciteSQLResult(getOrOpenAnalysisContext(file),
                                                        oql, sortBy, ascendingOrder,
                                                        pagingRequest.getPage(), pagingRequest.getPageSize()));
    }
}
