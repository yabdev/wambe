package com.wambe.api.integration.supabase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wambe.api.common.error.ApiException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class SupabaseIdentityAdminAdapter implements IdentityAdminPort {

    private final RestClient client;

    public SupabaseIdentityAdminAdapter(
            RestClient.Builder builder,
            @Value("${wambe.supabase.url}") String baseUrl,
            @Value("${wambe.supabase.service-role-key}") String serviceRoleKey) {
        this.client = builder
                .baseUrl(baseUrl)
                .defaultHeader("apikey", serviceRoleKey)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                .build();
    }

    @Override
    public LinkedIdentity verifyLinkedIdentity(UUID userId, String provider) {
        try {
            AdminUser user = client.get()
                    .uri("/auth/v1/admin/users/{id}", userId)
                    .retrieve()
                    .body(AdminUser.class);
            if (user == null || user.emailConfirmedAt() == null) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "EMAIL_NOT_VERIFIED",
                        "Verify the email address before linking sign-in methods");
            }
            Identity identity = user.identities() == null
                    ? null
                    : user.identities().stream()
                            .filter(item -> provider.equals(item.provider()))
                            .findFirst()
                            .orElse(null);
            if (identity == null || identity.identityId() == null) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "IDENTITY_NOT_LINKED",
                        "Complete provider linking before confirming it");
            }
            return new LinkedIdentity(identity.identityId(), user.emailConfirmedAt());
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "IDENTITY_PROVIDER_UNAVAILABLE",
                    "Identity verification is temporarily unavailable");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AdminUser(
            @JsonProperty("email_confirmed_at") OffsetDateTime emailConfirmedAt,
            List<Identity> identities) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Identity(
            String provider,
            @JsonProperty("identity_id") String identityId) {
    }
}
