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
package org.eclipse.jifa.master.service;

import com.alibaba.druid.pool.DruidDataSource;
import io.reactivex.Single;
import io.vertx.core.Promise;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.support.K8SWorkerScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ServiceVerticle extends AbstractVerticle implements Constant {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceVerticle.class);

    @Override
    public void start(Promise <Void> startFuture) {

        vertx.rxExecuteBlocking(future -> {
            // init data source
            DruidDataSource ds = new DruidDataSource();
            ds.setUrl(config().getJsonObject(DB_KEYWORD).getString(DB_URL));
            ds.setUsername(config().getJsonObject(DB_KEYWORD).getString(DB_USERNAME));
            ds.setPassword(config().getJsonObject(DB_KEYWORD).getString(DB_PASSWORD));
            ds.setDriverClassName(config().getJsonObject(DB_KEYWORD).getString(DB_DRIVER_CLASS_NAME));

            // create proxy first
            AdminService.createProxy(vertx);
            JobService.createProxy(vertx);
            ConfigService.createProxy(vertx);
            WorkerService.createProxy(vertx);
            FileService.createProxy(vertx);
            LOGGER.info("Create service proxy done");

            JDBCClient jdbcClient = new JDBCClient(io.vertx.ext.jdbc.JDBCClient.create(vertx.getDelegate(), ds));

            Pivot pivot = Pivot.instance(vertx, jdbcClient);

            JobService.create(vertx, pivot, jdbcClient);
            FileService.create(vertx, pivot, jdbcClient);
            ConfigService.create(vertx, jdbcClient);
            AdminService.create(vertx, jdbcClient);
            WorkerService.create(vertx, jdbcClient, pivot);
            LOGGER.info("Create service done");

            // K8S
            Map<String, String> args = new HashMap<>();
            args.put(Constant.K8S_NAMESPACE, config().getJsonObject(K8S_KEYWORD).getString(Constant.K8S_NAMESPACE));
            args.put(Constant.K8S_WORKER_IMAGE, config().getJsonObject(K8S_KEYWORD).getString(Constant.K8S_WORKER_IMAGE));
            args.put(Constant.K8S_MINIMAL_MEM_REQ, config().getJsonObject(K8S_KEYWORD).getString(Constant.K8S_MINIMAL_MEM_REQ));
            pivot.getWorkerScheduler().initialize(args);
            LOGGER.info("Configure K8S done");

            future.complete(Single.just(this));
        }).subscribe(f -> startFuture.complete(), startFuture::fail);
    }


}
