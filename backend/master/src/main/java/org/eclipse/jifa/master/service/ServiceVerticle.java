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
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.jdbc.JDBCClient;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.support.Pivot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceVerticle extends AbstractVerticle implements Constant {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceVerticle.class);

    @Override
    public void start(Promise<Void> startFuture) {
        vertx.executeBlocking(event -> {
            // init Data source
            DruidDataSource ds = new DruidDataSource();
            ds.setUrl(config().getString(DB_URL));
            ds.setUsername(config().getString(DB_USERNAME));
            ds.setPassword(config().getString(DB_PASSWORD));
            ds.setDriverClassName(config().getString(DB_DRIVER_CLASS_NAME));

            // create proxy first
            JDBCClient jdbcClient = JDBCClient.create(vertx, ds);

            Pivot pivot = Pivot.instance(vertx, jdbcClient);

            ServiceCenter.initialize(pivot, jdbcClient);
            LOGGER.info("Create service done");

            event.complete();
        });
    }
}
