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
import org.eclipse.jifa.worker.vo.heapdump.overview.BigObject;
import org.eclipse.jifa.worker.vo.heapdump.overview.Details;
import org.eclipse.mat.inspections.BiggestObjectsPieQuery;
import org.eclipse.mat.query.IResultPie;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.SnapshotInfo;

import java.util.List;
import java.util.stream.Collectors;

class OverviewRoute extends HeapBaseRoute {

    @RouteMeta(path = "/details")
    void details(Future<Details> future, @ParamKey("file") String file) {
        SnapshotInfo snapshotInfo = Analyzer.getOrOpenSnapshotContext(file).getSnapshot().getSnapshotInfo();
        Details details = new Details(snapshotInfo.getJvmInfo(), snapshotInfo.getIdentifierSize(),
                                      snapshotInfo.getCreationDate().getTime(), snapshotInfo.getNumberOfObjects(),
                                      snapshotInfo.getNumberOfGCRoots(), snapshotInfo.getNumberOfClasses(),
                                      snapshotInfo.getNumberOfClassLoaders(), snapshotInfo.getUsedHeapSize(),
                                      snapshotInfo.layoutAvailable());
        future.complete(details);
    }

    @RouteMeta(path = "/biggestObjects")
    void biggestObjects(Future<List<BigObject>> future, @ParamKey("file") String file) throws Exception {
        ISnapshot snapshot = Analyzer.getOrOpenSnapshotContext(file).getSnapshot();
        BiggestObjectsPieQuery query = new BiggestObjectsPieQuery();
        query.snapshot = snapshot;
        List<? extends IResultPie.Slice> slices = query.execute(HeapDumpSupport.VOID_LISTENER).getSlices();
        future.complete(slices.stream()
                              .map(slice -> new BigObject(slice.getLabel(),
                                                          slice.getContext() != null ? slice.getContext().getObjectId()
                                                                                     : HeapDumpSupport.ILLEGAL_OBJECT_ID,
                                                          slice.getValue(), slice.getDescription()))
                              .collect(Collectors.toList()));
    }
}
