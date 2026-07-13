package com.wambe.api.integration.supabase;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface IdentityAdminPort {

    LinkedIdentity verifyLinkedIdentity(UUID userId, String provider);

    record LinkedIdentity(String providerSubject, OffsetDateTime verifiedAt) {
    }
}
