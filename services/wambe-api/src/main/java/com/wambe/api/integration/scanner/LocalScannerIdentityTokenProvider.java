package com.wambe.api.integration.scanner;

import java.util.Optional;

public class LocalScannerIdentityTokenProvider implements ScannerIdentityTokenProvider {

    @Override
    public Optional<String> token() {
        return Optional.empty();
    }
}
