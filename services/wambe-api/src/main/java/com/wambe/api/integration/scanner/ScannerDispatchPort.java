package com.wambe.api.integration.scanner;

import com.wambe.api.media.persistence.MediaEntity;
import java.util.UUID;

public interface ScannerDispatchPort {

    void dispatch(UUID jobId, MediaEntity media);
}
