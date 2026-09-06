# MVP launch runbook

Sequenced path from the verified local baseline to a working staging MVP and then a
production pilot. Written 2026-08-04, updated 2026-09-06. Companion to
`staging-release-checklist.md`, which remains the authoritative control list; this
runbook orders the founder work needed before those controls can be executed.

## Verified baseline (2026-08-04)

All checks were re-run locally on `main` at `0873a08`:

- Web: `npm run lint`, `npm run typecheck`, 75/75 unit tests, production build, and
  30/30 Playwright e2e tests (desktop and mobile Chromium) — all passing.
- `wambe-api`: `mvnw clean verify` (Testcontainers/PostgreSQL 16) — passing.
- `media-scanner`: `mvnw clean verify` — passing.
- `jackson-databind` is pinned at 2.21.5 (SEC-011 remediation).
- US-005 frontend polish is committed (`81acffd`); `implementation_frontend` is
  APPROVED and `qa` is READY in its `STATUS.yaml`.

Only documentation, CI wiring and provisioning scripts changed after `0873a08`; no
application code moved, so that baseline still describes the release candidate.

## Status (2026-09-06)

Done:

- `gh` is authenticated as `yabdev`; `yabdev/wambe` exists and `main` is pushed.
  Local and `origin/main` are in sync. The repository has been public since
  2026-09-06 by Product Owner decision, so the founder product paper PDF and all
  SDLC artifacts are public; keep anything confidential out of the repository.
- The `staging` GitHub environment exists (variables are populated by
  `infra/gcp/bootstrap-project.sh`).
- CI validates the v1.1 staging-evidence example against its schema, closing the
  US-004 "explicit AJV workflow step" blocker.
- Provisioning is scripted so the founder work is mostly account creation:
  `infra/supabase/README.md` + `01-app-roles.sql`, `infra/gcp/bootstrap-project.sh`,
  `infra/gcp/deploy-cloud-run.sh`.

Blocked on founder action, in order:

1. **The `yabdev` GitHub account is locked for billing, so no Actions job can
   start.** While the repository was private, every run since 2026-09-03 ended in
   `startup_failure` with zero jobs. The Product Owner made the repository public
   on 2026-09-06; jobs are now created but each fails with the annotation "The job
   was not started because your account is locked due to a billing issue"
   (`Security` run `34050843613`). Public visibility does not lift an account
   lock. Fix: https://github.com/settings/billing, settle the outstanding balance
   or update the payment method, then run
   `gh workflow run ci.yml --repo yabdev/wambe --ref main`. Fallback, a Product
   Owner decision because `yabdev/wambe` is the canonical home hard-coded in the
   evidence checker, the v1.1 example and the WIF attribute condition: transfer the
   repository to an unlocked account or organisation and update those references.
   Until one of these is done, `BUILD-001`, `BUILD-002` and `release-images.yml`
   are impossible.
2. `NVD_API_KEY` is not set (`gh secret list` is empty); the `Security` workflow's
   Java job fails by design without it.
3. No Supabase, Google Cloud or Vercel project exists. `gcloud`, `vercel` and
   `supabase` CLIs are not installed on the founder machine; Google Cloud Shell has
   `gcloud` and `gh` preinstalled and runs the scripts as-is.

## Phase 1 — GitHub release baseline

1. Clear the account billing lock above, then run CI on the release SHA:
   `gh workflow run ci.yml --repo yabdev/wambe --ref main` (`CI` has a
   `workflow_dispatch` trigger so a SHA can be re-verified without a new push).
   Confirm it is green (`BUILD-001`).
