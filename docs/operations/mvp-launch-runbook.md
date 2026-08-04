# MVP launch runbook

Sequenced path from the verified local baseline to a working staging MVP.
Written 2026-08-04. Companion to `staging-release-checklist.md`, which remains the
authoritative control list; this runbook orders the founder work needed before those
controls can be executed.

## Verified baseline (2026-08-04)

All checks were re-run locally on `main` at `0873a08`:

- Web: `npm run lint`, `npm run typecheck`, 75/75 unit tests, production build, and
  30/30 Playwright e2e tests (desktop and mobile Chromium) — all passing.
- `wambe-api`: `mvnw clean verify` (Testcontainers/PostgreSQL 16) — passing.
- `media-scanner`: `mvnw clean verify` — passing.
- `jackson-databind` is pinned at 2.21.5 (SEC-011 remediation).
- US-005 frontend polish is committed (`81acffd`); `implementation_frontend` is
  APPROVED and `qa` is READY in its `STATUS.yaml`.

No cloud resource exists yet. Everything below is founder work, roughly in order,
using free tiers wherever possible.

## Phase 1 — GitHub release baseline (~20 minutes)

The canonical home is `yabdev/wambe` (founder decision, 2026-08-04). This machine's
`gh` CLI is currently authenticated as `yabdevTM`, which cannot see that repository —
re-authenticate as `yabdev` first, then create the repo if it does not exist and push:

```powershell
gh auth login            # sign in as yabdev
gh repo create yabdev/wambe --private --description "Wambe event publishing platform"
git push -u origin main  # origin already points at https://github.com/yabdev/wambe.git
```

Also clear the stale credential if plain `git push` still fails after `gh auth login`:
Windows Credential Manager may hold a `yabdevTM` token for `github.com`
(`cmdkey /list | findstr github`, then remove the stale entry).

Then:

1. Request a free NVD API key (https://nvd.nist.gov/developers/request-an-api-key)
   and set it: `gh secret set NVD_API_KEY`.
2. Confirm the `CI` workflow is green on the pushed SHA.
3. Manually dispatch the `Security` workflow and confirm it is green
   (`BUILD-001`/`BUILD-002` evidence).
4. Create a `staging` GitHub environment — `release-images.yml` targets it. Its
   `GCP_*` variables come from Phase 3.

## Phase 2 — Supabase staging project (~45 minutes, free tier)

Create one Supabase project for staging. Collect/configure:

1. **Auth**: enable Google OAuth and email/password; set the site URL and redirect
   URLs to the exact Vercel staging origin (Phase 4) plus `http://localhost:3000`
   for local verification.
2. **Database**: note the pooled `DATABASE_URL`. Create the application login
   (`wambe_api`) and migration principal split — mirror the roles created by
   `infra/local/postgres` init scripts (`DATABASE_USER`/`DATABASE_PASSWORD` vs
   `FLYWAY_USER`/`FLYWAY_PASSWORD`).
3. **Storage**: create the private media bucket used by the API's supabase storage
   adapter (`STORAGE_TYPE=supabase`); never expose the service-role key to the
   browser.
4. **Record**: `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_JWKS_URI`,
   `SUPABASE_ISSUER`, anon key for the frontend.

## Phase 3 — Google Cloud: API + scanner on Cloud Run (~2 hours)

Follow `infra/gcp/cloud-run/README.md`; the full env table is in
`services/wambe-api/README.md`.

1. Create a GCP project; enable Cloud Run, Artifact Registry, Secret Manager, and
   Cloud Scheduler.
2. Set up Workload Identity Federation for GitHub Actions, then populate the
   `staging` environment vars (`GCP_WORKLOAD_IDENTITY_PROVIDER`,
   `GCP_ARTIFACT_PUBLISHER_SERVICE_ACCOUNT`, `GCP_REGION`) and dispatch
   `Release images` to publish digest-pinned images with SBOM/provenance
   (`BUILD-003`/`BUILD-004`).
3. Generate a fresh scanner HMAC secret (≥32 chars, not the local default) in
   Secret Manager (`SECURITY-002`).
4. Render `wambe-api.service.example.yaml` and `media-scanner.service.example.yaml`
   with real values; deploy. Scanner: internal ingress, no public invoker, API
   service account only (`MEDIA-006`). API: public, `CORS_ALLOWED_ORIGINS` set to
   the exact Vercel origin, `INTERNAL_JOB_KEY` empty (`SECURITY-003`).
5. Create the Cloud Scheduler jobs with a dedicated OIDC service account per
   `infra/gcp/scheduler/README.md`.

## Phase 4 — Vercel frontend (~30 minutes)

1. Import the GitHub repo; root directory `apps/web`.
2. Environment: `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`,
   `NEXT_PUBLIC_WAMBE_API_URL` (the Cloud Run API URL + `/api/v1`),
   `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY`, `NEXT_PUBLIC_SITE_URL` (the Vercel origin),
   and `NEXT_PUBLIC_DEMO_MODE=false`.
3. Create a Google Maps JavaScript API key restricted to that origin.
4. Note: the Vercel Hobby tier prohibits commercial use; budget for Vercel Pro
   (or an equivalent host) before charging customers. Staging validation on Hobby
   is a judgment call for the Product Owner.
5. Update Supabase auth redirect URLs and API CORS with the final origin.

## Phase 5 — Evidence and release gates

1. Execute `staging-release-checklist.md` end to end; record results in a
   staging-evidence JSON conforming to
   `docs/sdlc/stories/US-004-complete-staging-release-controls/contracts/staging-evidence-v1.1.schema.json`.
2. US-004: with the exact-SHA release, green CI/Security, canonical images, and
   activated staging, invoke `/persona-qa US-004`. PO-waive (with recorded
   reasons) any control that is out of MVP scope rather than letting it stall.
3. US-005: `qa` is READY. US5-AC-009 needs five host and five guest opt-in
   usability sessions, or an explicit PO limitation decision.
4. Pilot: one real event, one real host, a handful of guests, before wider release.

## Open decisions for the Product Owner

- Vercel Hobby vs Pro for staging/production.
- Which of the 42 staging controls to waive for MVP.
- US5-AC-009 usability-session threshold: run sessions or record a limitation.
- OPS-007: name a backup staging/on-call contact outside the repository.
