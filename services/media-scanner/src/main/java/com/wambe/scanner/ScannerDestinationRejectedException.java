package com.wambe.scanner;

public class ScannerDestinationRejectedException extends RuntimeException {

    private final String reason;

    public ScannerDestinationRejectedException(String reason) {
        super("Scanner destination rejected");
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