2. Request a free NVD API key (https://nvd.nist.gov/developers/request-an-api-key)
   and set it: `gh secret set NVD_API_KEY --repo yabdev/wambe`.
3. Manually dispatch `Security` on the same SHA and confirm it is green
   (`BUILD-002`): `gh workflow run security.yml --repo yabdev/wambe --ref main`.
4. Optional but recommended: enable required reviewers on the `staging`
   environment so image publication needs an explicit approval.

## Phase 2 — Supabase staging project (~45 minutes, free tier)

Follow `infra/supabase/README.md`. It covers the asymmetric JWT signing key the API
requires, Google/email auth, exact redirect URLs, the `wambe_api` login role, the two
private buckets, the pooler connection values for Secret Manager, and the free-tier
limits that need Product Owner decisions (no PITR, project pausing, SMTP limits).

## Phase 3 — Google Cloud: API + scanner on Cloud Run (~1 hour)

1. Create a GCP project for staging and link billing (free credits are fine; scale-to-
   zero Cloud Run and one Artifact Registry repository cost cents at pilot volume).
2. In Cloud Shell (or a shell with `gcloud` and `gh`):

   ```bash
   git clone https://github.com/yabdev/wambe.git && cd wambe
   PROJECT_ID=<staging-project-id> REGION=europe-west1 bash infra/gcp/bootstrap-project.sh
   ```

   It enables APIs, creates the Artifact Registry repo, four service accounts,
   Workload Identity Federation limited to `yabdev/wambe`, the Secret Manager secrets
   (the scanner HMAC is generated; Supabase values are prompted), and sets the
   `GCP_*` variables on the GitHub `staging` environment.
3. Dispatch `Release images` on the release SHA
   (`gh workflow run release-images.yml --repo yabdev/wambe --ref main`), then
   download `release-images-staging-<sha>` and read `apiImageDigest` and
   `scannerImageDigest` (`BUILD-003`/`BUILD-004`).
4. Deploy, scanner first, with confirmation at each step:

   ```bash
   PROJECT_ID=<staging-project-id> DEPLOY_ENV=staging \
   API_IMAGE_DIGEST=sha256:<...> SCANNER_IMAGE_DIGEST=sha256:<...> \
   SUPABASE_URL=https://<ref>.supabase.co FRONTEND_ORIGIN=https://<vercel-origin> \
   bash infra/gcp/deploy-cloud-run.sh
   ```

   The script renders the templates outside the repository, refuses tags and
   unrendered placeholders, keeps the scanner internal with the API service account
   as the only invoker (`MEDIA-006`), leaves `INTERNAL_JOB_KEY` empty
   (`SECURITY-003`), sets CORS to the exact Vercel origin, waits for API health, and
   creates the two Cloud Scheduler jobs with the dedicated OIDC service account.
   The Vercel origin must be known first; create the Vercel project (Phase 4) before
   this step or redeploy the API once the origin is final.

## Phase 4 — Vercel frontend (~30 minutes)

1. Import the GitHub repo; root directory `apps/web`.
2. Environment: `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`,
   `NEXT_PUBLIC_WAMBE_API_URL` (the Cloud Run API origin + `/api/v1`),
   `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY`, `NEXT_PUBLIC_SITE_URL` (the exact Vercel
   origin), and `NEXT_PUBLIC_DEMO_MODE=false`. The build fails on purpose if demo
   mode is on or the site URL is not an exact https origin.
3. Create a Google Maps API key in the staging GCP project restricted to that origin
   and to the Maps JavaScript API and Geocoding API (the venue picker uses the
   geocoding library) (`SHARE-001`).
4. Vercel Hobby prohibits commercial use; budget for Vercel Pro before charging
   customers. Staging validation on Hobby is a Product Owner judgment call.
5. Update Supabase auth redirect URLs and API CORS with the final origin.

## Phase 5 — Evidence and release gates

1. Execute `staging-release-checklist.md` end to end; record results in a
   staging-evidence JSON conforming to
   `docs/sdlc/stories/US-004-complete-staging-release-controls/contracts/staging-evidence-v1.1.schema.json`
   and check it with `node scripts/check-staging-evidence-template.mjs --final <file>`.
2. US-004: with the exact-SHA release, green CI/Security, canonical images, and
   activated staging, invoke `/persona-qa US-004`. PO-waive (with recorded
   reasons) any control that is out of MVP scope rather than letting it stall.
3. US-005: `qa` is READY. US5-AC-009 needs five host and five guest opt-in
   usability sessions, or an explicit PO limitation decision.
4. Pilot: one real event, one real host, a handful of guests, before wider release.

## Phase 6 — Production promotion

Production is a separate Product Owner decision after staging evidence exists. It
reuses the shape, never the resources:

1. New Supabase project (Pro if PITR is required), new GCP project, Vercel
   production environment. Nothing is shared with staging.
2. `bootstrap-project.sh` with the production `PROJECT_ID` (set
   `SKIP_SECRET_PROMPTS=1` if secrets are added separately; `GITHUB_ENVIRONMENT` can
   stay `staging` because `release-images.yml` has no production option by design).
3. `deploy-cloud-run.sh` with `DEPLOY_ENV=production` and the **same digests** that
   passed staging. `BUILD-005` requires a `production` GitHub environment with
   required reviewers even though no workflow deploys through it yet.
4. Pilot access is controlled by who is invited; the `host_creation_enabled` and
   `protected_visibility_share_enabled` flags are not implemented.

## Open decisions for the Product Owner

- Vercel Hobby vs Pro for staging/production.
- Which of the 42 staging controls to waive for MVP (likely candidates on free tiers:
  `RECOVERY-007` Supabase restore drill, `OPS-004` provider quota alerts, `OPS-007`
  backup on-call contact).
- US5-AC-009 usability-session threshold: run sessions or record a limitation.
- OPS-007: name a backup staging/on-call contact outside the repository.
