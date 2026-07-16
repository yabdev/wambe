package com.wambe.api.integration.scanner;

import java.util.Optional;

public interface ScannerIdentityTokenProvider {

    Optional<String> token();
}
