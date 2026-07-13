package com.wambe.api.scanner;

import com.wambe.api.common.security.ScannerHmacFilter;
import com.wambe.api.generated.api.InternalApi;
import com.wambe.api.generated.model.InternalJobResult;
import com.wambe.api.generated.model.ScannerCallbackRequest;
import com.wambe.api.retention.InternalJobService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalController implements InternalApi {

    private final ScannerCallbackService callbacks;
    private final InternalJobService jobs;
    private final HttpServletRequest request;

    public InternalController(
            ScannerCallbackService callbacks,
            InternalJobService jobs,
            HttpServletRequest request) {
        this.callbacks = callbacks;
        this.jobs = jobs;
        this.request = request;
    }

    @Override
    public ResponseEntity<Void> _acceptScannerCallback(
            OffsetDateTime xWambeTimestamp,
            UUID xWambeNonce,
            ScannerCallbackRequest scannerCallbackRequest) {
        callbacks.accept(
                xWambeTimestamp,
                xWambeNonce,
                String.valueOf(request.getAttribute(ScannerHmacFilter.BODY_DIGEST_ATTRIBUTE)),
                scannerCallbackRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<InternalJobResult> _dispatchDueScanJobs() {
        return ResponseEntity.accepted().body(jobs.dispatchDueScans());
    }

    @Override
    public ResponseEntity<InternalJobResult> _runRetentionJobs() {
        return ResponseEntity.accepted().body(jobs.runRetention());
    }
}
