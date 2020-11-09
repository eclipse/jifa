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
package org.eclipse.jifa.master.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.serviceproxy.ServiceBinder;
import org.eclipse.jifa.common.enums.FileTransferState;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.master.entity.File;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.enums.Deleter;
import org.eclipse.jifa.master.model.TransferWay;
import org.eclipse.jifa.master.service.impl.FileServiceImpl;
import org.eclipse.jifa.master.service.impl.Pivot;

import java.util.List;
import java.util.Map;

@ProxyGen
@VertxGen
public interface FileService {

    @GenIgnore
    static void create(Vertx vertx, Pivot pivot, JDBCClient dbClient) {
        new ServiceBinder(vertx.getDelegate())
            .setAddress(FileService.class.getSimpleName())
            .register(FileService.class, new FileServiceImpl(pivot, dbClient));
    }

    @GenIgnore
    static void createProxy(Vertx vertx) {
        ProxyDictionary.add(FileService.class, new org.eclipse.jifa.master.service.reactivex.FileService(
            new FileServiceVertxEBProxy(vertx.getDelegate(), FileService.class.getSimpleName())));
    }

    void count(String userId, FileType type, String expectedFilename, Handler<AsyncResult<Integer>> handler);

    void files(String userId, FileType type, String expectedFilename, int page, int pageSize,
               Handler<AsyncResult<List<File>>> handler);

    void file(String name, Handler<AsyncResult<File>> handler);

    void deleteFile(String name, Deleter deleter, Handler<AsyncResult<Void>> handler);

    void transfer(String userId, FileType type, String originalName, String name, TransferWay transferWay,
                  Map<String, String> transferInfo, Handler<AsyncResult<Job>> handler);

    void transferDone(String name, FileTransferState transferState, long size,
                      Handler<AsyncResult<Void>> handler);

    void setShared(String name, Handler<AsyncResult<Void>> handler);

    void updateDisplayName(String name, String displayName, Handler<AsyncResult<Void>> handler);
}
