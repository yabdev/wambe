# Cloud Run deployment templates

These templates encode the approved US-003 staging topology without real project IDs,
domains, service accounts, image digests, or secret values. Render them outside the
repository and review the resulting diff before any `gcloud run services replace`
command.

## Required substitutions

- `PROJECT_ID`, `REGION`, `API_SERVICE_ACCOUNT`, `SCANNER_SERVICE_ACCOUNT`
- `SCHEDULER_SERVICE_ACCOUNT_UNIQUE_ID`: the Scheduler service account's numeric `uniqueId`
  (`gcloud iam service-accounts describe <email> --format='value(uniqueId)'`). The API
  compares `SCHEDULER_SUBJECT` with the Google ID token `sub` claim, which carries that
  ID; the email only appears in the `email` claim.
- immutable `API_IMAGE_DIGEST` and `SCANNER_IMAGE_DIGEST`
- exact frontend/API URLs and Supabase issuer/JWKS/storage values
- Secret Manager secret names and versions
- the scanner `run.app` URL, used identically as `SCANNER_URL` and
  `SCANNER_AUDIENCE`
- `VPC_NETWORK` and `VPC_SUBNETWORK` for the approved same-project internal route

Never deploy a mutable `latest` tag. Release automation must promote the same tested
image digest from staging to production.

## Network and IAM policy

- `wambe-api` is internet-reachable because browsers call it directly; Spring bearer
  authorization remains authoritative. Grant public invoker only to this service.
- `media-scanner` uses internal ingress, concurrency `1`, and a bounded maximum scale.
  Do not grant `allUsers` invocation. Grant the API service account
  `roles/run.invoker` on this scanner service only.
- The API template shows Direct VPC egress with `all-traffic`, allowing its call to the
  scanner's default `run.app` URL to qualify as internal ingress. Verify region,
  routing, quotas and actual provider cost before applying it. Stop for Product Owner
  approval if the supported route requires a billed connector, load balancer or other
  commitment; never make the scanner public as a workaround.
- Every deployed API dispatch sends a Google ID token for the exact scanner audience in
  `X-Serverless-Authorization` plus the independent body HMAC. Local/test profiles use
  HMAC only.
- Cloud Scheduler receives a dedicated service account with invoker access only to the
  two internal-job endpoints. `INTERNAL_JOB_KEY` must remain unset.
- Runtime service accounts receive only their required Secret Manager, storage, logging,
  and invocation permissions. Migration credentials are supplied only to the API
  revision that performs Flyway startup.

## Pre-deploy gates

1. Run `scripts/check-deploy-env.ps1 -Environment staging`.
2. Run `scripts/verify.ps1 -IncludeBrowser -IncludeImages` and require green CI.
3. Confirm image digests and generated SBOM/provenance in the release workflow.
4. Confirm the scanner HMAC secret is rotated, at least 32 characters, and present in
   both services through Secret Manager.
5. Confirm Security independently retested the SEC-001 envelope, SEC-002 startup,
   SEC-003 JWT and SEC-008 destination/IAM controls. Templates are not closure evidence.
6. Run Flyway against staging, execute the post-release checklist, and record redacted
   evidence before production approval.

Validate rendered YAML in CI and inspect the effective service diff against
`gcloud run services describe` before applying it. `gcloud run services replace` is a
mutating command; do not execute it without Product Owner authorization and environment
change approval.
