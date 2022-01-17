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

package org.eclipse.jifa.worker.route.gclog;

import org.eclipse.jifa.gclog.model.GCModel;
import io.vertx.core.Promise;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;

public class GCMetadataRoute extends GCLogBaseRoute {
    @RouteMeta(path = "/metadata")
    void detailMetadata(Promise<GCModel.GCLogDetailMetadata> promise, @ParamKey("file") String file) {
        final GCModel model = Analyzer.getOrOpenGCLogModel(file);
        promise.complete(model.getGcDetailMetadata());
    }
}
