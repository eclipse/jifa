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
package org.eclipse.jifa.server.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.condition.ConditionalOnRole;
import org.eclipse.jifa.server.enums.Role;
import org.eclipse.jifa.server.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.eclipse.jifa.server.Constant.HTTP_LOGIN_MAPPING;
import static org.eclipse.jifa.server.Constant.HTTP_USER_MAPPING;

@ConditionalOnRole({Role.MASTER, Role.STANDALONE_WORKER})
@RestController
public class UserController extends ConfigurationAccessor {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(value = HTTP_LOGIN_MAPPING)
    public void login(@RequestBody LoginRequest request, HttpServletResponse response) throws InterruptedException {
        response.addHeader(HttpHeaders.AUTHORIZATION, userService.login(request.username, request.password).getToken());
    }

    @PostMapping(value = HTTP_USER_MAPPING)
    public void register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        if (!config.isAllowRegistration()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        response.addHeader(HttpHeaders.AUTHORIZATION,
                           userService.register(request.name, request.username, request.password).getToken());
    }

    record LoginRequest(String username, String password) {
    }

    record RegisterRequest(String name, String username, String password) {
    }
}
