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
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.common.vo.support.SearchType;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;

import static org.eclipse.jifa.hda.api.Model.DuplicatedClass;

class DuplicatedClassesRoute extends HeapBaseRoute {

    @RouteMeta(path = "/duplicatedClasses/classes")
    void classRecords(Promise<PageView<DuplicatedClass.ClassItem>> promise, @ParamKey("file") String file,
                      @ParamKey(value = "searchText", mandatory = false) String searchText,
                      @ParamKey(value = "searchType", mandatory = false) SearchType searchType,
                      PagingRequest pagingRequest) {
        promise.complete(analyzerOf(file).getDuplicatedClasses(searchText, searchType, pagingRequest.getPage(),
                                                               pagingRequest.getPageSize()));
    }

    @RouteMeta(path = "/duplicatedClasses/classLoaders")
    void classLoaderRecords(Promise<PageView<DuplicatedClass.ClassLoaderItem>> promise, @ParamKey("file") String file,
                            @ParamKey("index") int index,
                            PagingRequest pagingRequest) {
        promise.complete(analyzerOf(file).getClassloadersOfDuplicatedClass(index, pagingRequest.getPage(),
                                                                           pagingRequest.getPageSize()));
    }
}
