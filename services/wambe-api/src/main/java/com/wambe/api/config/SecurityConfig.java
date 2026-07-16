package com.wambe.api.config;

import com.wambe.api.common.security.HostJwtClaimValidator;
import com.wambe.api.common.security.InternalJobKeyFilter;
import com.wambe.api.common.security.RequestEnvelopeFilter;
import com.wambe.api.common.security.ScannerHmacFilter;
import com.wambe.api.observability.WambeMetrics;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain scannerChain(
            HttpSecurity http,
            RequestEnvelopeFilter requestEnvelopeFilter,
            ScannerHmacFilter scannerHmacFilter) throws Exception {
        return http
                .securityMatcher("/api/v1/internal/scanner/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(scannerHmacFilter, AnonymousAuthenticationFilter.class)
                .addFilterBefore(requestEnvelopeFilter, ScannerHmacFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("SCANNER"))
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain internalJobsChain(
            HttpSecurity http,
            InternalJobKeyFilter internalJobKeyFilter,
            @Qualifier("internalJwtDecoder") JwtDecoder internalJwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http
                .securityMatcher("/api/v1/internal/jobs/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(internalJobKeyFilter, AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAnyRole("INTERNAL_JOB", "SCHEDULER"))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                        .decoder(internalJwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    @Order(4)
    SecurityFilterChain hostChain(
            HttpSecurity http,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource,
            @Qualifier("hostJwtDecoder") JwtDecoder hostJwtDecoder) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/api/v1/public/**")
                        .permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(hostJwtDecoder)))
                .build();
    }

    @Bean
    JwtDecoder hostJwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwksUri,
            @Value("${wambe.auth.issuer}") String issuer,
            @Value("${wambe.auth.audience}") String audience,
            HostJwtClaimValidator hostJwtClaimValidator) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Required audience is missing", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                audienceValidator,
                hostJwtClaimValidator));
        return decoder;
    }

    @Bean
    HostJwtClaimValidator hostJwtClaimValidator(WambeMetrics metrics) {
        return new HostJwtClaimValidator(metrics);
    }

    @Bean
    JwtDecoder internalJwtDecoder(
            @Value("${wambe.internal-jobs.jwks-uri}") String jwksUri,
            @Value("${wambe.internal-jobs.issuer}") String issuer,
            @Value("${wambe.internal-jobs.audience}") String audience,
            @Value("${wambe.internal-jobs.scheduler-subject:}") String schedulerSubject) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Scheduler audience is invalid", null));
        OAuth2TokenValidator<Jwt> subjectValidator = jwt ->
                !schedulerSubject.isBlank() && schedulerSubject.equals(jwt.getSubject())
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "Scheduler subject is invalid", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                audienceValidator,
                subjectValidator));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(
            @Value("${wambe.internal-jobs.scheduler-subject:}") String schedulerSubject) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            if (!schedulerSubject.isBlank() && schedulerSubject.equals(jwt.getSubject())) {
                return List.of((GrantedAuthority)
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SCHEDULER"));
            }
            Object scope = jwt.getClaims().get("scope");
            if (scope instanceof String value) {
                return Arrays.stream(value.split(" "))
                        .filter(item -> !item.isBlank())
                        .map(item -> (GrantedAuthority)
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_" + item))
                        .toList();
            }
            return List.of();
        });
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${wambe.cors.allowed-origins}") List<String> allowedOrigins) {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Idempotency-Key",
                "If-Match",
                "X-Request-Id"));
        configuration.setExposedHeaders(List.of("X-Request-Id", "ETag"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", configuration);
        return source;
    }

    @Bean
    FilterRegistrationBean<ScannerHmacFilter> disableScannerFilterRegistration(ScannerHmacFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<RequestEnvelopeFilter> disableEnvelopeFilterRegistration(RequestEnvelopeFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<InternalJobKeyFilter> disableJobFilterRegistration(InternalJobKeyFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
