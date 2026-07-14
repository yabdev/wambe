# Cloud Run deployment templates

These templates encode the approved US-002 topology without real project IDs, domains,
service accounts, image digests, or secret values. Render them outside the repository
and review the resulting diff before any `gcloud run services replace` command.

## Required substitutions

- `PROJECT_ID`, `REGION`, `API_SERVICE_ACCOUNT`, `SCANNER_SERVICE_ACCOUNT`
- immutable `API_IMAGE_DIGEST` and `SCANNER_IMAGE_DIGEST`
- exact frontend/API URLs and Supabase issuer/JWKS/storage values
- Secret Manager secret names and versions
- the private scanner URL reachable through the selected internal ingress/VPC design

Never deploy a mutable `latest` tag. Release automation must promote the same tested
image digest from staging to production.

## Network and IAM policy

- `wambe-api` is internet-reachable because browsers call it directly; Spring bearer
  authorization remains authoritative. Grant public invoker only to this service.
- `media-scanner` uses internal ingress, concurrency `1`, and a bounded maximum scale as
  compensating controls for SEC-001. Provide an internal load-balancer/VPC path from the
  API before deployment. Do not make the scanner public merely to simplify routing.
- Cloud Scheduler receives a dedicated service account with invoker access only to the
  two internal-job endpoints. `INTERNAL_JOB_KEY` must remain unset.
- Runtime service accounts receive only their required Secret Manager, storage, logging,
  and invocation permissions. Migration credentials are supplied only to the API
  revision that performs Flyway startup.

## Pre-deploy gates

1. Run `scripts/check-deploy-env.ps1`.
2. Run `scripts/verify.ps1 -IncludeBrowser -IncludeImages` and require green CI.
3. Confirm image digests and generated SBOM/provenance in the release workflow.
4. Confirm the scanner HMAC secret is rotated, at least 32 characters, and present in
   both services through Secret Manager.
5. Confirm SEC-001 and SEC-002 application remediations are retested. These templates
   reduce exposure but do not close either code finding.
6. Run Flyway against staging, execute the post-release checklist, and record redacted
   evidence before production approval.

Validate rendered YAML in CI and inspect the effective service diff against
`gcloud run services describe` before applying it. `gcloud run services replace` is a
mutating command; do not execute it without Product Owner authorization and environment
change approval.
