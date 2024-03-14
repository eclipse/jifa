/********************************************************************************
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.server.service;

import org.eclipse.jifa.server.domain.entity.shared.user.UserEntity;
import org.eclipse.jifa.server.domain.exception.UsernamePasswordValidationException;
import org.eclipse.jifa.server.domain.security.JifaAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

public interface UserService {

    JifaAuthenticationToken login(String username, String password) throws UsernamePasswordValidationException;

    JifaAuthenticationToken handleOauth2Login(OAuth2AuthenticationToken token);

    JifaAuthenticationToken register(String name, String username, String password, boolean admin);

    default JifaAuthenticationToken register(String name, String username, String password) {
        return register(name, username, password, false);
    }

    Long getCurrentUserId();

    boolean isCurrentUserAdmin();

    String getCurrentUserJwtTokenOrNull();

    UserEntity getCurrentUser();

    UserEntity getCurrentUserRef();
}
