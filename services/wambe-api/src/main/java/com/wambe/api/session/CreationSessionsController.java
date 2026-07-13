package com.wambe.api.session;

import com.wambe.api.common.security.CurrentOwner;
import com.wambe.api.generated.api.CreationSessionsApi;
import com.wambe.api.generated.model.WambeProductEventV1;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CreationSessionsController implements CreationSessionsApi {

    private final CurrentOwner currentOwner;
    private final CreationMilestoneService milestones;

    public CreationSessionsController(CurrentOwner currentOwner, CreationMilestoneService milestones) {
        this.currentOwner = currentOwner;
        this.milestones = milestones;
    }

    @Override
    public ResponseEntity<Void> _recordCreationMilestone(
            UUID sessionId,
            UUID idempotencyKey,
            WambeProductEventV1 wambeProductEventV1) {
        milestones.record(
                currentOwner.requireId(),
                sessionId,
                idempotencyKey,
                wambeProductEventV1);
        return ResponseEntity.accepted().build();
    }
}
