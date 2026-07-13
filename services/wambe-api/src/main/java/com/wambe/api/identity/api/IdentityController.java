package com.wambe.api.identity.api;

import com.wambe.api.auth.IdentityLinkService;
import com.wambe.api.common.security.CurrentOwner;
import com.wambe.api.generated.api.AuthApi;
import com.wambe.api.generated.model.LinkIdentityRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IdentityController implements AuthApi {

    private final CurrentOwner currentOwner;
    private final IdentityLinkService identityLinkService;

    public IdentityController(CurrentOwner currentOwner, IdentityLinkService identityLinkService) {
        this.currentOwner = currentOwner;
        this.identityLinkService = identityLinkService;
    }

    @Override
    public ResponseEntity<Void> _linkIdentity(
            UUID idempotencyKey,
            LinkIdentityRequest linkIdentityRequest) {
        identityLinkService.confirm(
                currentOwner.requireId(),
                idempotencyKey,
                linkIdentityRequest.getProvider().getValue());
        return ResponseEntity.noContent().build();
    }
}
