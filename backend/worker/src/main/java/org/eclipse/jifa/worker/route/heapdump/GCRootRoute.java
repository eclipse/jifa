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
import org.eclipse.jifa.hda.api.Model;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;

import java.util.List;

import static org.eclipse.jifa.hda.api.Model.GCRoot;

class GCRootRoute extends HeapBaseRoute {

    @RouteMeta(path = "/GCRoots")
    void roots(Promise<List<GCRoot.Item>> promise, @ParamKey("file") String file) {
        promise.complete(analyzerOf(file).getGCRoots());
    }

    @RouteMeta(path = "/GCRoots/classes")
    void classes(Promise<PageView<GCRoot.Item>> promise, @ParamKey("file") String file,
                 @ParamKey("rootTypeIndex") int rootTypeIndex, PagingRequest pagingRequest) {
        promise.complete(analyzerOf(file).getClassesOfGCRoot(rootTypeIndex, pagingRequest.getPage(),
                                                             pagingRequest.getPageSize()));
    }

    @RouteMeta(path = "/GCRoots/class/objects")
    void objects(Promise<PageView<Model.JavaObject>> promise, @ParamKey("file") String file,
                 @ParamKey("rootTypeIndex") int rootTypeIndex, @ParamKey("classIndex") int classIndex,
                 PagingRequest pagingRequest) {
        promise.complete(analyzerOf(file).getObjectsOfGCRoot(rootTypeIndex, classIndex,
                                                             pagingRequest.getPage(),
                                                             pagingRequest.getPageSize()));
    }
}
