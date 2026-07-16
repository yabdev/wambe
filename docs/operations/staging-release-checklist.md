# US-003 staging release checklist

Record only redacted command output, workflow URLs, revision/digest IDs and test results.
Never paste secrets, bearer tokens, signed URLs or personal event content.

## Build and supply chain

- [ ] `BUILD-001` — Required `CI` workflow is green for the release commit.
- [ ] `BUILD-002` — Scheduled/manual `Security` workflow is green with `NVD_API_KEY`
      configured.
- [ ] `BUILD-003` — API and scanner digests recorded by the release workflow match the
      images deployed and tested in staging; neither uses `latest`, and promotion does
      not rebuild them.
- [ ] `BUILD-004` — SBOM/provenance is retained for each image, or the absence is
      accepted as a release
      blocker.
- [ ] `BUILD-005` — Production promotion approval is enabled in the GitHub environment.

## Security blockers

- [ ] `SECURITY-001` — SEC-001: oversized unsigned scanner/API callback requests receive early `413`
      without proportional memory growth; scanner ingress is internal.
- [ ] `SECURITY-002` — SEC-002: API and scanner fail startup when the HMAC secret is
      absent, short or the
      known local default; rotated Secret Manager value works.
- [ ] `SECURITY-003` — `INTERNAL_JOB_KEY` is empty and rejected.
- [ ] `SECURITY-004` — Host JWT fixtures/real tokens prove only `role=authenticated`
      reaches host routes.
- [ ] `SECURITY-005` — OAuth callback rejects backslash/protocol-relative external
      redirect targets.

## Authentication and authorization

- [ ] `AUTH-001` — Google sign-in succeeds from the exact staging Vercel origin.
- [ ] `AUTH-002` — Verified email/password registration, sign-in and reset delivery
      succeed.
- [ ] `AUTH-003` — Session refresh succeeds and expired/invalid JWTs fail closed.
- [ ] `AUTH-004` — Linked identity requires verified matching email and recent
      authentication.
- [ ] `AUTH-005` — Cross-owner API and pooled-connection RLS tests pass against staging.
- [ ] `AUTH-006` — CORS allows only the exact staging origin.

## Storage and malware

- [ ] `MEDIA-001` — Browser uploads directly to the quarantine bucket using a short-lived
      signed URL.
- [ ] `MEDIA-002` — A clean JPG/PNG/WebP and PDF complete scanning and use only active
      previews.
- [ ] `MEDIA-003` — EICAR is rejected; quarantine is removed and the draft remains
      available.
- [ ] `MEDIA-004` — Wrong-size, unsupported-type, replayed and wrong-preview-path
      requests fail.
- [ ] `MEDIA-005` — Scanner accepts only the rotated HMAC and allowed storage/API
      destinations.
- [ ] `MEDIA-006` — Scanner ingress is internal, has no public invoker, and accepts API
      dispatch only
      with the API service account's exact-audience Google ID token plus valid HMAC.
- [ ] `MEDIA-007` — Deleted media and aged draft objects are removed from Supabase
      storage.

## Maps, public metadata and sharing

- [ ] `SHARE-001` — Google Maps key is restricted to exact staging/production origins and
      required APIs.
- [ ] `SHARE-002` — Address search, pin confirmation and failure recovery work on desktop
      and mobile.
- [ ] `SHARE-003` — Public/private-link pages render expected OG metadata externally.
- [ ] `SHARE-004` — Invite-only, hidden-location, unpublished and deleted events expose no protected
      metadata beyond the approved response semantics.
- [ ] `SHARE-005` — WhatsApp/social crawler unfurl is checked on the public staging
      domain.

## Jobs, migrations and recovery

- [ ] `RECOVERY-001` — Flyway reports expected versions with no pending unexpected
      migration.
- [ ] `RECOVERY-002` — Scan-dispatch Scheduler job succeeds using exact OIDC
      subject/audience.
- [ ] `RECOVERY-003` — Retention job succeeds on synthetic aged data and reports
      database/object outcomes.
- [ ] `RECOVERY-004` — API and scanner independently roll back to prior Cloud Run
      revisions.
- [ ] `RECOVERY-005` — Vercel rolls back to the prior deployment.
- [ ] `RECOVERY-006` — No down migration is required and the prior API remains
      schema-compatible.
- [ ] `RECOVERY-007` — A Supabase staging restore drill records the actual RPO/RTO and
      reruns retention.

## Observability and support

- [ ] `OPS-001` — Health, request rate, errors and latency are visible without sensitive
      labels.
- [ ] `OPS-002` — Publish-error and API-unavailable alerts route to the founder on-call.
- [ ] `OPS-003` — Backend envelope/JWT/destination rejection, scan age/result and retention metrics
      are visible with bounded labels; configured alerts are tested.
- [ ] `OPS-004` — Auth callback and provider quota alerts are implemented and tested.
- [ ] `OPS-005` — Telemetry outage does not prevent draft save or publication.
- [ ] `OPS-006` — Log review confirms no tokens, signed URLs, titles, addresses,
      filenames or bodies.
- [ ] `OPS-007` — Founder primary and backup on-call contacts are recorded outside the
      public repo.

## Release decision

- Release commit:
- API image digest:
- Scanner image digest:
- Vercel deployment:
- Staging evidence location:
- Open risks/waivers:
- Product Owner decision and date:
