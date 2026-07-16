package com.wambe.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@Profile({"local", "test"})
@ConditionalOnProperty(name = "wambe.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalDevStorageSecurityConfig {

    @Bean
    @Order(2)
    SecurityFilterChain localDevStorageChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/dev-storage/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
