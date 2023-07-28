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
package org.eclipse.jifa.server.domain.security;

import org.eclipse.jifa.server.Constant;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

public class JifaAuthenticationToken extends AbstractAuthenticationToken {

    private static final Collection<? extends GrantedAuthority> ADMIN = Collections.singleton((GrantedAuthority) () -> Constant.ROLE_PREFIX + Constant.ROLE_ADMIN);

    private final long userId;

    private final boolean admin;

    private final String token;
    private final Instant expiresAt;

    public JifaAuthenticationToken(long userId, boolean admin, String token, Instant expiresAt) {
        super(admin ? ADMIN : Collections.emptySet());
        this.userId = userId;
        this.admin = admin;
        this.expiresAt = expiresAt;
        this.token = token;
        setAuthenticated(true);
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    public long getUserId() {
        return userId;
    }

    public boolean isAdmin() {
        return admin;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
