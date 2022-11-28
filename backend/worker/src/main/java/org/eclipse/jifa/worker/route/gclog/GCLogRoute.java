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

import org.eclipse.jifa.gclog.diagnoser.AnalysisConfig;
import org.eclipse.jifa.gclog.diagnoser.GlobalDiagnoser;
import org.eclipse.jifa.gclog.vo.GCEventVO;
import org.eclipse.jifa.gclog.model.modeInfo.GCLogMetadata;
import org.eclipse.jifa.gclog.model.GCModel;
import io.vertx.core.Promise;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.gclog.model.modeInfo.VmOptions;
import org.eclipse.jifa.gclog.vo.*;
import org.eclipse.jifa.worker.route.HttpMethod;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;

import java.util.List;
import java.util.Map;

public class GCLogRoute extends org.eclipse.jifa.worker.route.gclog.GCLogBaseRoute {
    @RouteMeta(path = "/metadata")
    void metadata(Promise<GCLogMetadata> promise, @ParamKey("file") String file) {
        final GCModel model = Analyzer.getOrOpenGCLogModel(file);
        promise.complete(model.getGcModelMetadata());
    }

    @RouteMeta(path = "/objectStatistics")
    void objectStats(Promise<ObjectStatistics> promise, @ParamKey("file") String file,
                     @ParamKey("start") double start,
                     @ParamKey("end") double end) {
        final GCModel model = Analyzer.getOrOpenGCLogModel(file);
        promise.complete(model.getObjectStatistics(new TimeRange(start, end)));
    }

    @RouteMeta(path = "/memoryStatistics")
    void memoryStats(Promise<MemoryStatistics> promise, @ParamKey("file") String file,
                     @ParamKey("start") double start,
                     @ParamKey("end") double end) {
        final GCModel model = Analyzer.getOrOpenGCLogModel(file);
        promise.complete(model.getMemoryStatistics(new TimeRange(start, end)));
    }

    @RouteMeta(path = "/pauseDistribution")
    void pauseStats(Promise<Map<String, int[]>> promise, @ParamKey("file") String file,
                    @ParamKey("start") double start,
                    @ParamKey("end") double end,
                    @ParamKey("partitions") int[] partitions) {
        final GCModel model = Analyzer.getOrOpenGCLogModel(file);
        promise.complete(model.getPauseDistribution(new TimeRange(start, end), partitions));
    }

    @RouteMeta(path = "/pauseStatistics")
    void pauseStats(Promise<PauseStatistics> promise, @ParamKey("file") String file,
                    @ParamKey("start") double start,
                    @ParamKey("end") double end) {
        final GCModel model = Analyzer.getOrOpenGCLogModel(file);
        promise.complete(model.getPauseStatistics(new TimeRange(start, end)));
    }

    @RouteMeta(path = "/phaseStatistics")
    void phaseStats(Promise<PhaseStatistics> promise, @ParamKey("file") String file,
                    @ParamKey("start") double start,
                    @ParamKey("end") double end) {
        final GCModel model = Analyzer.getOrOpenGCLogModel(file);
        promise.complete(model.getPhaseStatistics(new TimeRange(start, end)));
    }

    @RouteMeta(path = "/gcDetails")
    void detail(Promise<PageView<GCEventVO>> promise, @ParamKey("file") String file,
                @ParamKey(value = "eventType", mandatory = false) String eventType,
                @ParamKey(value = "gcCause", mandatory = false) String gcCause,
                @ParamKey(value = "logTimeLow", mandatory = false) Double logTimeLow,
                @ParamKey(value = "logTimeHigh", mandatory = false) Double logTimeHigh,
                @ParamKey(value = "pauseTimeLow", mandatory = false) Double pauseTimeLow,
                // time range of config is ignored for the time being
                @ParamKey("config") AnalysisConfig config,
                PagingRequest pagingRequest) {
        final GCModel model = Analyzer.getOrOpenGCLogModel(file);
        GCModel.GCDetailFilter filter = new GCModel.GCDetailFilter(eventType, gcCause, logTimeLow, logTimeHigh, pauseTimeLow);
        promise.complete(model.getGCDetails(pagingRequest, filter, config));
    }

    @RouteMeta(path = "/vmOptions", method = HttpMethod.GET)
    void getVMOptions(Promise<VmOptions.VmOptionResult> promise,
                      @ParamKey("file") String file) {
        final GCModel model = Analyzer.getOrOpenGCLogModel(file);
        VmOptions options = model.getVmOptions();
        promise.complete(options == null ? null : options.getVmOptionResult());
    }

    @RouteMeta(path = "/timeGraphData", method = HttpMethod.GET)
    void getTimeGraphData(Promise<Map<String, List<Object[]>>> promise,
                          @ParamKey("file") String file,
                          @ParamKey("dataTypes") String[] dateTypes) {
        final GCModel model = Analyzer.getOrOpenGCLogModel(file);
        promise.complete(model.getTimeGraphData(dateTypes));
    }


    @RouteMeta(path = "/vmOptions", method = HttpMethod.POST)
    void setVMOptions(Promise<Void> promise,
                      @ParamKey("file") String file, @ParamKey("options") String options) {
        final GCModel model = Analyzer.getOrOpenGCLogModel(file);
        model.setVmOptions(new VmOptions(options));
        promise.complete();
    }

    @RouteMeta(path = "/diagnoseInfo", method = HttpMethod.GET)
    void getDiagnoseInfo(Promise<GlobalDiagnoser.GlobalAbnormalInfo> promise,
                          @ParamKey("file") String file,
                          @ParamKey("config") AnalysisConfig config) {
        final GCModel model = Analyzer.getOrOpenGCLogModel(file);
        promise.complete(model.getGlobalAbnormalInfo(config));
    }
}
