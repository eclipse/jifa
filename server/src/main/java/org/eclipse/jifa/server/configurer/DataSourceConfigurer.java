/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.server.configurer;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jifa.common.util.Validate;
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Configuration
public class DataSourceConfigurer extends ConfigurationAccessor {

    private String mysqlHost;

    private String mysqlDBName;

    private String mysqlUsername;

    private String mysqlPassword;

    private void initMysqlConfig() {
        mysqlHost = System.getenv("MYSQL_HOST");
        if (mysqlHost != null) {
            Validate.notBlank(mysqlHost);
            mysqlDBName = System.getenv("MYSQL_DATABASE_NAME");
            if (StringUtils.isBlank(mysqlDBName)) {
                mysqlDBName = "jifa";
            }
            Validate.notBlank(mysqlDBName);
            mysqlUsername = System.getenv("MYSQL_USERNAME");
            mysqlPassword = System.getenv("MYSQL_PASSWORD");
            Validate.notBlank(mysqlUsername);
            Validate.notBlank(mysqlPassword);
        }
    }

    @Bean
    public DataSource getDataSource() {
        initMysqlConfig();
        if (mysqlHost != null) {
            DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
            dataSourceBuilder.driverClassName("com.mysql.cj.jdbc.Driver");
            dataSourceBuilder.url(String.format("jdbc:mysql://%s/%s?createDatabaseIfNotExist=true", mysqlHost, mysqlDBName));
            dataSourceBuilder.username(mysqlUsername);
            dataSourceBuilder.password(mysqlPassword);
            return dataSourceBuilder.build();
        } else if (isStandaloneWorker()) {
            DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
            dataSourceBuilder.driverClassName("org.h2.Driver");
            dataSourceBuilder.url(String.format("jdbc:h2:file:%s/.h2db", config.getStoragePath().toString()));
            dataSourceBuilder.username("jifa");
            dataSourceBuilder.password("jifa");
            return dataSourceBuilder.build();
        }

        return Validate.error("Mysql environments must be configured");
    }
}
