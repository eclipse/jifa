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

import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.condition.ConditionalOnRole;
import org.eclipse.jifa.server.enums.Role;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@SuppressWarnings("NullableProblems")
@Configuration
@EnableWebMvc
public class HttpConfigurer extends ConfigurationAccessor implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (isMaster() || isStandaloneWorker()) {
            registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/");
        }
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        if (isMaster() || isStandaloneWorker()) {
            String viewName = "forward:index.html";
            registry.addViewController("/").setViewName(viewName);
            registry.addViewController("/heapDump").setViewName(viewName);
            registry.addViewController("/gcLog").setViewName(viewName);
            registry.addViewController("/gcLogCompare").setViewName(viewName);
            registry.addViewController("/threadDump").setViewName(viewName);
        }
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(Constant.HTTP_API_PREFIX, i -> true);
    }

    @ConditionalOnRole({Role.MASTER, Role.STANDALONE_WORKER})
    @Bean
    public InternalResourceViewResolver defaultViewResolver() {
        return new InternalResourceViewResolver();
    }

    @Bean
    TomcatServletWebServerFactory tomcatServletWebServerFactory() {
        TomcatServletWebServerFactory tomcatServletWebServerFactory = new TomcatServletWebServerFactory();
        tomcatServletWebServerFactory.addConnectorCustomizers(connector -> connector.setAsyncTimeout(-1));
        return tomcatServletWebServerFactory;
    }
}
