# Wambe media scanner

Java 21 / Spring Boot Cloud Run worker for quarantine-file validation. Dispatch requests
and callbacks are HMAC-SHA256 signed over:

```text
timestamp + "\n" + nonce + "\n" + sha256(rawBody)
```

The service enforces a 10 MB download limit, detects file type from magic bytes, compares
it with the claimed type, streams content to `clamd` using the INSTREAM protocol, writes
an image preview object, and sends a single-use callback to the API. PDF preview rendering
is intentionally deferred; clean PDFs remain usable as labelled PDF cards.

Build and test:

```powershell
.\mvnw.cmd clean verify
docker build -f services/media-scanner/Dockerfile -t media-scanner:local .
```

The container starts ClamAV, refreshes signatures, waits for `clamd` readiness, then
starts the non-root Java process. Cloud Run should use a 60-second request timeout or
higher, keep concurrency low, and source `SCANNER_HMAC_SECRET` from Secret Manager.
