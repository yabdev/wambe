package com.wambe.api.common.security;

import com.wambe.api.common.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentOwner {

    public UUID requireId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
        }
        String subject = authentication instanceof JwtAuthenticationToken jwt
                ? jwt.getToken().getSubject()
                : authentication.getName();
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_SUBJECT", "Authentication is invalid");
        }
    }
}
