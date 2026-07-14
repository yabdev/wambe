# US-002 staging release checklist

Record only redacted command output, workflow URLs, revision/digest IDs and test results.
Never paste secrets, bearer tokens, signed URLs or personal event content.

## Build and supply chain

- [ ] Required `CI` workflow is green for the release commit.
- [ ] Scheduled/manual `Security` workflow is green with `NVD_API_KEY` configured.
- [ ] API and scanner image digests match the tested artifacts; neither uses `latest`.
- [ ] SBOM/provenance is retained for each image, or the absence is accepted as a release
      blocker.
- [ ] Production promotion approval is enabled in the GitHub environment.

## Security blockers

- [ ] SEC-001: oversized unsigned scanner/API callback requests receive early `413`
      without proportional memory growth; scanner ingress is internal.
- [ ] SEC-002: API and scanner fail startup when the HMAC secret is absent, short or the
      known local default; rotated Secret Manager value works.
- [ ] `INTERNAL_JOB_KEY` is empty and rejected.
- [ ] Host JWT fixtures/real tokens prove only `role=authenticated` reaches host routes.
- [ ] OAuth callback rejects backslash/protocol-relative external redirect targets.

## Authentication and authorization

- [ ] Google sign-in succeeds from the exact staging Vercel origin.
- [ ] Verified email/password registration, sign-in and reset delivery succeed.
- [ ] Session refresh succeeds and expired/invalid JWTs fail closed.
- [ ] Linked identity requires verified matching email and recent authentication.
- [ ] Cross-owner API and pooled-connection RLS tests pass against staging.
- [ ] CORS allows only the exact staging origin.

## Storage and malware

- [ ] Browser uploads directly to the quarantine bucket using a short-lived signed URL.
- [ ] A clean JPG/PNG/WebP and PDF complete scanning and use only active previews.
- [ ] EICAR is rejected; quarantine is removed and the draft remains available.
- [ ] Wrong-size, unsupported-type, replayed and wrong-preview-path requests fail.
- [ ] Scanner accepts only the rotated HMAC and allowed storage/API destinations.
- [ ] Deleted media and aged draft objects are removed from Supabase storage.

## Maps, public metadata and sharing

- [ ] Google Maps key is restricted to exact staging/production origins and required APIs.
- [ ] Address search, pin confirmation and failure recovery work on desktop and mobile.
- [ ] Public/private-link pages render expected OG metadata externally.
- [ ] Invite-only, hidden-location, unpublished and deleted events expose no protected
      metadata beyond the approved response semantics.
- [ ] WhatsApp/social crawler unfurl is checked on the public staging domain.

## Jobs, migrations and recovery

- [ ] Flyway reports expected versions with no pending unexpected migration.
- [ ] Scan-dispatch Scheduler job succeeds using exact OIDC subject/audience.
- [ ] Retention job succeeds on synthetic aged data and reports database/object outcomes.
- [ ] API and scanner independently roll back to prior Cloud Run revisions.
- [ ] Vercel rolls back to the prior deployment.
- [ ] No down migration is required and the prior API remains schema-compatible.
- [ ] A Supabase staging restore drill records the actual RPO/RTO and reruns retention.

## Observability and support

- [ ] Health, request rate, errors and latency are visible without sensitive labels.
- [ ] Publish-error and API-unavailable alerts route to the founder on-call.
- [ ] Scan age/result, retention, auth callback and quota alerts are implemented and tested.
- [ ] Telemetry outage does not prevent draft save or publication.
- [ ] Log review confirms no tokens, signed URLs, titles, addresses, filenames or bodies.
- [ ] Founder primary and backup on-call contacts are recorded outside the public repo.

## Release decision

- Release commit:
- API image digest:
- Scanner image digest:
- Vercel deployment:
- Staging evidence location:
- Open risks/waivers:
- Product Owner decision and date:
