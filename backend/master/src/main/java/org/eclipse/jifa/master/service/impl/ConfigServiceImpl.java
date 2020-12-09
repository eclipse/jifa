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
package org.eclipse.jifa.master.service.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.reactivex.SingleHelper;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import org.eclipse.jifa.master.service.ConfigService;
import org.eclipse.jifa.master.service.sql.ConfigSQL;

import static org.eclipse.jifa.master.service.impl.helper.SQLHelper.ja;

public class ConfigServiceImpl implements ConfigService {

    private final JDBCClient dbClient;

    public ConfigServiceImpl(JDBCClient dbClient) {
        this.dbClient = dbClient;
    }

    @Override
    public void getConfig(String configName, Handler<AsyncResult<String>> handler) {
        dbClient.rxQueryWithParams(ConfigSQL.SELECT, ja(configName))
                .map(resultSet -> resultSet.getRows().get(0).getString("value"))
                .subscribe(SingleHelper.toObserver(handler));
    }
}
