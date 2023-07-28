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
import org.eclipse.jifa.server.domain.converter.ApiResponseMessageConverter;
import org.eclipse.jifa.server.enums.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.List;

@ConditionalOnRole({Role.MASTER, Role.STANDALONE_WORKER})
@Configuration
@EnableWebSocketMessageBroker
public class StompConfigurer extends ConfigurationAccessor implements WebSocketMessageBrokerConfigurer {

    private final int channelPoolSize = Runtime.getRuntime().availableProcessors() * 2;

    private final int channelQueueCapacity = 64;

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        int maxBufferSize = 64 * 1024 * 1024;
        container.setMaxTextMessageBufferSize(maxBufferSize);
        container.setMaxBinaryMessageBufferSize(maxBufferSize);
        return container;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(Constant.STOMP_ENDPOINT).withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes(Constant.STOMP_APPLICATION_DESTINATION_PREFIX)
                .setUserDestinationPrefix(Constant.STOMP_USER_DESTINATION_PREFIX)
                .setPreservePublishOrder(true);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                    .corePoolSize(channelPoolSize)
                    .maxPoolSize(channelPoolSize)
                    .queueCapacity(channelQueueCapacity);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                    .corePoolSize(channelPoolSize)
                    .maxPoolSize(channelPoolSize)
                    .queueCapacity(channelQueueCapacity);
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        messageConverters.add(0, new ApiResponseMessageConverter());
        return false;
    }
}
