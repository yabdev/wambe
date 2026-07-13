package com.wambe.api.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalJobKeyFilter extends OncePerRequestFilter {

    private final String localKey;

    public InternalJobKeyFilter(@Value("${wambe.internal-jobs.local-key:}") String localKey) {
        this.localKey = localKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/internal/jobs/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String supplied = request.getHeader("X-Wambe-Internal-Job-Key");
        if (!localKey.isBlank() && supplied != null
                && MessageDigest.isEqual(
                        localKey.getBytes(StandardCharsets.UTF_8),
                        supplied.getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(
                            "local-scheduler",
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_JOB"))));
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (supplied != null) {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
