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

import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.model.TimeLineChartView;
import io.vertx.core.Promise;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;

public class Graph extends GCLogBaseRoute {
    @RouteMeta(path = "/graph")
    void count(Promise<TimeLineChartView> promise,
               @ParamKey("file") String file,
               @ParamKey("type") String type,
               @ParamKey("timeSpan") double timeSpan,
               @ParamKey("timePoint") double timePoint) {
        final GCModel model = Analyzer.getOrOpenGCLogModel(file);
        promise.complete(model.getGraphView(type, timeSpan, timePoint));
    }
}
