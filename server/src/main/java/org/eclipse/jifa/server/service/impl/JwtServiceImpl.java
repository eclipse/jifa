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
package org.eclipse.jifa.server.service.impl;

import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.domain.entity.shared.UserEntity;
import org.eclipse.jifa.server.domain.security.JifaAuthenticationToken;
import org.eclipse.jifa.server.service.JwtService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtServiceImpl extends ConfigurationAccessor implements JwtService {

    private static final String ISSUER = "jifa";

    private static final String CLAIM_USER_ID_KEY = "userId";

    private static final String CLAIM_ADMIN_KEY = "admin";

    private final JwtEncoder jwtEncoder;

    public JwtServiceImpl(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    @Override
    public String generateToken(UserEntity user) {
        return generateToken(user.getId(), user.isAdmin());
    }

    @Override
    public String refreshToken() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context != null) {
            Authentication authentication = context.getAuthentication();
            if (authentication instanceof JifaAuthenticationToken jifaToken) {
                Instant now = Instant.now();
                Instant expiresAt = jifaToken.getExpiresAt();
                Instant refreshWindow = expiresAt.minusSeconds(Constant.JWT_REFRESH_WINDOW);
                if (now.isAfter(refreshWindow) && now.isBefore(expiresAt)) {
                    return generateToken(jifaToken.getUserId(), jifaToken.isAdmin());
                }
            }
        }
        return null;
    }

    @Override
    public JifaAuthenticationToken convert(Jwt jwt) {
        return new JifaAuthenticationToken(jwt.getClaim(CLAIM_USER_ID_KEY),
                                           jwt.getClaimAsBoolean(CLAIM_ADMIN_KEY),
                                           jwt.getTokenValue(),
                                           jwt.getExpiresAt());
    }

    private String generateToken(long userId, boolean admin) {
        Instant now = Instant.now();
        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                                             .issuer(ISSUER)
                                             .issuedAt(now)
                                             .expiresAt(now.plusSeconds(Constant.JWT_EXPIRY))
                                             .claim(CLAIM_USER_ID_KEY, userId)
                                             .claim(CLAIM_ADMIN_KEY, admin)
                                             .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claimsSet)).getTokenValue();
    }
}
