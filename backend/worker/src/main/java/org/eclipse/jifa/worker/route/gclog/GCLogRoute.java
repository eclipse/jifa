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
import io.vertx.core.Promise;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.gclog.model.TimeLineChartView;
import org.eclipse.jifa.gclog.vo.GCLogMetadata;
import org.eclipse.jifa.gclog.vo.ObjectStatistics;
import org.eclipse.jifa.gclog.vo.TimeRange;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;

public class GCLogRoute extends org.eclipse.jifa.worker.route.gclog.GCLogBaseRoute {
    @RouteMeta(path = "/metadata")
    void metadata(Promise<GCLogMetadata> promise, @ParamKey("file") String file) {
        final GCModel model = Analyzer.getOrOpenGCLogModel(file);
        promise.complete(model.getGcModelMetadata());
    }

    @RouteMeta(path = "/objectStatistics")
    void objectStats(Promise<ObjectStatistics> promise, @ParamKey("file") String file,
                     @ParamKey("start")double start,
                     @ParamKey("end")double end) {
        final GCModel model = Analyzer.getOrOpenGCLogModel(file);
        promise.complete(model.getObjectStatistics(new TimeRange(start, end)));
    }

    @RouteMeta(path = "/gcDetails")
    void detail(Promise<PageView<String>> promise, @ParamKey("file") String file,
                @ParamKey(value = "eventType", mandatory = false) String eventType,
                @ParamKey(value = "gcCause", mandatory = false) String gcCause,
                @ParamKey(value = "logTimeLow", mandatory = false) Double logTimeLow,
                @ParamKey(value = "logTimeHigh", mandatory = false) Double logTimeHigh,
                @ParamKey(value = "pauseTimeLow", mandatory = false) Double pauseTimeLow,
                PagingRequest pagingRequest) {
        final GCModel model = Analyzer.getOrOpenGCLogModel(file);
        GCModel.GCDetailFilter filter = new GCModel.GCDetailFilter(eventType, gcCause, logTimeLow, logTimeHigh, pauseTimeLow);
        promise.complete(model.getGCDetails(pagingRequest, filter));
    }

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
