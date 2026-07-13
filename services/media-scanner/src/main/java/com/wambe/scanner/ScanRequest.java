package com.wambe.scanner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.UUID;

public record ScanRequest(
        @NotNull UUID jobId,
        @NotNull UUID mediaId,
        @NotNull URI readUrl,
        @NotNull URI previewWriteUrl,
        @NotBlank String previewQuarantinePath,
        @NotNull URI callbackUrl,
        @NotBlank String claimedMimeType) {
}
