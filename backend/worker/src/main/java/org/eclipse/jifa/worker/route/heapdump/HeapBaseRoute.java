/********************************************************************************
 * Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation
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

import org.eclipse.jifa.hda.api.HeapDumpAnalyzer;
import org.eclipse.jifa.common.listener.ProgressListener;
import org.eclipse.jifa.worker.route.BaseRoute;
import org.eclipse.jifa.worker.route.MappingPrefix;
import org.eclipse.jifa.worker.support.Analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@MappingPrefix("/heap-dump/:file")
public class HeapBaseRoute extends BaseRoute {

    private static final List<Class<? extends HeapBaseRoute>> ROUTES = new ArrayList<>();

    static {
        ROUTES.add(OverviewRoute.class);
        ROUTES.add(ObjectRoute.class);
        ROUTES.add(InspectorRoute.class);
        ROUTES.add(DominatorTreeRoute.class);
        ROUTES.add(HistogramRoute.class);
        ROUTES.add(UnreachableObjectsRoute.class);
        ROUTES.add(ClassLoaderRoute.class);
        ROUTES.add(DuplicatedClassesRoute.class);
        ROUTES.add(SystemPropertyRoute.class);
        ROUTES.add(ThreadRoute.class);
        ROUTES.add(ObjectListRoute.class);
        ROUTES.add(ClassReferenceRoute.class);
        ROUTES.add(OQLRoute.class);
        ROUTES.add(CalciteSQLRoute.class);
        ROUTES.add(DirectByteBufferRoute.class);
        ROUTES.add(GCRootRoute.class);
        ROUTES.add(PathToGCRootsRoute.class);
        ROUTES.add(CompareRoute.class);
        ROUTES.add(LeakRoute.class);
        ROUTES.add(MergePathToGCRootsRoute.class);
        ROUTES.add(StringsRoute.class);
    }


    public static List<Class<? extends HeapBaseRoute>> routes() {
        return ROUTES;
    }

    public static HeapDumpAnalyzer analyzerOf(String dump) {
        return Analyzer.getOrBuildHeapDumpAnalyzer(dump, Collections.emptyMap(), ProgressListener.NoOpProgressListener);
    }
}
