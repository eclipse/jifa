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

import io.vertx.core.Promise;

import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.tda.ThreadDumpAnalyzer;
import org.eclipse.jifa.tda.diagnoser.Diagnostic;
import org.eclipse.jifa.tda.diagnoser.ThreadDumpAnalysisConfig;
import org.eclipse.jifa.tda.enums.MonitorState;
import org.eclipse.jifa.tda.enums.ThreadType;
import org.eclipse.jifa.tda.vo.Content;
import org.eclipse.jifa.tda.vo.Overview;
import org.eclipse.jifa.tda.vo.VBlockingThread;
import org.eclipse.jifa.tda.vo.VFrame;
import org.eclipse.jifa.tda.vo.VMonitor;
import org.eclipse.jifa.tda.vo.VThread;
import org.eclipse.jifa.worker.route.HttpMethod;
import org.eclipse.jifa.worker.route.MappingPrefix;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@MappingPrefix("/:file")
public class ThreadDumpRoute extends ThreadDumpBaseRoute {

    @RouteMeta(path = "/overview")
    public void overview(Promise<Overview> promise, @ParamKey("file") String file) {
        ThreadDumpAnalyzer analyzer = Analyzer.threadDumpAnalyzerOf(file);
        promise.complete(analyzer.overview());
    }

    @RouteMeta(path = "/callSiteTree")
    public void callSiteTree(Promise<PageView<VFrame>> promise,
                             @ParamKey("file") String file,
                             @ParamKey("parentId") int parentId,
                             PagingRequest paging) {
        ThreadDumpAnalyzer analyzer = Analyzer.threadDumpAnalyzerOf(file);
        promise.complete(analyzer.callSiteTree(parentId, paging));
    }

    @RouteMeta(path = "/thread")
    public void thread(Promise<VThread> promise,
                        @ParamKey("file") String file,
                        @ParamKey(value = "id", mandatory = true) int id) {
        ThreadDumpAnalyzer analyzer = Analyzer.threadDumpAnalyzerOf(file);
        promise.complete(analyzer.thread(id));
    }

    @RouteMeta(path = "/threads")
    public void threads(Promise<PageView<VThread>> promise,
                        @ParamKey("file") String file,
                        @ParamKey(value = "name", mandatory = false) String name,
                        @ParamKey(value = "type", mandatory = false) ThreadType type,
                        @ParamKey(value = "state", mandatory = false) String state,
                        @ParamKey(value = "id", mandatory = false) List<Integer> id,
                        PagingRequest paging) {
        ThreadDumpAnalyzer analyzer = Analyzer.threadDumpAnalyzerOf(file);
        promise.complete(analyzer.threads(name, type, state, id, paging));
    }

    @RouteMeta(path = "/threadsOfGroup")
    public void threadsOfGroup(Promise<PageView<VThread>> promise,
                               @ParamKey("file") String file,
                               @ParamKey(value = "groupName") String groupName,
                               PagingRequest paging) {
        ThreadDumpAnalyzer analyzer = Analyzer.threadDumpAnalyzerOf(file);
        promise.complete(analyzer.threadsOfGroup(groupName, paging));
    }

    @RouteMeta(path = "/rawContentOfThread")
    public void rawContentOfThread(Promise<List<String>> promise,
                                   @ParamKey("file") String file,
                                   @ParamKey("id") int id) throws IOException {
        ThreadDumpAnalyzer analyzer = Analyzer.threadDumpAnalyzerOf(file);
        promise.complete(analyzer.rawContentOfThread(id));
    }

    @RouteMeta(path = "/content")
    public void content(Promise<Content> promise,
                        @ParamKey("file") String file,
                        @ParamKey("lineNo") int lineNo,
                        @ParamKey("lineLimit") int lineLimit) throws IOException {
        ThreadDumpAnalyzer analyzer = Analyzer.threadDumpAnalyzerOf(file);
        promise.complete(analyzer.content(lineNo, lineLimit));
    }

    @RouteMeta(path = "/monitors")
    public void monitors(Promise<PageView<VMonitor>> promise, @ParamKey("file") String file, PagingRequest paging) {
        ThreadDumpAnalyzer analyzer = Analyzer.threadDumpAnalyzerOf(file);
        promise.complete(analyzer.monitors(paging));
    }

    @RouteMeta(path = "/threadCountsByMonitor")
    public void threadCountsByMonitor(Promise<Map<MonitorState, Integer>> promise, @ParamKey("file") String file,
                                      @ParamKey("id") int id) {
        ThreadDumpAnalyzer analyzer = Analyzer.threadDumpAnalyzerOf(file);
        promise.complete(analyzer.threadCountsByMonitor(id));
    }

    @RouteMeta(path = "/threadsByMonitor")
    public void threadsByMonitor(Promise<PageView<VThread>> promise, @ParamKey("file") String file,
                                 @ParamKey("id") int id, @ParamKey("state") MonitorState state,
                                 PagingRequest paging) {
        ThreadDumpAnalyzer analyzer = Analyzer.threadDumpAnalyzerOf(file);
        promise.complete(analyzer.threadsByMonitor(id, state, paging));
    }

    /**
     * returns a list of threads that are blocking other threads
     * @param promise
     * @param file file id of the thread dump
     */
    @RouteMeta(path = "/threadsBlocking")
    public void threadsBlocking(Promise<List<VBlockingThread>> promise, @ParamKey("file") String file) {
        ThreadDumpAnalyzer analyzer = Analyzer.threadDumpAnalyzerOf(file);
        promise.complete(analyzer.blockingThreads());
    }

    /**
     * returns a list of threads that are consuming the most CPU. The threads are sorted
     * from most CPU consuming, to least
     * @param promise
     * @param file file id of the thread dump
     * @param max the max amount of threads to include (<code>-1</code> for unlimited)
     * @param threadType filter the results for a specfic thread type (may be <code>null</code>)
     */
    @RouteMeta(path = "/cpuConsumingThreads")
    public void cpuConsumingThreads(Promise<List<VThread>> promise, @ParamKey("file") String file, @ParamKey(value="max",  mandatory = false) int max, @ParamKey(value="type", mandatory = false) ThreadType threadType) {
        ThreadDumpAnalyzer analyzer = Analyzer.threadDumpAnalyzerOf(file);
        promise.complete(analyzer.cpuConsumingThreads(threadType, max > 0 ? max : Integer.MAX_VALUE));
    }

    @RouteMeta(path = "/diagnoseInfo", method = HttpMethod.GET)
    public void getDiagnoseInfo(Promise<List<Diagnostic>> promise,
                          @ParamKey("file") String file,
                          @ParamKey(value = "config", mandatory = false) ThreadDumpAnalysisConfig config) {
        ThreadDumpAnalyzer analyzer = Analyzer.threadDumpAnalyzerOf(file);
        promise.complete(analyzer.diagnose(Optional.ofNullable(config).orElse(new ThreadDumpAnalysisConfig())));
    }
}
