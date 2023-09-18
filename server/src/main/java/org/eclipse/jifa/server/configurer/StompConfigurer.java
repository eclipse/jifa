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
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.condition.ConditionalOnRole;
import org.eclipse.jifa.server.domain.converter.AnalysisApiStompResponseMessageConverter;
import org.eclipse.jifa.server.enums.Role;
import org.eclipse.jifa.server.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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

    private final JwtDecoder jwtDecoder;

    private final JwtService jwtService;

    public StompConfigurer(JwtDecoder jwtDecoder, JwtService jwtService) {
        this.jwtDecoder = jwtDecoder;
        this.jwtService = jwtService;
    }

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
        registry.addEndpoint(Constant.STOMP_ENDPOINT).setAllowedOrigins("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes(Constant.STOMP_APPLICATION_DESTINATION_PREFIX)
                .setUserDestinationPrefix(Constant.STOMP_USER_DESTINATION_PREFIX)
                .setPreservePublishOrder(true)
                .configureBrokerChannel()
                .interceptors(new ExecutorChannelInterceptor() {
                    @SuppressWarnings("NullableProblems")
                    @Override
                    public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex) {
                        if (ex != null) {
                            return;
                        }
                        SimpMessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message.getHeaders(), SimpMessageHeaderAccessor.class);
                        SimpMessageType messageType = accessor.getMessageType();
                        String sessionId = accessor.getSessionId();
                        String receipt = accessor.getFirstNativeHeader(StompHeaders.RECEIPT);
                        if (messageType != SimpMessageType.SUBSCRIBE || StringUtils.isBlank(sessionId) || StringUtils.isBlank(receipt)) {
                            return;
                        }
                        if (handler instanceof SimpleBrokerMessageHandler broker) {
                            StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.RECEIPT);
                            headerAccessor.setSessionId(sessionId);
                            headerAccessor.setReceiptId(receipt);
                            broker.getClientOutboundChannel().send(MessageBuilder.createMessage(Constant.EMPTY_BYTE_ARRAY, headerAccessor.getMessageHeaders()));
                        }
                    }
                });
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @SuppressWarnings("NullableProblems")
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message,
                                                          StompHeaderAccessor.class);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    List<String> value = accessor.getNativeHeader("jifa-token");
                    String token = null;
                    if (value != null && value.size() > 0) {
                        token = value.get(0);
                    }

                    if (StringUtils.isNotBlank(token)) {
                        accessor.setUser(jwtService.convert(jwtDecoder.decode(token)));
                    }
                }
                return message;
            }
        });
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
        messageConverters.add(0, new AnalysisApiStompResponseMessageConverter());
        return false;
    }
}
