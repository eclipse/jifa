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
package org.eclipse.jifa;

import com.google.gson.JsonElement;
import org.eclipse.jifa.server.Configuration;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.controller.AnalysisApiStompController;
import org.eclipse.jifa.server.domain.dto.AnalysisApiRequest;
import org.eclipse.jifa.server.domain.dto.AnalysisApiStompResponseMessage;
import org.eclipse.jifa.server.enums.AnalysisApiResponseCode;
import org.eclipse.jifa.server.service.AnalysisApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.GsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.MimeType;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("NullableProblems")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ControllerTest {

//    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
//
//    private final Configuration jifa;
//
//    @LocalServerPort
//    private Integer port;
//
//    private WebSocketStompClient webSocketStompClient;
//
//    @MockBean
//    AnalysisApiService apiService;
//
//    public ControllerTest(Configuration jifa) {
//        this.jifa = jifa;
//    }
//
//    @BeforeEach
//    void setup() {
//        this.webSocketStompClient = new WebSocketStompClient(new SockJsClient(
//                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
//        this.webSocketStompClient.setMessageConverter(new GsonMessageConverter());
//        this.webSocketStompClient.setTaskScheduler(new ConcurrentTaskScheduler());
//    }
//
//    @Test
//    public void test() throws Throwable {
//
//        {
//            String api = "test.request";
//            Mockito.when(apiService.lookup(Mockito.eq(api))).thenReturn(new ApiMeta(api, null, true));
//
//            CompletableFuture<AnalysisApiStompResponseMessage> response = new CompletableFuture<>();
//            Mockito.when(apiService.invoke(Mockito.any()))
//                   .then(invocation -> {
//                       new Thread(() -> {
//                           try {
//                               Thread.sleep(2000);
//                               response.complete(new AnalysisApiStompResponseMessage(AnalysisApiResponseCode.SUCCESS, AnalysisApiStompResponseMessage.toData("SUCCESS"), invocation.getArgument(1)));
//                           } catch (Throwable ignored) {
//                           }
//                       }).start();
//                       return response;
//                   });
//
//            HttpClient httpClient = HttpClient.newHttpClient();
//            HttpRequest httpRequest = HttpRequest.newBuilder(new URI(String.format("http://localhost:%d%s/%s", port, Constant.HTTP_API_PREFIX, api)))
//                                                 .GET()
//                                                 .build();
//
//            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
//
//            System.out.println("http response = " + httpResponse.body());
//        }
//
//        {
//            StompSession session = webSocketStompClient
//                    .connectAsync(String.format("ws://localhost:%d%s", port, Constant.STOMP_ENDPOINT), new StompSessionHandlerAdapter() {
//                    })
//                    .get(1000, SECONDS);
//
//            Map<String, CountDownLatch> countDownLatchMap = new ConcurrentHashMap<>();
//            Map<String, Boolean> resultMap = new ConcurrentHashMap<>();
//
//            session.subscribe(Constant.STOMP_USER_DESTINATION_PREFIX + "/*", new StompSessionHandlerAdapter() {
//
//                @Override
//                public Type getPayloadType(StompHeaders headers) {
//                    return JsonElement.class;
//                }
//
//                @Override
//                public void handleTransportError(StompSession session, Throwable exception) {
//                    exception.printStackTrace();
//                }
//
//                @Override
//                public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
//                    exception.printStackTrace();
//                }
//
//                @Override
//                public void handleFrame(StompHeaders headers, Object payload) {
//
//                    LOGGER.info("Handling a frame, header = {}, payload = {}", headers, payload);
//
//                    String requestId = headers.getFirst(Constant.STOMP_ANALYSIS_API_REQUEST_ID_KEY);
//
//                    try {
//                        assertNotNull(requestId);
//
//                        if (AnalysisApiResponseCode.SUCCESS.name().equals(headers.getFirst(Constant.STOMP_ANALYSIS_API_RESPONSE_SUCCESS_KEY))) {
//                            resultMap.put(requestId, true);
//                            countDownLatchMap.get(requestId).countDown();
//                        }
//                    } catch (Throwable t) {
//                        LOGGER.error("Fail to handle frame", t);
//                        if (requestId != null) {
//                            resultMap.put(requestId, false);
//                            countDownLatchMap.get(requestId).countDown();
//                        }
//                    }
//                }
//            });
//
//            String api = "test.request";
//            Mockito.when(apiService.lookup(Mockito.eq(api))).thenReturn(new ApiMeta(api, null, true));
//
//            CompletableFuture<AnalysisApiStompResponseMessage> response = new CompletableFuture<>();
//            Mockito.when(apiService.invoke(Mockito.any()))
//                   .then(invocation -> {
//                       invocation.<AnalysisApiRequest.AcceptedCallback<AnalysisApiStompController.PassThrough>>getArgument(2).accept(invocation.getArgument(1));
//                       response.complete(new AnalysisApiStompResponseMessage(AnalysisApiResponseCode.SUCCESS, AnalysisApiStompResponseMessage.toData("SUCCESS"), invocation.getArgument(1)));
//                       return response;
//                   });
//
//            MimeType type = new MimeType("application", "json", StandardCharsets.UTF_8);
//            Map<String, Object> payload = new HashMap<>();
//            payload.put("a", "b");
//            {
//                String id = UUID.randomUUID().toString();
//                countDownLatchMap.put(id, new CountDownLatch(1));
//                StompHeaders headers = new StompHeaders();
//                headers.setDestination(Constant.STOMP_APPLICATION_DESTINATION_PREFIX + "/test.request");
//                headers.set(Constant.STOMP_ANALYSIS_API_REQUEST_ID_KEY, id);
//                headers.setContentType(type);
//                session.send(headers, payload);
//
//                assertTrue(countDownLatchMap.get(id).await(1, SECONDS));
//                assertTrue(resultMap.computeIfAbsent(id, i -> false));
//            }
//        }
//    }
}
