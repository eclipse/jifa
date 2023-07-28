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
import jakarta.servlet.Filter;
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.condition.ConditionalOnRole;
import org.eclipse.jifa.server.filter.JwtTokenRefreshFilter;
import org.eclipse.jifa.server.service.JwtService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    public SecurityFilterChain configure(HttpSecurity hs, JwtService jwtService) throws Exception {
        String adminApiPrefix = Constant.HTTP_API_PREFIX + "/" + Constant.ADMIN_ONLY + "/**";

        hs.cors(customizer -> {
          })
          .csrf(AbstractHttpConfigurer::disable)
          .sessionManagement(
                  customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(
                  customizer -> customizer.requestMatchers(adminApiPrefix).hasRole(Constant.ROLE_ADMIN)
                                          .requestMatchers(Constant.HTTP_API_PREFIX + "/auth/**").permitAll())
          .oauth2ResourceServer(customizer -> customizer.jwt(jwtCustomizer -> jwtCustomizer.jwtAuthenticationConverter(jwtService::convert)))
          .exceptionHandling(
                  customizer -> customizer.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                                          .accessDeniedHandler(new BearerTokenAccessDeniedHandler()))
          .requestCache(customizer -> customizer.requestCache(new NullRequestCache()));

        if (config.isAllowAnonymousAccess()) {
            hs.anonymous(customizer -> customizer.principal(Constant.ANONYMOUS_USERNAME).key("jifa"))
              .authorizeHttpRequests(customizer -> customizer.anyRequest().anonymous());
        } else {
            hs.authorizeHttpRequests(customizer -> customizer.anyRequest().authenticated());
        }
        return hs.build();
    }

    @ConditionalOnRole({MASTER, STANDALONE_WORKER})
    @Bean
    public FilterRegistrationBean<Filter> refreshJwtTokenFilter(JwtService jwtService) {
        FilterRegistrationBean<Filter> frb = new FilterRegistrationBean<>();
        frb.setFilter(new JwtTokenRefreshFilter(jwtService));
        frb.addUrlPatterns(Constant.HTTP_API_PREFIX + "/*");
        // must be after spring security filter chain
        frb.setOrder(Integer.MAX_VALUE);
        return frb;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
