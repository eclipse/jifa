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

import jakarta.annotation.PostConstruct;
import org.eclipse.jifa.common.domain.exception.ShouldNotReachHereException;
import org.eclipse.jifa.server.domain.entity.shared.UserEntity;
import org.eclipse.jifa.server.domain.exception.UsernamePasswordValidationException;
import org.eclipse.jifa.server.domain.exception.ValidationException;
import org.eclipse.jifa.server.domain.security.JifaAuthenticationToken;
import org.eclipse.jifa.server.repository.UserRepo;
import org.eclipse.jifa.server.service.JwtService;
import org.eclipse.jifa.server.service.SensitiveDataService;
import org.eclipse.jifa.server.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.eclipse.jifa.common.domain.exception.CommonException.CE;
import static org.eclipse.jifa.common.enums.CommonErrorCode.INTERNAL_ERROR;
import static org.eclipse.jifa.server.Constant.ANONYMOUS_USERNAME;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepo userRepo;
    private final SensitiveDataService sensitiveDataService;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private UserEntity anonymousUser;

    public UserServiceImpl(UserRepo userRepo, SensitiveDataService sensitiveDataService, PasswordEncoder encoder, JwtService jwtService) {
        this.userRepo = userRepo;
        this.sensitiveDataService = sensitiveDataService;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @PostConstruct
    private void init() {
        // find or create an entity for anonymous user
        anonymousUser = userRepo.findByUsername(ANONYMOUS_USERNAME).orElseGet(() -> {
            UserEntity user = new UserEntity();
            user.setUsername(ANONYMOUS_USERNAME);
            // Anonymous user's password will not be used, so just set a random value
            user.setPassword(encoder.encode(UUID.randomUUID().toString()));
            user.setAdmin(false);
            try {
                return userRepo.save(user);
            } catch (Throwable t) {
                user = userRepo.findByUsername(ANONYMOUS_USERNAME).orElseThrow(() -> CE(INTERNAL_ERROR));
            }
            return user;
        });
    }

    @Override
    public String login(String username, String password) throws UsernamePasswordValidationException {
        if (ANONYMOUS_USERNAME.equals(username)) {
            throw new ValidationException("Username is illegal");
        }

        UserEntity entity = userRepo.findByUsername(username).orElseThrow(
                () -> new UsernamePasswordValidationException("User doesn't exist"));
        try {
            password = sensitiveDataService.decrypt(password);
        } catch (Throwable t) {
            throw new UsernamePasswordValidationException("Failed to decrypt password", t);
        }

        if (!encoder.matches(password, entity.getPassword())) {
            throw new UsernamePasswordValidationException("Incorrect password");
        }

        return jwtService.generateToken(entity);
    }

    @Override
    public void create(String username, String password, boolean admin) {
        if (ANONYMOUS_USERNAME.equals(username)) {
            throw new ValidationException("Username is illegal");
        }

        if (userRepo.findByUsername(username).isPresent()) {
            throw new ValidationException("Username exists");
        }

        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setPassword(encoder.encode(password));
        entity.setAdmin(admin);
        userRepo.save(entity);
    }

    @Override
    public long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JifaAuthenticationToken token) {
            return token.getUserId();
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            return anonymousUser.getId();
        }

        throw new ShouldNotReachHereException();
    }

    @Override
    public boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JifaAuthenticationToken token) {
            return token.isAdmin();
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }

        throw new ShouldNotReachHereException();
    }

    @Override
    public String getCurrentUserJwtToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JifaAuthenticationToken token) {
            return token.getToken();
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        throw new ShouldNotReachHereException();
    }

    @Override
    public UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JifaAuthenticationToken token) {
            return userRepo.findById(token.getUserId()).orElseThrow(() -> CE(INTERNAL_ERROR));
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            return anonymousUser;
        }

        throw new ShouldNotReachHereException();
    }

    @Override
    public long getAnonymousUserId() {
        return anonymousUser.getId();
    }
}


