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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceVerticle extends AbstractVerticle implements Constant {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceVerticle.class);

    @Override
    public void start(Promise <Void> startFuture) {

        vertx.rxExecuteBlocking(future -> {
            // init Data source
            DruidDataSource ds = new DruidDataSource();
            ds.setUrl(config().getString(DB_URL));
            ds.setUsername(config().getString(DB_USERNAME));
            ds.setPassword(config().getString(DB_PASSWORD));
            ds.setDriverClassName(config().getString(DB_DRIVER_CLASS_NAME));

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

            future.complete(Single.just(this));
        }).subscribe(f -> startFuture.complete(), startFuture::fail);
    }


}
