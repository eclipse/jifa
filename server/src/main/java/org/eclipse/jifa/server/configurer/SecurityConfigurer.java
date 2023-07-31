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

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import jakarta.annotation.Nullable;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.condition.ConditionalOnRole;
import org.eclipse.jifa.server.domain.security.JifaAuthenticationToken;
import org.eclipse.jifa.server.filter.JwtTokenRefreshFilter;
import org.eclipse.jifa.server.service.JwtService;
import org.eclipse.jifa.server.service.UserService;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;

import java.time.Duration;

import static org.eclipse.jifa.server.Constant.HTTP_API_PREFIX;
import static org.eclipse.jifa.server.enums.Role.MASTER;
import static org.eclipse.jifa.server.enums.Role.STANDALONE_WORKER;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfigurer extends ConfigurationAccessor {

    @Bean
    public JwtEncoder jwtEncoder() {
        var jwk = new RSAKey.Builder(getPublicKey()).privateKey(getPrivateKey()).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(getPublicKey()).build();

        if (getRole() == MASTER || getRole() == STANDALONE_WORKER) {
            decoder.setJwtValidator(new JwtTimestampValidator(Duration.ZERO));
        }
        return decoder;
    }

    @Bean
    public SecurityFilterChain configure(HttpSecurity hs, UserService userService, JwtService jwtService,
                                         @Nullable OAuth2ClientProperties oauth2ClientProperties) throws Exception {
        hs.cors(cors -> {
          })
          .csrf(AbstractHttpConfigurer::disable)
          .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .requestCache(cache -> cache.requestCache(new NullRequestCache()));

        hs.anonymous(customizer -> customizer.principal(Constant.ANONYMOUS_USERNAME).key("jifa"));

        hs.authorizeHttpRequests(requests -> {
            String authApiMatchers = HTTP_API_PREFIX + "/auth/**";
            requests.requestMatchers(authApiMatchers).permitAll();
            String apiMatchers = HTTP_API_PREFIX + "/**";
            if (!config.isAllowAnonymousAccess()) {
                requests.requestMatchers(apiMatchers).authenticated();
            }
            requests.anyRequest().permitAll();
        });

        hs.authenticationProvider(new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String username = (String) authentication.getPrincipal();
                String password = (String) authentication.getCredentials();
                try {
                    return userService.login(username, password);
                } catch (Throwable t) {
                    throw new AuthenticationServiceException(t.getMessage(), t);
                }
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return UsernamePasswordAuthenticationToken.class == authentication;
            }
        });

        hs.formLogin(formLogin -> {
            formLogin.successHandler((request, response, authentication) -> {
                Cookie jifaToken = new Cookie("jifa_token", ((JifaAuthenticationToken) authentication).getToken());
                jifaToken.setPath("/");
                jifaToken.setHttpOnly(false);
                response.addCookie(jifaToken);
                response.sendRedirect("/");
            });
        });

        if (oauth2ClientProperties != null && !oauth2ClientProperties.getRegistration().isEmpty()) {
            hs.oauth2Login(oauth2 -> oauth2.successHandler((request, response, authentication) -> {
                Cookie jifaToken = new Cookie("jifa_token", userService.handleOauth2Login((OAuth2AuthenticationToken) authentication).getToken());
                jifaToken.setPath("/");
                jifaToken.setHttpOnly(false);
                response.addCookie(jifaToken);
                response.sendRedirect("/");
            }));
        }

        hs.oauth2ResourceServer(rs -> rs.jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtService::convert)));

        hs.exceptionHandling(eh -> {
            // We currently don't have a custom login page, and invoking authenticationEntryPoint will disable the login page provided by spring,
            // hence we call defaultAuthenticationEntryPointFor instead.
            // eh.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint());
            eh.defaultAuthenticationEntryPointFor(new BearerTokenAuthenticationEntryPoint(), (request) -> true);
            eh.accessDeniedHandler(new BearerTokenAccessDeniedHandler());
        });
        return hs.build();
    }

    @ConditionalOnRole({MASTER, STANDALONE_WORKER})
    @Bean
    public FilterRegistrationBean<Filter> refreshJwtTokenFilter(JwtService jwtService) {
        FilterRegistrationBean<Filter> frb = new FilterRegistrationBean<>();
        frb.setFilter(new JwtTokenRefreshFilter(jwtService));
        frb.addUrlPatterns(HTTP_API_PREFIX + "/*");
        // must be after spring security filter chain
        frb.setOrder(Integer.MAX_VALUE);
        return frb;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
