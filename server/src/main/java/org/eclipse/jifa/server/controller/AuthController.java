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

import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.condition.ConditionalOnRole;
import org.eclipse.jifa.server.enums.Role;
import org.eclipse.jifa.server.service.CipherService;
import org.eclipse.jifa.server.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnRole({Role.MASTER, Role.STANDALONE_WORKER})
@RequestMapping("auth")
@RestController
public class AuthController extends ConfigurationAccessor {

    private final UserService userService;

    private final CipherService cipherService;

    public AuthController(UserService userService, CipherService cipherService) {
        this.userService = userService;
        this.cipherService = cipherService;
    }

    @GetMapping(value = "/public-key")
    public String meta() {
        return cipherService.getPublicKeyString();
    }

//    @PostMapping(value = "/login")
//    public void login(@RequestParam String username, @RequestParam String password,
//                      HttpServletResponse response) {
//        response.addHeader(HttpHeaders.AUTHORIZATION, userService.login(username, password));
//    }
}
