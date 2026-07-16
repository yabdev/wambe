package com.wambe.scanner;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScanController {

    private final ScanService scans;

    public ScanController(ScanService scans) {
        this.scans = scans;
    }

    @PostMapping("/scan")
    ResponseEntity<Void> scan(@Valid @RequestBody ScanRequest request) {
        scans.scan(request);
        return ResponseEntity.accepted().build();
    }

    @ExceptionHandler(ScannerDestinationRejectedException.class)
    ResponseEntity<Void> rejectedDestination() {
        return ResponseEntity.unprocessableEntity().build();
    }
}
