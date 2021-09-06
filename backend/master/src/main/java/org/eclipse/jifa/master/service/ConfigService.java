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

import io.vertx.core.Future;
import io.vertx.ext.sql.ResultSet;
import org.eclipse.jifa.master.service.orm.SQLHelper;
import org.eclipse.jifa.master.service.sql.ConfigSQL;
import org.eclipse.jifa.master.support.$;

public class ConfigService extends ServiceCenter {

    public String getConfig(String configName) {
        Future<ResultSet> future = $.async(dbClient::queryWithParams, ConfigSQL.SELECT, SQLHelper.makeSqlArgument(configName));
        ResultSet resultSet = $.await(future);
        String config = resultSet.getRows().get(0).getString("value");
        return config;
    }
}
