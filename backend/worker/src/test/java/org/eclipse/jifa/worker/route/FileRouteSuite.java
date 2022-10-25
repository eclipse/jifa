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
package org.eclipse.jifa.worker.route;

import com.google.gson.reflect.TypeToken;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.common.vo.FileInfo;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.worker.Worker;
import org.eclipse.jifa.worker.WorkerGlobal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

import static org.eclipse.jifa.common.util.GsonHolder.GSON;

public class FileRouteSuite extends Base {
    private static Logger LOGGER = LoggerFactory.getLogger(FileRouteSuite.class);

    public static void test(TestContext context) {
        Async async = context.async();
        LOGGER.info("port = {}, host = {}, uri = {}", WorkerGlobal.PORT, WorkerGlobal.HOST, uri("/files"));
        CLIENT.get(WorkerGlobal.PORT, WorkerGlobal.HOST, uri("/files"))
              .addQueryParam("type", FileType.HEAP_DUMP.name())
              .addQueryParam("page", "1")
              .addQueryParam("pageSize", "10")
              .send(ar -> {
                  context.assertTrue(ar.succeeded(), ar.cause() != null ? ar.cause().getMessage() : "");
                  Type type = new TypeToken<PageView<FileInfo>>() {
                  }.getType();
                  PageView<FileInfo> view = GSON.fromJson(ar.result().bodyAsString(), type);
                  context.assertTrue(view.getData().size() > 0, ar.result().bodyAsString());
                  async.complete();
              });
    }
}
