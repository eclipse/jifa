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
import org.eclipse.jifa.common.enums.FileTransferState;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.util.PageViewBuilder;
import org.eclipse.jifa.common.vo.FileInfo;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.FileSupport;

import java.io.File;
import java.util.stream.Collectors;

import static org.eclipse.jifa.common.enums.FileType.HEAP_DUMP;
import static org.eclipse.jifa.hda.api.Model.Comparison;

class CompareRoute extends HeapBaseRoute {

    @RouteMeta(path = "/compare/files")
    void files(Promise<PageView<FileInfo>> promise, @ParamKey("file") String source,
               @ParamKey(value = "expected", mandatory = false) String expected, PagingRequest pagingRequest) {
        promise.complete(PageViewBuilder.build(FileSupport.info(FileType.HEAP_DUMP).stream().filter(
            fileInfo -> !fileInfo.getName().equals(source) && fileInfo.getTransferState() == FileTransferState.SUCCESS)
                                                          .sorted((i1, i2) -> Long.compare(i2.getCreationTime(),
                                                                                           i1.getCreationTime()))
                                                          .collect(Collectors.toList()), pagingRequest));
    }

    @RouteMeta(path = "/compare/summary")
    void summary(Promise<Comparison.Summary> promise, @ParamKey("file") String target,
                 @ParamKey("baseline") String baseline) {
        promise.complete(HeapBaseRoute.analyzerOf(target)
                                      .getSummaryOfComparison(new File(FileSupport.filePath(HEAP_DUMP, baseline)).toPath()));
    }

    @RouteMeta(path = "/compare/records")
    void record(Promise<PageView<Comparison.Item>> promise, @ParamKey("file") String target,
                @ParamKey("baseline") String baseline, PagingRequest pagingRequest) {
        promise.complete(
            analyzerOf(target).getItemsOfComparison(new File(FileSupport.filePath(HEAP_DUMP, baseline)).toPath(),
                                                      pagingRequest.getPage(), pagingRequest.getPageSize()));
    }
}
