# Wambe operations handbook

This handbook is the operational source of truth for the US-002 founder MVP. It does
not authorize deployment. Production changes require Product Owner approval and the
target environment's change controls.

## Release posture

**Current posture: not ready for production.** Local builds and tests pass, and
credential-free CI/deployment templates now exist. Production remains blocked by:

1. SEC-001: unbounded request buffering before scanner HMAC authentication.
2. SEC-002: deployed profiles do not fail startup on the known default scanner secret.
3. Real staging evidence for Supabase/Resend auth, storage, live ClamAV, Google Maps,
   Scheduler OIDC, retention, external previews, telemetry and rollback.
4. A successful CI security workflow with `NVD_API_KEY` and full-history Gitleaks.

Operations controls such as internal scanner ingress, concurrency limits and mandatory
Secret Manager injection reduce risk but do not close SEC-001 or SEC-002.

## Ownership and environments

The Product Owner is release authority. During the founder MVP, the founder is primary
on-call and owns incident command, provider access and customer communication. A backup
operator must be named before production; no single unrecorded personal account should
be the only recovery path.

| Environment | Data | Deployment | Purpose |
|---|---|---|---|
| Local | Synthetic only | Docker Compose and local processes | Development and deterministic automation |
| Staging | Synthetic/test identities only | Separate Vercel, GCP and Supabase projects | Integration, security and rollback evidence |
| Production | Real host content | Separate Vercel, GCP and Supabase projects | Approved pilot traffic |

Never share databases, storage buckets, OAuth credentials, signing secrets or service
accounts between staging and production.

## Delivery gates

The required GitHub `CI` workflow verifies contracts, both Java services, frontend
static/unit/browser/accessibility checks, and both container builds. The scheduled
`Security` workflow scans Git history, npm dependency graphs and Java dependencies.
Configure `NVD_API_KEY` as a repository secret before treating Java scanning as evidence.

Local equivalents:

```powershell
.\scripts\verify.ps1
.\scripts\verify.ps1 -IncludeBrowser -IncludeImages
.\scripts\check-deploy-env.ps1 -Environment staging
```

A release uses immutable image digests. Never rebuild between staging and production,
never deploy `latest`, and retain the previous healthy Cloud Run revision and Vercel
deployment for rollback. GitHub environment protection should require Product Owner
approval for production and should hold cloud credentials only through short-lived
workload identity, not static JSON keys.

## Required deployed configuration

The pre-deploy script checks presence and obvious unsafe values. Secret values belong in
Secret Manager or the provider's encrypted environment settings and must never appear in
logs, workflow output, screenshots or evidence files.

| Component | Required controls |
|---|---|
| API | `SPRING_PROFILES_ACTIVE=staging` or `production`; Supabase storage; exact HTTPS CORS origin; separate Flyway/application DB credentials; rotated scanner secret; Scheduler issuer/audience/subject; empty `INTERNAL_JOB_KEY` |
| Scanner | Same rotated scanner secret; internal ingress; concurrency one; bounded max scale; no public invoker |
| Web | Exact API/site URLs; real Supabase and restricted Maps keys; `NEXT_PUBLIC_DEMO_MODE` absent/false |
| Scheduler | Dedicated service account, exact API audience, invoker permission only |
| Telemetry | TLS, private admin access, encrypted volumes, no event/venue/media payloads in logs |

Application defaults are development conveniences, not deployment values. Until
SEC-002 adds runtime fail-fast validation, the deploy pipeline and post-deploy smoke test
must reject the known local secret.

## Deployment sequence

1. Provision isolated staging Supabase Auth/PostgreSQL/private buckets and Resend SMTP.
2. Run Flyway with the migration principal, then run API verification against staging.
3. Deploy the scanner from an immutable digest with internal ingress and a rotated secret.
4. Execute clean-image and EICAR scan paths before enabling media upload.
5. Deploy the API with zero host traffic, verify health, JWT/CORS/RLS, Scheduler OIDC,
   public metadata redaction and signed storage.
6. Deploy the web preview with demo mode off; run desktop/mobile staging smoke tests.
7. Deploy telemetry and verify application behavior while telemetry is unreachable.
8. Complete `staging-release-checklist.md`, perform a rollback drill and obtain release
   approval.
9. Promote the same digests/configuration shape to production, first to internal/pilot
   users. Stop expansion if any critical alert fires.

The architecture names `host_creation_enabled` and
`protected_visibility_share_enabled`, but those runtime flags are not implemented.
Until they exist, pilot access must be controlled through deployment/access policy; do
not claim application-level gradual rollout.

## Database migration and rollback

Flyway is the sole schema owner and Hibernate remains `ddl-auto=validate`. Migrations are
forward-only:

- take a provider backup/PITR checkpoint before production migration;
- apply to staging first and record `flyway info`;
- use expand/migrate/contract changes so the previous API remains compatible;
- never edit an applied migration or run an ad-hoc destructive production statement;
- repair failure with a reviewed forward migration.

Rollback order:

1. Stop new exposure and, if needed, disable the affected Vercel deployment.
2. Shift API or scanner traffic independently to the prior healthy Cloud Run revision.
3. Roll back Vercel to the previous immutable deployment.
4. Do not reverse Flyway history. If the previous API is incompatible, roll forward with
   the prepared compatibility migration.
