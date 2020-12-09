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
import org.eclipse.jifa.master.service.impl.ConfigServiceImpl;

@ProxyGen
@VertxGen
public interface ConfigService {

    @GenIgnore
    static void create(Vertx vertx, JDBCClient dbClient) {
        new ServiceBinder(vertx.getDelegate()).setAddress(ConfigService.class.getSimpleName())
                                              .register(ConfigService.class, new ConfigServiceImpl(dbClient));
    }

    @GenIgnore
    static void createProxy(Vertx vertx) {
        ProxyDictionary.add(ConfigService.class, new org.eclipse.jifa.master.service.reactivex.ConfigService(
            new ConfigServiceVertxEBProxy(vertx.getDelegate(), ConfigService.class.getSimpleName())));
    }

    void getConfig(String configName, Handler<AsyncResult<String>> handler);

}
