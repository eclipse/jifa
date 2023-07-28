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
import org.eclipse.jifa.server.service.SensitiveDataService;
import org.eclipse.jifa.server.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnRole({Role.MASTER, Role.STANDALONE_WORKER})
@RequestMapping("auth")
@RestController
public class AuthController extends ConfigurationAccessor {

    private final UserService userService;

    private final SensitiveDataService sensitiveDataService;

    public AuthController(UserService userService, SensitiveDataService sensitiveDataService) {
        this.userService = userService;
        this.sensitiveDataService = sensitiveDataService;
    }

    @GetMapping(value = "/public-key")
    public String meta() {
        return sensitiveDataService.getPublicKeyString();
    }

    @PostMapping(value = "/login")
    public void login(@RequestParam String username, @RequestParam String password,
                      HttpServletResponse response) {
        response.addHeader(HttpHeaders.AUTHORIZATION, userService.login(username, password));
    }
}