5. Reconcile media left in `scanning` by rerunning scan dispatch after scanner recovery.
6. Verify health, publish, public metadata and one clean upload after rollback.

Rollback triggers include sustained publication failures, authorization isolation
failure, malware promotion failure, migration incompatibility, or uncontrolled resource
growth. A security isolation or malware-control failure stops the release immediately.

## Reliability objectives

These are pilot objectives, not contractual promises:

| SLI | Initial objective | Alert |
|---|---|---|
| API availability | 99.5% per calendar month | health absent for 3 minutes |
| Publish server success | at least 95% over a rolling hour | server error rate above 5% for 10 minutes |
| Auth callback success | at least 95% over a rolling hour | failures above 5% for 10 minutes |
| Scan queue age | p95 below 10 minutes | oldest due job above 10 minutes |
| Daily retention | every scheduled run succeeds | any failed/missed daily run |
| API latency | observe p50/p95/p99 by route | p95 above 2 seconds for 10 minutes |

`infra/observability/prometheus/wambe-alerts.yml` implements only metrics already
available through HTTP/Actuator. Scan queue age, retention outcome, auth callback and
telemetry-drop metrics are not yet exported; their alerts remain blocked application
instrumentation work and must not be represented as active.

Dashboard minimums are RED (rate, errors, duration), Cloud Run instance/concurrency/
memory, PostgreSQL pool/quota, storage use, scan job age/result and retention outcome.
Logs and traces use random request IDs and must exclude bearer tokens, signed URLs,
addresses, filenames, event titles and request bodies.

## Capacity and cost controls

- Start API and scanner at zero minimum instances; set one minimum API instance only if
  staging cold-start evidence violates the user journey.
- Scanner concurrency remains one because ClamAV and file buffers are memory-heavy.
- Bound maximum instances and monitor rejected/queued work before increasing limits.
- Alert on Cloud Run, database, storage, egress and telemetry VPS quota/budget thresholds
  configured in the provider accounts.
- Retain raw logs/traces for seven days and aggregates for thirty days as approved.
- Reassess the self-hosted telemetry VPS if maintenance cost distracts from the MVP;
  telemetry must fail open and never block event creation.

No monetary estimate is recorded because project region, traffic and provider plan are
not yet known.

## Backup, restore and disaster recovery

| Asset | Backup/restore approach | Recovery target assumption |
|---|---|---|
| Supabase PostgreSQL | Provider backup/PITR if enabled; quarterly staging restore drill | RPO depends on purchased plan; restore within the same working day |
| Private storage | Provider durability plus lifecycle/retention; verify object deletion | No restoration of host-deleted media to active service |
| Cloud Run/Vercel | Stateless immutable revisions and configuration | Prior revision within 30 minutes |
| Telemetry VPS | Encrypted volume backup before upgrades and daily provider snapshot | Best effort; product remains available without telemetry |
| GitHub/configuration | Protected repository, reviewed IaC/templates and provider export | Recreate services from repository plus Secret Manager |

Before production, record the actual provider backup plan and measured restore duration.
A restore drill uses staging/synthetic data. Restored deleted content must not reappear
on active public or host surfaces; run retention immediately after any point-in-time
restore and reconcile against the deletion ledger/audit events.

## Incident runbooks

### API unavailable or latency

1. Confirm Cloud Run revision health, instance/memory/concurrency and Supabase status.
2. Correlate by request ID; do not request tokens or sensitive payloads from users.
3. If the new revision is implicated, shift traffic to the previous revision.
4. Verify health, login, draft save and lean publish before resolving.

### Publication errors above five percent

1. Separate validation `4xx` from server/storage/database `5xx`.
2. Check DB pool saturation, Flyway state, idempotency conflicts and storage dependencies.
3. Stop rollout and roll back API if the increase follows deployment.
4. Preserve request IDs and sanitized error codes for follow-up.

### Scanner backlog, rejection anomaly or memory pressure

1. Disable new media upload exposure while keeping lean publication available.
2. Inspect scanner memory/restarts, ClamAV definition age and oldest due scan job.
3. Treat unexpected clean results or default-secret acceptance as a security incident.
4. Roll back scanner, rotate HMAC if compromise is suspected, then rerun dispatch.
5. Never manually promote quarantine objects.

### Authentication callback failure

1. Check Supabase status, Google OAuth redirect allowlist, Resend and exact Vercel URL.
2. Verify API JWT issuer/audience/role and CORS using a staging account.
3. Do not weaken redirect, JWT or email-verification controls to restore service.

### Retention failure

1. Confirm Scheduler OIDC subject/audience and correlated API response.
2. Rerun once against staging or after confirming production job idempotency.
3. Compare database rows and storage paths; alert if objects remain after database purge.
4. Do not perform unreviewed bulk deletion.

## Support and post-incident process

Support triage records environment, time, route, safe error code, request ID, browser and
whether media was used. It never requests passwords, access tokens, signed URLs or
uploaded files over chat/email. Security/privacy reports receive immediate founder
escalation.

For Severity 1 (data isolation, malware activation, credential compromise, destructive
retention), halt rollout and notify the Product Owner immediately. For Severity 2
(sustained publish/auth/scan outage), begin rollback while investigating. Lower-severity
issues enter the product backlog.

Complete a blameless incident review within two working days for Severity 1/2 events:
timeline, impact, detection, contributing controls, corrective owner/date and evidence
that the fix was retested.
