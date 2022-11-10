/********************************************************************************
 * Copyright (c) 2022, Contributors to the Eclipse Foundation
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

class StringsRoute extends HeapBaseRoute {

    @RouteMeta(path = "/findStrings")
    void strings(Promise<PageView<Model.TheString.Item>> promise, @ParamKey("file") String file,
                 @ParamKey("pattern") String pattern, PagingRequest pagingRequest) {
        promise.complete(analyzerOf(file).getStrings(pattern, pagingRequest.getPage(), pagingRequest.getPageSize()));
    }

}
