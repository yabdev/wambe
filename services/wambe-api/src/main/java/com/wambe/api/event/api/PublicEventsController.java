package com.wambe.api.event.api;

import com.wambe.api.event.application.PublicMetadataService;
import com.wambe.api.generated.api.PublicApi;
import com.wambe.api.generated.model.PublicEventMetadata;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicEventsController implements PublicApi {

    private final PublicMetadataService metadata;

    public PublicEventsController(PublicMetadataService metadata) {
        this.metadata = metadata;
    }

    @Override
    public ResponseEntity<PublicEventMetadata> _getPublicEventMetadata(String slug) {
        return ResponseEntity.ok(metadata.get(slug));
    }
}
