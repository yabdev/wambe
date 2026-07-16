# US-003 — Secure US-002 staging release

## Intake

### Problem

US-002 is functionally complete but cannot be promoted to production. Security review
left two open High findings: SEC-001 permits memory-exhaustion pressure by buffering
scanner/callback bodies before authentication, and SEC-002 lets deployed services fall
back to a known development HMAC secret. The real-provider staging checklist is also
unexecuted, so authentication, signed storage, live malware scanning, Scheduler OIDC,
external previews, observability, recovery and supply-chain controls lack release
evidence.

The current process stops at local/demo verification. Application, QA, security and
operations artifacts exist, but the completed US-002 changes are not yet committed and
there are no isolated staging provider accounts. Therefore CI cannot produce an
immutable release baseline and no real trust boundary has been exercised.

### Target users and stakeholders

- Wambe hosts whose identity, event content and uploaded media require safe isolation.
- The founder/operator responsible for release, incident response and provider access.
- Product Owner, engineering, QA, security and operations reviewers.

The founder is the primary staging operator and on-call owner. A named backup operator
is required before production, but not before staging work begins.

### Desired outcome and success measures

The Product Owner selected the full release-readiness scope. Success means SEC-001 and
SEC-002 are remediated and independently retested, every applicable item in
`docs/operations/staging-release-checklist.md` has redacted evidence or an explicit PO
decision, and staging is ready for a separate production promotion decision. This story
does not authorize or perform production deployment.

**Intake success measures**

1. **Zero open Critical/High findings in scope:** SEC-001 and SEC-002 have code,
   automated negative tests and independent security retest evidence.
2. **100% checklist disposition:** every staging checklist item is `PASS`, a linked
   defect/blocker, or an explicit PO `WAIVE`; unchecked or implied passes are not valid.
3. **Clean supply-chain evidence:** required CI and Security workflows pass for one
   immutable commit; API/scanner image digests and SBOM/provenance are recorded.
4. **No credential/privacy leakage:** secret/history scans pass and evidence contains no
   credentials, tokens, signed URLs or personal event/media data.
5. **Recoverability demonstrated:** API, scanner and Vercel rollback drills pass;
   Supabase restore capability and measured RPO/RTO are evidenced or raised to the PO as
   a paid-plan blocker.

Evidence sources are GitHub Actions, automated test reports, immutable image metadata,
provider configuration exports, request IDs/metrics, synthetic staging test records,
rollback/restore drill logs and the staging checklist. Provider screenshots or logs are
supporting evidence only and must be redacted. Data quality is limited by synthetic
traffic, small sample sizes and provider free-tier behavior; none of this proves
production capacity or the strategic under-180-second host KPI.

### Scope, constraints, dependencies, and priority

- **Priority:** P0 release blocker before any production launch.
- **Scope:** both High security fixes plus the complete US-002 staging checklist.
- **Environment authority:** configure and test staging only; no production changes.
- **Binding constraints:** retain the approved Next.js, Spring Boot, Cloud Run, Supabase,
  Flyway, Google Maps and telemetry architecture and the versioned US-002 contracts.
- Use synthetic staging identities/content; never copy production data into staging.
- Never commit credentials, tokens, signed URLs or provider secrets; evidence must be
  redacted.
- Depends on a committed US-002 baseline and access to isolated staging provider
  projects/accounts.
- No staging services currently exist. Provisioning Supabase, Google Cloud/Maps, Vercel,
  Resend, telemetry and an NVD key is part of the intended workflow, subject to access.
- Use free or lowest-cost tiers first. Any paid commitment requires a focused PO
  decision; missing PITR, quota or checklist capability remains a blocker rather than
  being silently waived or automatically upgraded.
- Existing US-002 public/private-link behavior and versioned contracts must not change
  unless a discovered blocker is returned to architecture and the PO.
- Production data, DNS cutover, production secrets and production deployment are out of
  authority for this story.

**Dependencies and monitoring**

- Source dependency: commit the completed US-002 baseline before CI/release work.
- Access dependency: provider accounts, verified domains where required, least-
  privilege operator/service identities and GitHub environment configuration.
- Technical dependency: bounded request handling, non-local startup secret validation,
  private scanner ingress, real Supabase/ClamAV/Scheduler paths and exported alert
  metrics.
- Governance dependency: each paid capability, contract change, risk waiver or
  production action requires a separate PO decision.
- Progress is monitored by checklist completion and unresolved blocker count, not by
  elapsed calendar time.

### Open questions

- No provider accounts currently exist; setup begins only after intake/requirements
  approval and must use isolated staging resources.
- Founder is primary staging operator; a backup must be named before production.
- Provider plan capabilities are unknown. Intake does not assume PITR, quotas or paid
  features; requirements must turn unavailable capabilities into visible PO blockers.
- Requirements must define evidence format, applicability rules, severity/retest closure
  and the boundary between implementation work and credential-dependent staging work.

## Requirements

### User story and business value

**Primary operator story**

As the founder responsible for Wambe's release, I want the known authentication,
scanner and secret-management defects fixed and the complete release path exercised in
an isolated staging environment, so that a future production decision is based on
repeatable evidence rather than local/demo assumptions.

**Host protection story**

As a Wambe host, I need authentication, tenant isolation and uploaded-media processing
to fail safely under malformed or hostile input, so that my identity, venue and event
content are not exposed or activated unsafely.

**Release-authority story**

As Product Owner, I want every release-critical checklist item to pass or receive an
explicit waiver, with costs and destructive actions paused for my decision, so that
staging readiness is transparent and does not silently become production authorization.

**Current process:** uncommitted US-002 changes → local/demo tests → documented but
unexecuted release checklist → production no-go.

**Proposed process:** commit immutable baseline → implement SEC-001–004 → run CI/security
gates → create least-privilege isolated staging → deploy immutable artifacts → execute
auth/storage/scanner/maps/jobs/observability/recovery checks → independently retest
security → disposition every checklist item → PO staging-readiness decision. Any failed
critical check returns to implementation; paid capability, contract change, waiver,
destructive action or production action returns to the PO.

### In scope / out of scope

**In scope**

- Commit the completed US-002 work before US-003 implementation begins.
- Remediate and retest SEC-001 (bounded pre-authentication request handling) and SEC-002
  (non-local fail-fast secret validation).
- Remediate and retest checklist-required SEC-003 (`role=authenticated` JWT enforcement)
  and SEC-004 (same-origin OAuth return target).
- Run required CI, browser/accessibility, dependency, secret-history, image/SBOM and
  provenance checks against one immutable source revision.
- Create/configure isolated staging resources for Supabase, Resend, Google Cloud Run,
  Secret Manager, Scheduler, Google Maps, Vercel, telemetry and NVD scanning, using
  free/lowest-cost options first.
- Agent-guided configuration through user-authenticated CLI/provider tools, pausing
  before every paid commitment or destructive action.
- Execute and evidence all release-critical items in
  `docs/operations/staging-release-checklist.md`.
- Validate rollback, restore capability, alerting, log redaction and telemetry fail-open.
- Preserve existing host behavior, contracts and WCAG 2.2 AA regression evidence.

**Out of scope**

- Production deployment, production DNS cutover, production secrets/data, real host
  migration, public launch or production load/capacity certification.
- Automatic purchase or upgrade of any provider plan.
- New event features, visual redesign, contract-breaking API changes or vendor changes.
- General remediation of SEC-005–SEC-010, except where a staging checklist item requires
  a compensating control and Security explicitly accepts the evidence. These findings
  remain visible residual risks.
- Proving the strategic median-under-180-second KPI from synthetic staging traffic.

### Functional and non-functional requirements

**Business and functional requirements**

| ID | Requirement | Business value / success measure |
|---|---|---|
| BR-001 | Implementation must start from a committed, reviewable US-002 baseline; CI and release evidence reference that immutable commit. | Repeatable supply-chain evidence; Intake measure 3 |
| BR-002 | Staging-ready means every release-critical checklist item is PASS or explicitly WAIVED by the PO. A defect/blocker is a valid disposition but prevents a ready recommendation. | Transparent release decision; Intake measure 2 |
| BR-003 | The story may configure and test staging only. No production project, data, secret, DNS or deployment may be changed. | Scope/risk control |
| BR-004 | Free/lowest-cost services are preferred. Any paid commitment, unavailable control, destructive operation or risk waiver pauses for a focused PO decision. | Cost/risk control |
| FR-001 | API scanner callbacks and scanner dispatch must enforce a documented small request-envelope maximum before unbounded buffering or business processing. Oversized, missing/invalid-length and chunked-over-limit requests fail safely with `413`; invalid authentication remains neutral. | Closes SEC-001; Intake measure 1 |
| FR-002 | Local development secrets may exist only in explicit local/test configuration. Staging/production API and scanner startup must fail when scanner HMAC is absent, blank, known-default or shorter than the approved entropy floor; deployed internal job keys must be absent/empty. | Closes SEC-002; Intake measures 1 and 4 |
| FR-003 | Host API JWT validation must require valid issuer, signature, expiry, audience, UUID subject and `role=authenticated`; missing, `anon` and `service_role` roles are rejected. | Closes SEC-003; checklist auth boundary |
| FR-004 | OAuth callback navigation must resolve to the configured Wambe origin and approved internal paths. Protocol-relative, encoded separator, backslash, control-character and external-origin targets are rejected to a safe default. | Closes SEC-004; phishing resistance |
| FR-005 | CI must lint contracts; verify both Java services; build the generated client; run frontend lint/type/unit/browser/accessibility/build; build images; and publish immutable digests with SBOM/provenance. | Intake measure 3 |
| FR-006 | Security automation must complete full-history secret scanning, npm audit and Java dependency analysis using an NVD API key; failures produce visible blockers, not implied passes. | Intake measures 3 and 4 |
| FR-007 | Staging identity must prove Google OAuth, verified email/password, reset delivery, refresh, safe linking, exact CORS and cross-owner/RLS denial using synthetic accounts. | Checklist authentication readiness |
| FR-008 | Staging media must prove signed quarantine upload, exact-size/type checks, clean image/PDF promotion, EICAR rejection, callback replay/path protection, allowed scanner destinations and real object deletion. | Safe host media and checklist readiness |
| FR-009 | Restricted Maps keys, desktop/mobile address-pin behavior, public/private-link previews and protected/unpublished/deleted metadata behavior must be verified on the public staging domain. | Host journey and privacy regression |
| FR-010 | Flyway state, Scheduler OIDC scan dispatch/retention, empty job-key rejection, synthetic retention outcomes and database/object reconciliation must be evidenced. | Lifecycle and retention integrity |
| FR-011 | Staging must expose sanitized health/RED metrics and tested publish/API alerts. Missing scan/auth/retention/quota metrics remain blockers unless implemented or explicitly waived. Telemetry loss must not block save/publish. | Observable, fail-open operations |
| FR-012 | API, scanner and Vercel rollback drills must restore the prior artifact without down migrations. Supabase restore/PITR capability and measured RPO/RTO must be demonstrated or raised as a paid-plan blocker. | Intake measure 5 |
| FR-013 | Evidence must record commit, image digests, deployment identifiers, safe request IDs, result, timestamp and reviewer, with no credentials, signed URLs or personal content. | Auditability and privacy |
| FR-014 | The completed checklist and independent QA/Security retests determine the recommendation; implementation authors cannot self-close SEC findings. | Independent assurance |

**Non-functional requirements**

| ID | Requirement |
|---|---|
| NFR-001 | Zero open Critical/High findings are permitted in the staging-ready recommendation. |
| NFR-002 | Request-envelope rejection must occur without memory use proportional to attacker-controlled body size; the exact limit is architecture-owned and tested at/below/above boundary. |
| NFR-003 | Secrets use least privilege, provider secret stores and short-lived/operator-authenticated access; no static cloud credential is committed or placed in evidence. |
| NFR-004 | Tests and setup are repeatable from documented scripts/templates; provider-console-only steps must record reproducible configuration evidence. |
| NFR-005 | Staging uses synthetic data and log/metric labels exclude tokens, signed URLs, event titles, addresses, filenames and request bodies. |
| NFR-006 | Existing OpenAPI/product-event contracts, four visibility modes, accessibility and local development remain backward compatible unless the PO reopens architecture. |
| NFR-007 | Cost changes are fail-closed: unknown/free-tier limitations are reported before purchase, and no cost estimate is invented without provider evidence. |
| NFR-008 | Staging evidence establishes correctness and recoverability, not production scale, availability commitments or KPI representativeness. |

### Business rules and dependencies

1. The PO is the sole authority for paid services, waivers, contract changes, destructive
   operations and production actions.
2. The user creates/authenticates provider accounts; the agent may configure staging
   through authenticated tools and must pause before paid or destructive actions.
3. Secrets are entered through provider/CLI authentication or secret stores, never chat,
   source files, workflow logs or evidence documents.
4. A checklist `BLOCKED`/`FAIL` item is traceable progress but prevents staging-ready
   status unless the PO explicitly waives that specific risk.
5. Security closes SEC-001–004 only after independent evidence; QA owns regression and
   end-to-end acceptance evidence.
6. Existing US-002 contracts are canonical. A needed contract/privacy change blocks work
   and returns to architecture/PO.
7. Staging and production identities/resources are separate. Staging contains synthetic
   identities, events and files only.
8. EICAR is used solely in the authorized staging scanner path and is never promoted to
   active storage.
9. Flyway remains forward-only; rollback shifts immutable application revisions and
   uses reviewed forward fixes rather than editing migration history.
10. SEC-005–SEC-010 remain recorded unless separately remediated or waived; passing
    US-003 must not imply they were closed.

**Dependencies**

- Committed US-002 baseline and a clean working tree before implementation.
- GitHub repository Actions/environments and free NVD API key.
- New isolated Supabase, Resend, Google Cloud/Maps, Vercel and telemetry resources.
- Domain/redirect verification where provider flows require it.
- Provider free-tier capability discovery; paid-only gaps require PO decisions.
- Founder as primary staging operator; backup operator required before production.

**Data and evidence quality**

- Synthetic accounts/events/media are the only allowed staging test data.
- Evidence status values are `NOT_RUN`, `PASS`, `FAIL`, `BLOCKED`, `WAIVED`; each
  non-`NOT_RUN` item requires timestamp, source and reviewer.
- Screenshots are secondary evidence; machine-readable CI/test/provider output is
  preferred and must be redacted.
- Small synthetic samples cannot validate production throughput, availability or the
  under-180-second product KPI.

### Acceptance criteria

1. **AC-001 — Immutable baseline and CI**
   - Given the approved US-002 work, when US-003 implementation begins, then it is based
     on a committed revision and required CI is green for that exact revision.
2. **AC-002 — Bounded unauthenticated bodies (SEC-001)**
   - Given scanner dispatch or API callback requests below, at and above the documented
     envelope limit, including chunked/no-length input, when received, then valid bounded
     input proceeds to authentication and oversized input receives `413` before
     proportional buffering; burst evidence shows stable memory/instance behavior.
3. **AC-003 — Fail-fast deployed secrets (SEC-002)**
   - Given staging/production profiles, when HMAC is absent, blank, known-default or
     below the approved entropy floor, then API/scanner startup fails; when a rotated
     Secret Manager value is supplied, both start and only that value authenticates.
   - And `INTERNAL_JOB_KEY` is empty and rejected in staging.
4. **AC-004 — Authenticated host role (SEC-003)**
   - Given otherwise valid signed JWT fixtures/real tokens, then
     `role=authenticated` is accepted and missing/anon/service-role claims are rejected.
5. **AC-005 — Safe OAuth return target (SEC-004)**
   - Given valid internal paths and crafted external/backslash/encoded/control-character
     targets, then only same-origin approved paths are followed and all unsafe values
     resolve to the safe default.
6. **AC-006 — Supply-chain assurance**
   - Given the release commit, then contract lint, Java/frontend/browser/image builds,
     Gitleaks, npm audit and NVD-backed Java dependency checks pass; immutable API/scanner
     digests and SBOM/provenance are recorded without secrets.
7. **AC-007 — Real staging authentication/isolation**
   - Given synthetic staging users, then Google and verified email/password/reset/refresh/
     linking work, exact CORS is enforced, and cross-owner/RLS access remains denied.
8. **AC-008 — Real staging media safety**
   - Given clean supported files, EICAR, wrong size/type and replay/path attacks, then
     clean files alone reach active storage, unsafe files are rejected, drafts survive,
     and database/storage state reconciles.
9. **AC-009 — Maps and public metadata**
   - Given desktop/mobile staging and each visibility/lifecycle state, then restricted
     Maps behavior works and only approved metadata/unfurls are externally observable.
10. **AC-010 — Jobs and retention**
    - Given exact Scheduler OIDC identity and synthetic aged records, then dispatch and
      retention succeed idempotently, the local key fails, and rows/objects are removed
      consistently.
11. **AC-011 — Observability and privacy**
    - Given normal/failing publication and a telemetry outage, then required alerts fire,
      save/publish remain functional when telemetry is unavailable, and sampled logs/
      metrics contain none of the prohibited sensitive fields.
12. **AC-012 — Rollback and restore**
    - Given new staging revisions, then API, scanner and Vercel return to prior versions
      without down migration; restore capability/RPO/RTO is measured or blocks for PO
      plan decision, and deleted content is not restored to active service.
13. **AC-013 — Checklist/readiness decision**
    - Given all staging work, then every checklist item has evidence and status; any
      release-critical `FAIL`, `BLOCKED` or `NOT_RUN` prevents a staging-ready
      recommendation unless explicitly waived by the PO.
14. **AC-014 — Scope and cost guard**
    - Throughout execution, no production resource/data is changed and no paid or
      destructive action occurs without a focused PO decision.
15. **AC-015 — Regression compatibility**
    - Existing US-002 API/product-event contracts, host flows, visibility behavior,
      accessibility and local developer workflow pass regression without unapproved
      behavioral expansion.

**Traceability summary**

- SEC-001 → FR-001, NFR-002, AC-002.
- SEC-002 → FR-002, NFR-003, AC-003.
- SEC-003 → FR-003, AC-004 and checklist authentication.
- SEC-004 → FR-004, AC-005 and checklist authentication.
- Supply chain/privacy → FR-005/006/013, NFR-003/005, AC-006/011.
- Full staging checklist → FR-007–014 and AC-007–015.
- Cost/no-production constraints → BR-003/004, NFR-007, AC-014.

### Open questions

- Architecture must choose and document the exact request-envelope limit and enforcement
  layer(s); requirements specify observable boundary behavior, not implementation.
- Provider project names, regions, domains, redirect URLs and service-account identities
  are execution-time configuration, not product decisions.
- Free-tier PITR, telemetry hosting, quotas and external-preview capabilities are unknown
  until accounts are created. Any paid gap returns to the PO; it cannot be silently
  waived.
- SEC-008's destination restriction is required by the staging checklist but is not one
  of the four findings selected for code closure. Architecture/Security must decide
  whether network egress allowlisting is sufficient compensating evidence or whether
  application URL allowlisting becomes required scope.
- The backup operator remains unnamed; this blocks production readiness, not staging
  execution.
## UX

### User journey and flows

US-003 is an operational hardening story, not a product-feature story. The approved
requirements explicitly preserve the US-002 host journeys and prohibit a visual redesign.
The primary user is therefore the founder/operator moving from an immutable baseline to
an evidence-backed staging-readiness decision:

1. confirm the committed source revision and local gates;
2. review independent SEC-001–004 implementation, QA and Security status;
3. authenticate to isolated staging providers without placing secrets in evidence;
4. execute each release-critical checklist item;
5. record a redacted result as `NOT_RUN`, `IN_PROGRESS`, `PASS`, `FAIL`, `BLOCKED` or
   `WAIVED`;
6. return failures to implementation and pause blocked/paid/destructive work for a
   focused PO decision;
7. request staging-ready review only when all critical items pass or carry an explicit
   PO waiver; and
8. exit to a separate production decision, which remains unavailable in this story.

The complete success, failure, blocker and exit flow is in
[WIREFRAMES.md](WIREFRAMES.md#user-flow).

**Evidence versus assumptions:** Approved requirements and the existing operations
checklist validate the state model, production boundary and operator role. No founder
usability session, support analytics or production release data exists yet. The
information hierarchy is therefore a design assumption to validate with a short founder
desk check before implementation handoff; it does not justify building a custom
dashboard.

### Screens, components, content, and states

No new host-facing screen or product component is required. The operator's interface is
the existing Markdown checklist plus GitHub Actions and provider consoles. The conceptual
readiness view organizes those sources into:

- **Overview:** environment, source commit, immutable image/deployment identifiers,
  critical-control progress, blockers and readiness state.
- **Security closure:** separate implementation, QA and Security evidence for SEC-001,
  SEC-002, SEC-003 and SEC-004; authors cannot self-close findings.
- **Provider checks:** auth/isolation, storage/scanner, Maps/public metadata, jobs,
  observability and recovery groups.
- **Evidence detail:** control ID, status, safe source/reference, timestamp, reviewer and
  non-sensitive outcome. Secrets, signed URLs, request bodies and personal content never
  render.
- **Decision state:** impact, free alternative, capability/cost evidence and explicit PO
  action for paid, destructive or waived work. Approval is never preselected.
- **Readiness summary:** passes, unresolved critical items, waivers and residual risks.
  It says “staging-ready,” never “production-approved.”

`NOT_RUN` is the intentional empty state and identifies its prerequisite. `IN_PROGRESS`
shows owner and safe progress. `PASS` requires immutable evidence. `FAIL` creates a
defect and returns to implementation. `BLOCKED` pauses for access/capability/PO input.
`WAIVED` requires the explicit PO command, scope, reason and date. Critical `FAIL`,
`BLOCKED` or `NOT_RUN` states disable readiness review unless an explicit waiver changes
the decision.

Host behavior remains unchanged: SEC-001 rejects at an internal protocol boundary;
SEC-002 fails service startup; SEC-003 retains neutral unauthorized behavior; and SEC-004
resolves unsafe callback targets to the existing safe `/events` destination without
echoing attacker-controlled content.

### Responsive and accessibility requirements

The durable checklist remains linear and reflows naturally. If a generated/custom
readiness viewer is later approved, desktop may use persistent group navigation and
two-column summaries; below tablet width it becomes one ordered section list. Mobile
keeps all evidence and decision content, places blockers before actions and never hides a
critical status.

- Use one page heading, ordered section headings and a skip link to the first failing or
  blocked control.
- Statuses require visible text; colour, icons and position are supplementary.
- Keyboard order is summary → critical controls → blocker → evidence → review action.
- Opening evidence or decision detail moves focus to its heading; closing returns focus
  to its trigger.
- Concise status changes use a polite live region. Deployment failure or evidence of
  secret exposure uses an assertive alert.
- Disabled review/production actions have adjacent explanations and are not the only way
  to discover blockers.
- Meet WCAG 2.2 AA contrast, visible-focus, 200% zoom/reflow and 24×24 CSS-pixel target
  requirements with adequate spacing.
- No motion is necessary; reduced-motion mode therefore loses no information.
- English is the MVP operator language. Timestamps show UTC and, where available, the
  operator's local timezone.

Use direct labels (`Pass`, `Fail`, `Blocked`, `Waived`, `Not run`, `In progress`) and
never label a merely dispositioned failure as `Done`. Evidence links name the control
and date rather than exposing secret-bearing provider URLs.

### Wireframes or prototype links

- Durable user flow, desktop/mobile frames and state contract:
  [WIREFRAMES.md](WIREFRAMES.md)
- Visual low-fidelity board:
  [US-003 staging readiness Canvas](C:/Users/olatu/.cursor/projects/c-Users-olatu-OneDrive-Desktop-wambe/canvases/us-003-staging-readiness.canvas.tsx)

These frames are an information-design handoff, not a requirement to implement a new
dashboard. Prefer the existing checklist and provider/GitHub patterns. If a custom
viewer becomes necessary, reuse Wambe typography, spacing, focus, button, status-summary
and error-summary patterns; use a typed status enum and a redacted evidence display
model rather than inferring success from an evidence link.

### Usability validation and success signals

Run a lightweight founder desk check against one mixed-state example. Within one minute,
the operator should identify the current environment, source revision, first
release-critical blocker, owner/next safe action, existing evidence and whether PO
approval is required. QA should exercise pass, fail, blocked, waived and mixed states on
desktop and narrow viewports with keyboard and screen-reader checks.

UX succeeds when statuses are unambiguous, no production action appears authorized, no
credential-bearing value renders, all critical blockers are discoverable and existing
host flows show no responsive or accessibility regression. These are design-quality
signals, not proof of production readiness or the under-180-second product KPI.

### Risks and open decisions

- A dashboard built from these conceptual frames would add unjustified implementation,
  authentication and maintenance scope. Architecture should keep the checklist as the
  source of truth unless the PO explicitly approves a new operator product surface.
- Provider-console terminology and accessibility vary; reproducible Markdown evidence
  must remain understandable without screenshots alone.
- Long evidence sets can create cognitive overload. Default to grouped summaries, place
  failures/blockers first and disclose full logs only on request.
- Architecture still owns the exact SEC-001 envelope limit and SEC-008 destination
  control. Neither changes the approved UX unless it introduces a host-visible state.
- Any maintenance mode, rollout flag, new outage screen or changed host error copy is a
  scope change requiring PO and UX review.
- The founder desk check remains unperformed; this is the only open UX validation item
  and may be completed during architecture review without blocking the documented flow.

**Developer handoff:** Preserve existing host copy/routes and implement no new frontend
screen for US-003. Backend and operations work should expose deterministic, redacted
status/evidence that maps to the state contract. QA owns host regression and mixed-state
accessibility checks; Security independently closes SEC-001–004.

## Architecture

### Context and selected approach

**Review date:** 2026-07-14  
**Change impact:** medium breadth, high security importance. Changes touch the two Java
services, the Next.js authentication boundary, delivery evidence and isolated staging
configuration, but do not alter product behavior, the v1 public API or the product data
model.

The as-built review confirmed the four selected gaps:

- `CachedBodyRequest` in `wambe-api` and `DispatchHmacFilter.CachedRequest` in
  `media-scanner` call unbounded `readAllBytes()` before HMAC authentication (SEC-001).
- both services inherit `local-scanner-secret-change-me` when
  `SCANNER_HMAC_SECRET` is absent; only a deploy script checks the value (SEC-002);
- `SecurityConfig.hostJwtDecoder` validates issuer, expiry and audience but not the
  Supabase `role=authenticated` user boundary (SEC-003); and
- `/auth/callback` accepts a one-slash value such as `/\evil.example`, which WHATWG URL
  resolution can treat as an external authority (SEC-004).

The scanner also dereferences request-provided storage and callback URLs without an
application destination policy (SEC-008). Internal ingress and HMAC reduce exposure but
do not satisfy the staging checklist when a default secret or compromised sender is in
the threat model.

**Selected approach:** apply the smallest contract-preserving controls at the existing
boundaries:

1. enforce one 16 KiB JSON envelope before buffering or HMAC work on both internal
   scanner endpoints;
2. move development credentials to explicit `local`/`test` profiles and fail startup in
   `staging`/`production` on missing, blank, short or known-default scanner HMAC values;
3. require the Supabase user role and UUID subject in the host JWT validator;
4. centralize same-origin post-auth path validation in a pure Next.js helper used both
   before OAuth initiation and by the authoritative callback;
5. require application-level scanner destination allowlisting, with paid VPC egress
   controls optional defence-in-depth; and
6. deploy the unchanged system to isolated staging from immutable artifacts and record
   independent, redacted evidence against a versioned schema.

The full context, trust-boundary, sequence, evidence-model, rollout and dependency views
are in [ARCHITECTURE_DIAGRAMS.md](ARCHITECTURE_DIAGRAMS.md).

### Components, interfaces, and data

| Component/boundary | Existing responsibility | US-003 architectural delta |
|---|---|---|
| Next.js `AuthPanel` and `/auth/callback` | Initiate Supabase OAuth/email flows, exchange PKCE code and navigate to host route | Shared `safeRedirectPath` policy, fixed safe default, negative tests and outcome-only callback telemetry; no new screen |
| Spring Security host chain | Stateless Supabase bearer validation and CORS | Require signature, issuer, expiry, `aud=authenticated`, `role=authenticated` and UUID `sub`; keep neutral `401` |
| API scanner callback chain | Authenticate HMAC callback and pass replayable body to generated controller | Path-scoped 16 KiB envelope check before body cache/HMAC; minimal `413`; existing nonce/idempotency retained |
| API startup configuration | Bind database, storage, scanner and Scheduler configuration | Explicit profile policy and deployed-secret validator before readiness/listening |
| API→scanner service call | Dispatch one signed scan request | In staging, API service account obtains a Google ID token for the scanner audience and sends it in `X-Serverless-Authorization`; scanner also verifies the existing HMAC for body integrity |
| Media-scanner dispatch chain | Authenticate API dispatch HMAC and invoke one scan | Cloud Run IAM/private ingress first, then the same bounded-envelope and deployed-secret controls as API |
| Media-scanner outbound client | Read quarantined object, write preview and return callback | Validate all three destinations before any network call; disable redirects; apply explicit timeouts |
| Supabase staging | Auth, PostgreSQL/RLS and private storage | New isolated project with synthetic identities/data; no schema change |
| GCP staging | Cloud Run, Scheduler, Secret Manager and Artifact Registry | New isolated project and least-privilege identities; scanner remains internal and concurrency one |
| Vercel staging | Host web and public metadata | Separate staging project/origin, demo mode off, exact environment values |
| GitHub Actions | CI, scheduled security scans and image publication | All release evidence references one source SHA; WIF publishes digest-addressed images with SBOM/provenance |
| Checklist/evidence | Human-readable operational source of truth | Machine-checkable release/control metadata using `STAGING_EVIDENCE_SCHEMA.json`; no custom dashboard |

**Staging identities and privileges**

- The browser receives only the Supabase anonymous key, restricted Maps browser key and
  public service URLs. It never receives database, service-role, scanner or telemetry
  credentials.
- `wambe-api` uses a dedicated Cloud Run service account, the application database role,
  server-side Supabase service-role access and `roles/run.invoker` on only the scanner
  service.
- `media-scanner` uses a separate service account, no database/service-role credential,
  the shared scanner HMAC and short-lived allowlisted signed URLs only.
- Cloud Scheduler uses a dedicated identity whose exact issuer, subject and audience are
  validated by the API. `INTERNAL_JOB_KEY` is empty outside local/test.
- GitHub publishes through Workload Identity Federation and an Artifact Registry writer;
  no static Google JSON key is stored in GitHub.
- Founder/operator access is human-authenticated through provider CLIs/consoles. Secret
  values are entered into provider secret stores, never chat, source or evidence.

**Data:** no Flyway migration or product table is expected. Existing PostgreSQL/RLS,
media state, nonce and idempotency models remain authoritative. The new
[staging evidence schema](STAGING_EVIDENCE_SCHEMA.json) is an operational file contract,
not application storage. It relates one immutable release record to many control
evidence records and conditionally requires a defect for `FAIL` and a PO decision for
`WAIVED`.

### API, event, and integration contracts

**Inherited product contracts remain unchanged.** No endpoint, request field, response
field, product event or media lifecycle value is added. The following are boundary and
configuration contracts.

#### SEC-001 request-envelope contract

- `POST /scan` and `POST /api/v1/internal/scanner/callback` accept at most **16,384
  bytes** of JSON. Typical signed dispatch bodies are below 5 KiB and callback bodies
  below 2 KiB; 16 KiB gives signed-URL headroom while bounding pre-authentication memory.
- A declared length above the limit is rejected without reading the body. Malformed,
  negative, conflicting or absent fixed-body length fails with `413` as required. A
  correctly declared chunked request is read through a `limit + 1` bounded stream and
  receives `413` only when it exceeds the limit.
- The bounded reader runs before the current body cache and HMAC filters. Cached wrappers
  consume only the bounded byte array; no raw attacker-controlled stream uses
  `readAllBytes()`.
- Ordering is: method/envelope → HMAC headers/timestamp/signature → strict JSON/Bean
  Validation → handler.
- Oversize receives a minimal `413` response and safe request ID where available. Invalid
  authentication on a valid-size body remains the existing neutral `401`. Neither
  response echoes headers, body, URL or signature material.
- The shared property is `wambe.security.request-envelope.max-bytes=16384` in each
  service. At-limit positive tests and below/above/chunked/no-length negative tests make
  configuration drift visible.

#### SEC-002 deployed-secret contract

- Known development values exist only under explicit `local`/`test` profiles.
  `application.yml` has no fallback scanner HMAC or internal-job key suitable for a
  deployed profile.
- A `staging` or `production` context fails during construction, before readiness, when
  `SCANNER_HMAC_SECRET` is absent, blank, equal to any committed development default or
  shorter than 32 characters. Provisioning must generate it from at least 32 random
  bytes using a cryptographically secure generator; runtime length/default checks cannot
  by themselves prove entropy.
- API deployed profiles additionally fail when `INTERNAL_JOB_KEY` is non-empty. Cloud
  Scheduler OIDC is the only deployed job credential.
- Secret Manager injects the same rotated staging HMAC into API and scanner. The secret
  is never exposed in health, exceptions, logs or evidence. Rotation requires new
  revisions of both services and an only-new-secret smoke test.
- `check-deploy-env.ps1` remains a pre-deploy defence and must match, but cannot replace,
  runtime validation.

#### SEC-003 host JWT contract

Host routes accept a token only when all of the following hold: trusted signature,
configured Supabase issuer, valid temporal claims, audience contains `authenticated`,
claim `role` equals exactly `authenticated`, and `sub` parses as UUID. Missing role,
`anon`, `service_role`, other roles or malformed subjects fail as invalid tokens with a
neutral `401`. Scanner and Scheduler continue through their separate HMAC/OIDC chains.

#### SEC-004 return-target contract

A shared pure helper receives the untrusted `next` value and configured
`NEXT_PUBLIC_SITE_URL` origin. It:

1. defaults absent, blank or invalid input to `/events`;
2. requires exactly one leading `/`;
3. rejects backslashes, controls, protocol-relative values and encoded slash/backslash
   bypasses after at most two bounded decode checks;
4. resolves against the configured origin and requires exact origin equality;
5. permits only `/events`, descendants of `/events`, and `/auth`;
6. returns normalized path plus query and discards fragments.

The helper is applied before `router.push`, email/OAuth `redirectTo` construction and in
the callback; the callback remains authoritative. Unsafe values are not reflected in
copy or telemetry. Missing/mismatched staging site-origin configuration fails deployment
validation rather than trusting forwarded host headers.

#### Private Cloud Run scanner invocation

- The scanner keeps `internal` ingress and has no `allUsers` invoker binding. The API
  service account receives `roles/run.invoker` on that scanner service only.
- For staging, the API obtains a Google-signed ID token through Application Default
  Credentials with audience equal to the scanner's full Cloud Run service URL. It sends
  this in `X-Serverless-Authorization`; HMAC headers remain separate and continue to
  authenticate payload integrity inside the container.
- API traffic to the scanner's default `run.app` URL must be routed through a same-project
  VPC path recognized as internal by Cloud Run (Direct VPC egress or the approved
  connector/private path). Provisioning selects the lowest-cost supported option and
  pauses if it creates a paid commitment.
- Local/test profiles disable Google ID-token acquisition and continue to use the local
  scanner URL plus HMAC. Deployed profiles fail configuration when scanner URL/audience
  is absent, non-HTTPS or mismatched.

This follows Google Cloud's service-to-service contract: a permitted caller presents an
ID token whose audience is the receiving service URL
([service-to-service authentication](https://cloud.google.com/run/docs/authenticating/service-to-service)).
Cloud Run-to-Cloud Run traffic must use an eligible VPC path to satisfy `internal`
ingress
([private networking](https://cloud.google.com/run/docs/securing/private-networking)).
IAM/network authentication and application HMAC are intentionally both required in
staging.

#### SEC-008 scanner destination contract

Before any outbound request, a `ScannerDestinationPolicy` validates:

- `readUrl` and `previewWriteUrl`: HTTPS, no userinfo, default port, exact configured
  Supabase host, approved `/storage/v1/object/` path family and no IP-literal host;
- `callbackUrl`: exact configured API origin and exact
  `/api/v1/internal/scanner/callback` path, without query or fragment; and
- all clients: redirect following disabled. A redirect response or policy mismatch is a
  scan failure and no second destination is contacted.

The valid hosts/origin derive from deployed `SUPABASE_URL` and `API_BASE_URL`; callers do
not supply the allowlist. Network egress filtering may later reinforce this policy but
is not the primary control.

#### Evidence contract

`STAGING_EVIDENCE_SCHEMA.json` version `1.0` records only:

- story/environment and immutable source SHA;
- optional API/scanner digests and Vercel deployment identifier as they become available;
- per-control ID and one of `NOT_RUN`, `IN_PROGRESS`, `PASS`, `FAIL`, `BLOCKED`,
  `WAIVED`;
- UTC timestamp, reviewer role/reference, redacted source references and bounded outcome
  text for every executed/dispositioned control;
- defect reference for `FAIL` and explicit PO decision reference for `WAIVED`.

The schema cannot identify a leaked secret. Human review, safe evidence producers and
full-history secret scanning remain mandatory. Final readiness cannot contain a
release-critical `NOT_RUN`, `IN_PROGRESS`, `FAIL` or `BLOCKED`.

#### Consistency, timeouts, retries and idempotency

- Existing at-least-once scan dispatch remains: two-minute lease and 1/5/20-minute retry
  progression. A `413` is a configuration/programming defect, not a larger-body retry.
- Existing callback nonce consumption, same-digest replay, matching terminal replay,
  conflicting-terminal rejection and idempotent storage promotion remain unchanged.
- Keep 5-second outbound connect and 30-second storage read/write timeouts; add an
  explicit 10-second API→scanner response and scanner→API callback response timeout.
- HMAC timestamps retain the five-minute window. Every retry receives a new dispatch
  nonce; callback idempotency protects repeated terminal outcomes.
- Telemetry export remains fail-open and never participates in save, publish, scan state
  or authentication decisions.

### Quality attributes, resilience, security, cost, and observability

**Security and privacy**

- Application-level limits and destination policy close the root causes; internal
  ingress, concurrency one, Secret Manager and exact CORS remain defence-in-depth.
- Stage and production projects, identities, secrets and data are separate. Staging uses
  synthetic accounts, events and media only.
- RLS, explicit owner predicates, private buckets, signed operation-scoped URLs and
  server-side service-role handling remain unchanged and require real staging regression.
- Evidence/logs exclude access/refresh tokens, secrets, signed URLs, request bodies,
  titles, addresses and filenames. Metrics use bounded route/reason labels only.
- SEC-005–SEC-007 and SEC-009–SEC-010 remain residual findings unless separately
  remediated or explicitly waived; passing US-003 must not imply closure.

**Reliability, scalability and performance**

- The 16 KiB bound makes pre-authentication heap use constant per request. Cloud Run
  scanner concurrency stays one and maximum scale stays three until measured evidence
  supports change.
- API and scanner still scale to zero. Cold starts are measured in staging; no paid
  minimum instance is assumed.
- Signed object download remains capped at 10 MiB. One scan handles one object; direct
  browser upload continues to bypass API memory.
- API/scanner revisions roll back independently. A scanner outage leaves drafts
  available and media non-active; it must never promote without a valid clean callback.

**Cost**

- Use current free/lowest-cost provider tiers and application allowlisting. No API
  gateway, Cloud Armor or custom dashboard is introduced.
- Internal scanner ingress requires a supported same-project VPC route from the API.
  Prefer Direct VPC egress when provider evidence shows it is the lowest-cost suitable
  option; a connector, load balancer or any billed network commitment pauses for PO
  approval.
- Artifact Registry, Scheduler, Cloud Run, Maps or telemetry may still require billing
  enablement or exceed free allowances. Execution must obtain provider evidence and
  pause before commitment.
- If Supabase free tier cannot perform the required restore/PITR drill, the item remains
  `BLOCKED` pending a PO purchase or explicit waiver; architecture does not assume a paid
  feature.

**Observability**

- Add low-cardinality counters for request-envelope rejection, JWT-role rejection,
  unsafe return-target fallback and scanner destination rejection. A deployed-secret
  startup failure cannot reliably export a metric because the service never starts; emit
  only a stable non-sensitive failure code and use failed-revision/provider logs as
  evidence. Never label with token, URL, path parameter, media ID or user content.
- Export scan result/age and retention outcome metrics from the API and an auth-callback
  outcome from Next.js/Vercel telemetry. Provider-native metrics cover Cloud Run
  instance/memory, database/storage quota and Vercel availability.
- Extend alerts/runbooks for oldest scan job, retention failure and auth-callback failure.
  Telemetry-drop and provider-quota checks may use provider alerts. Any unavailable
  release-critical signal remains a blocker or explicit PO waiver, not an implied pass.
- `X-Request-Id`, immutable revision and deployment IDs correlate evidence. HMAC/JWT
  rejection logs reveal only a stable reason code and service/route.

### Migration, rollout, rollback, and validation plan

**Migration and compatibility**

- No OpenAPI/product-event version or database migration is planned.
- Configuration changes are additive except removal of insecure base-profile defaults.
  Local Compose and automated tests must explicitly activate `local`/`test`.
- Previous API/web clients remain compatible. The stricter JWT check rejects only tokens
  outside the already-approved host trust boundary.
- The evidence schema is versioned independently; incompatible evidence changes require
  a new schema version rather than editing recorded evidence.

**Rollout**

1. Commit the completed US-002 baseline and obtain green CI for that immutable SHA.
2. Implement/test backend SEC-001/002/003/008 controls and observability; obtain backend
   gate approval.
3. Implement/test the frontend SEC-004 helper/callback integration; obtain frontend gate
   approval.
4. Run CI and Security on one release commit, including NVD-backed Java analysis and
   full-history Gitleaks; build once with SBOM/provenance and record image digests.
5. Through founder-authenticated tools, provision isolated staging identities/resources.
   Configure the lowest-cost supported internal VPC path, grant only the API service
   account scanner invoker, and deploy scanner first with internal ingress and rotated
   HMAC. Deploy API with Google ID-token dispatch and zero host traffic, then Vercel with
   demo mode off.
6. Apply Flyway using the migration principal, verify expected versions, and execute
   auth/RLS, media/EICAR, Maps/metadata, Scheduler/retention, observability and
   fail-open checks.
7. QA runs acceptance/regression; Security independently retests SEC-001–004 and
   destination controls. Operations runs rollback/restore drills and completes evidence.
8. The PO receives a staging-readiness recommendation only; production remains a
   separate future decision.

**Rollback**

- Stop exposure and shift API/scanner independently to prior healthy Cloud Run revisions;
  restore the prior Vercel deployment.
- Do not down-migrate or edit Flyway history. Use the compatible prior API or a reviewed
  forward fix.
- If HMAC compromise is possible, rotate Secret Manager and deploy both Java services
  before traffic resumes.
- Rerun scan dispatch for leased/scanning work, rerun retention after restore and verify
  deleted content is not active.
- Smoke health, authenticated host access, lean publish, metadata redaction and one clean
  upload. Record before/after revision IDs and duration without secret-bearing output.

**Validation**

- Unit/integration boundaries: body below/at/above 16 KiB; fixed/chunked/missing/invalid
  length; signed/unsigned; deployed secret matrix; JWT role/subject matrix; redirect
  attack matrix; destination scheme/host/port/path/redirect matrix.
- Resource check: repeated oversized requests show bounded heap/instance behavior.
- Supply chain: contract lint, both Maven verifies, frontend lint/type/unit/browser/
  accessibility/build, image build, Gitleaks, npm audit and NVD scan all pass on the
  release SHA.
- Staging: real synthetic Supabase auth/refresh/linking/RLS; signed uploads; clean media
  and EICAR; exact CORS; restricted Maps; metadata/unfurls; OIDC jobs; retention;
  telemetry outage; rollback and restore capability.
- Exit: all checklist controls have schema-valid redacted evidence, zero Critical/High
  in-scope findings remain, and no release-critical item is unresolved without an
  explicit PO waiver.

### Alternatives, risks, and ADRs

**ADR-style decisions — accepted by the Product Owner on 2026-07-14**

| ADR | Decision and rationale | Alternatives not selected | Reversibility |
|---|---|---|---|
| ADR-011 | One 16 KiB pre-auth JSON limit for dispatch and callback; ample legitimate headroom with simple parity and constant memory | Separate 8/32 KiB limits add drift; Cloud Run's larger platform limit and internal ingress do not close the code flaw | Low; configuration-only adjustment after measured staging payloads |
| ADR-012 | Explicit local/test secret defaults plus runtime staging/production fail-fast and 32-character minimum backed by CSPRNG provisioning | Deploy script or Secret Manager injection alone can be bypassed by omitted/misbound configuration | Low |
| ADR-013 | Host JWT requires `role=authenticated` and UUID `sub` in addition to issuer/signature/expiry/audience | Audience-only accepts tokens outside the approved user boundary; controller-only checks are inconsistent | Low |
| ADR-014 | Central same-origin, allowlisted path helper used before and after OAuth, defaulting to `/events` | Prefix-only checks are parser-unsafe; accepting arbitrary same-origin paths expands the post-auth surface unnecessarily | Low |
| ADR-015 | Application HTTPS destination allowlist is mandatory for scanner URLs; paid network egress filtering is optional | HMAC-only and network-only controls fail under default-secret/API compromise or add provider cost without validating application intent | Medium; host/path config can evolve behind the policy |
| ADR-016 | Existing Markdown checklist remains the operator source of truth with a small v1 JSON evidence schema | A custom dashboard creates a new authenticated product and maintenance burden; screenshots alone are not repeatable | Low |
| ADR-017 | Build once, deploy digest-addressed artifacts into fully isolated staging through WIF; no production action in US-003 | Rebuilding per environment breaks provenance; shared projects/secrets weaken isolation; static cloud keys increase exposure | Medium operational setup, low application coupling |
| ADR-018 | Scanner requires internal ingress, API-only Cloud Run IAM invocation and the existing HMAC; API obtains a Google ID token for the exact scanner audience | Public/unauthenticated invocation weakens the approved boundary; HMAC alone does not enforce caller identity; IAM alone does not bind the JSON body | Medium; local profile keeps HMAC-only development |

**Risks and mitigations**

- **16 KiB legitimate overflow:** staging records actual body size without URLs/body
  content. Raise the configuration only after evidence and matching tests.
- **Secret appears long but weak:** runtime checks catch common failure, while provisioning
  evidence requires CSPRNG generation and Secret Manager; never invent entropy from
  length alone.
- **Redirect parser discrepancies:** one shared helper, route-level tests and real Vercel/
  Supabase staging tests prevent frontend/callback drift.
- **Storage URL shape changes:** policy allows the provider's documented object path
  family, fails closed and can be adjusted without changing product contracts.
- **Private scanner path costs or routing failure:** verify Direct VPC egress and
  `run.app` routing in the selected region before mutation; pause before a billed
  connector/load balancer, and never make scanner ingress public as a workaround.
- **Provider capabilities/cost unknown:** record `BLOCKED` and return to the PO before
  purchase, destructive action or waiver.
- **Observability stack absent:** implement the required low-cardinality metrics first;
  unavailable hosting/alerts remain visible blockers rather than blocking product
  operations at runtime.

**Open PO decisions during later staging execution**

- purchase or waive Supabase restore/PITR if the free plan cannot demonstrate AC-012;
- fund or waive unavailable telemetry hosting/critical alert capability;
- approve a paid connector/load balancer if no no-cost supported internal scanner route
  is available;
- add stricter paid egress firewall controls as optional defence-in-depth;
- approve any other provider upgrade or checklist waiver; and
- name a backup operator before production (not required to execute staging).

These do not need to be guessed for architecture approval. The default is no purchase,
no waiver and a visible blocker.

### Implementation profile, order, and `[FE]` / `[BE]` / `[INT]` slices

**Approved profile:** `fullstack`  
**Approved order:** `backend-first`

Frontend work is required for SEC-004, so `backend-only` is invalid. Backend-first closes
the two High findings, establishes deployed configuration and stabilizes scanner/JWT
boundaries before the smaller callback slice. This preserves US-002 ADR-009 and avoids
parallel security-contract drift. The Product Owner approved this profile/order with the
architecture gate on 2026-07-14.

| Slice | Owner | Scope and completion evidence |
|---|---|---|
| US3-BE-01 `[BE]` | Backend | Bounded envelope reader/filter in both Java services; no raw unbounded cache; 16 KiB fixed/chunked boundary tests and minimal `413` |
| US3-BE-02 `[BE]` | Backend | Local/test profile split, runtime deployed-secret validators in API/scanner, empty deployed job key, startup matrix and only-rotated-secret smoke |
| US3-BE-03 `[BE]` | Backend | Supabase `role=authenticated` plus UUID-subject validator and signed JWT fixture matrix |
| US3-BE-04 `[BE]` | Backend | Cloud Run API→scanner ID-token adapter for exact audience, local-profile bypass, IAM/configuration negatives and explicit dispatch timeout |
| US3-BE-05 `[BE]` | Backend | Scanner destination policy, no redirects, explicit callback timeout and hostile destination tests |
| US3-BE-06 `[BE]` | Backend | Scan age/result, retention and security-rejection metrics with bounded labels; updated alert/runbook inputs |
| US3-FE-01 `[FE]` | Frontend | Pure safe-return helper used by callback, OAuth/email initiation and local navigation; route/unit attack matrix; existing host copy/routes unchanged |
| US3-FE-02 `[FE]` | Frontend | Outcome-only auth callback observability and staging regression; no new UI |
| US3-INT-01 `[INT]` | Backend first, Operations verifies | Validate evidence schema; align deploy checks/templates with runtime, scanner audience, internal VPC route and API-only invoker policy |
| US3-INT-02 `[INT]` | Operations | Commit/release SHA, WIF publication, SBOM/provenance and digest capture; Security workflow with NVD key |
| US3-INT-03 `[INT]` | Operations | Founder-authenticated isolated Supabase/GCP/Vercel/Maps/Resend/telemetry setup, pausing before cost/destruction |
| US3-INT-04 `[INT]` | QA/Security/Operations | Full staging checklist, independent SEC-001–004 retest, EICAR/auth/RLS/jobs/metadata evidence and rollback/restore drill |

**Gate ownership:** implementation authors do not mark security findings closed. QA owns
acceptance and host regression after both implementation gates. Security owns finding
closure. Operations owns environment, evidence completeness and staging handoff.

**Pre-implementation blocker:** the completed US-002 working tree must be committed
before US3-BE-01 starts so every result can reference an immutable baseline.

## Implementation — Backend

### Changed files and completed backend/integration slices

**Implementation baseline:** immutable commit `a9e0f16`
**Implementation date:** 2026-07-14

- **US3-BE-01 — bounded request envelopes:** added path-scoped, pre-HMAC
  `RequestEnvelopeFilter` implementations to the API callback and scanner dispatch
  boundaries. Both reject absent, malformed, conflicting, oversized or ambiguous fixed
  lengths, read chunked bodies through a `16,384 + 1` byte bound, include the safe API
  request ID where available, and fail closed with `413` if the bounded-body handoff is
  ever missing. Existing HMAC wrappers now cache only the bounded byte array; production
  code contains no raw servlet `readAllBytes()`.
- **US3-BE-02 — deployed secret gate:** moved known credentials into explicit
  `application-local.yml` and `application-test.yml` files for both Java services.
  `DeployedSecurityValidator` now fails every non-dev-only profile, including mixed
  profile attempts, when the scanner secret is missing, blank, short or known, when the
  API local job key is present, when local scanner destinations are enabled, or when
  the approved 16 KiB envelope setting drifts.
- **US3-BE-03 — host JWT boundary:** added `HostJwtClaimValidator` to the existing
  issuer/audience/signature/temporal validator chain. Host tokens now additionally
  require exact `role=authenticated` and a UUID `sub`; signed `anon`, `service_role`,
  missing-role and malformed/missing-subject fixtures fail as invalid tokens.
- **US3-BE-04 — private scanner invocation:** added Google ADC
  `IdTokenCredentials` support using `google-auth-library-oauth2-http 1.48.0`. Deployed
  dispatch sends `X-Serverless-Authorization: Bearer <Google ID token>` for the exact
  scanner audience plus the existing HMAC. Only explicit local/test-only profiles use
  the no-token provider. API dispatch now disables redirects and applies five-second
  connect and ten-second response timeouts.
- **US3-BE-05 — outbound scanner policy:** added `ScannerDestinationPolicy`, exact
  deployed Supabase/API origin startup validation, HTTPS/default-port/path checks and
  redirect-disabled clients. All read, preview-write and callback destinations are
  validated before the first outbound request. Storage retains its thirty-second
  operation timeout and callbacks now have five-second connect/ten-second response
  limits.
- **US3-BE-06 — backend observability:** added bounded-label counters for envelope, JWT
  and destination rejection, scan-result and retention outcomes, observed scan-job age,
  and last successful retention time. The scanner now exports Prometheus metrics.
  Alert rules cover API/scanner availability, security-boundary bursts, observed scan
  age, scan rejection ratio and failed/missed retention.
- **US3-INT-01 — deployment alignment:** updated Compose, Cloud Run templates,
  `check-deploy-env.ps1`, the operations handbook and staging checklist. Templates now
  carry scanner profile/destination inputs, exact scanner audience, API-only IAM notes
  and the Direct VPC egress placeholders needed for internal `run.app` invocation.
  No cloud resource was created or mutated.

Principal implementation locations are:

- `services/wambe-api/src/main/java/com/wambe/api/common/security/`
- `services/wambe-api/src/main/java/com/wambe/api/config/`
- `services/wambe-api/src/main/java/com/wambe/api/integration/scanner/`
- `services/wambe-api/src/main/java/com/wambe/api/observability/WambeMetrics.java`
- `services/media-scanner/src/main/java/com/wambe/scanner/`
- matching unit/integration tests under both services
- `infra/gcp/cloud-run/`, `infra/observability/prometheus/`,
  `scripts/check-deploy-env.ps1`, `compose.yaml` and `docs/operations/`

### API, data, migration, and compatibility notes

- The inherited OpenAPI v1, product-event schema, endpoint paths, request/response
  fields, callback HMAC, nonce/idempotency semantics and media lifecycle values are
  unchanged.
- There is no Flyway or product-data migration. The only added dependency is Google Auth
  for API service identity; Prometheus registry support was added to the scanner.
- Local and test behavior remains HMAC-only and supports `/dev-storage` solely through
  explicit profiles. Deployed profiles require HTTPS Supabase/API origins and cannot
  enable the local destination path.
- A scanner redirect or destination-policy rejection deliberately contacts no second
  destination, including the callback. The dispatch receives a non-success response,
  the existing API job logic marks the lease failed and applies the approved 1/5/20
  minute retry schedule, while media remains quarantined/non-active. A later valid retry
  reconciles the same job; this preserves the approved “no second destination” contract.
- No frontend code or SEC-004 return-target behavior was changed in this gate.

### Tests and verification evidence

- `services/wambe-api/mvnw.cmd -B clean verify` — **PASS**, 52 tests, zero failures,
  zero errors, zero skipped. This includes PostgreSQL/Testcontainers RLS, media callback,
  retention and security integration coverage plus the new envelope, secret/profile,
  signed-JWT, identity-header and metrics tests.
- `services/media-scanner/mvnw.cmd -B clean verify` — **PASS**, 24 tests, zero failures,
  zero errors, zero skipped. This includes exact 16 KiB boundaries, malformed/conflicting
  length handling, bounded unending streams, envelope/HMAC ordering, deployed startup
  matrices, hostile destination cases and redirect non-follow behavior.
- Signed host JWT fixtures cover `authenticated`, `anon`, `service_role`, missing role,
  malformed subject and missing subject against a real local JWKS decoder.
- Prometheus scrape output is asserted to expose
  `wambe_scan_job_age_seconds_max`, matching the checked-in alert expression.
- `scripts/check-deploy-env.ps1 -Environment staging` — **PASS** against a complete
  synthetic, credential-free staging configuration; synthetic values were removed from
  the shell immediately after the check.
- `git diff --check` — **PASS**. IDE diagnostics for both services, deployment script
  and infrastructure files reported no errors.

### Observability, limitations, risks, and rollback notes

- Metrics contain only fixed service/route/result/reason labels. They do not record
  tokens, signed URLs, media IDs, filenames, event content or request bodies.
- A secret/configuration startup failure emits only a stable
  `WAMBE_DEPLOYED_*` exception code because a service that fails before readiness cannot
  reliably export a metric.
- Security findings are **not marked closed by implementation**. QA and Security must
  independently retest SEC-001, SEC-002, SEC-003 and SEC-008. The frontend gate must
  separately implement SEC-004 before QA can start.
- Real Google ID-token acquisition, `roles/run.invoker`, internal ingress, Direct VPC
  routing, live Supabase signed URLs, EICAR, metrics scraping and alert delivery remain
  staging evidence. No provider account currently exists. Any billed connector, load
  balancer, egress route, telemetry service or plan upgrade remains a visible PO blocker.
- NVD-backed dependency analysis, full-history Gitleaks, image/SBOM/provenance builds and
  the machine-checkable release evidence file belong to the later Security/Operations
  execution and were not represented as complete here.
- Rollback is code/config-only because there is no schema change. Revert API and scanner
  revisions independently and keep host/media staging exposure disabled while a prior
  revision lacks these controls. Do not restore a default HMAC or local job key; rotate
  Secret Manager and redeploy both services if credential compromise is suspected.

## Implementation — Frontend

### Changed files and completed frontend/integration slices

- **US3-FE-01 `[FE]` — complete:** added the shared pure
  `safeRedirectPath`/`evaluateRedirectPath` policy. It bounds input to 2,048
  characters and two decode passes; rejects missing/blank/disallowed paths,
  protocol-relative values, backslashes, controls, dot segments and encoded path
  separators; resolves against the configured origin; permits only `/events`,
  `/events/**` and exact `/auth`; preserves query encoding and removes fragments.
- The policy is authoritative in `app/auth/callback/route.ts` and is also applied
  before Google OAuth, email verification/reset callback construction, password/demo
  `router.push` navigation and unauthenticated proxy return-target propagation.
- Added exact-origin configuration in `lib/auth/site-origin.ts`. Deployed callbacks
  never derive their redirect origin from request/forwarded host headers. Client
  origin mismatches fail closed, while demo fallback is restricted to loopback.
  Vercel preview/production builds reject demo mode and fail during config loading
  when `NEXT_PUBLIC_SITE_URL` is absent, malformed, non-HTTPS or non-origin-shaped.
- **US3-FE-02 `[FE]` — complete:** callback handling emits a fail-open structured
  `wambe.auth.callback` event containing only bounded outcome, fallback flag and
  fallback-reason labels. It never records code, token, email, raw `next`, URL or
  user/event content.
- Principal frontend files:
  - `apps/web/next.config.ts`
  - `apps/web/src/proxy.ts`
  - `apps/web/src/lib/auth/safe-redirect-path.ts`
  - `apps/web/src/lib/auth/site-origin.ts`
  - `apps/web/src/lib/auth/auth-callback-outcome.ts`
  - `apps/web/src/app/auth/callback/route.ts`
  - `apps/web/src/components/auth/AuthPanel.tsx`
  - co-located unit/route/component tests and
    `apps/web/e2e/create-publish.spec.ts`

### Components, states, accessibility, and responsive behavior

- No approved copy, visual component, route, layout or responsive behavior changed.
  Existing busy, configuration-error and auth-error behavior remains in place.
- Unsafe return targets fall back silently to `/events`; they are not reflected in
  UI text, URLs after navigation or telemetry. Missing deployed origin configuration
  returns a generic server error rather than constructing a redirect from untrusted
  request data.
- Existing semantic labels, keyboard behavior, focus order, reduced-motion styling
  and desktop/mobile layouts are unchanged. The full browser suite rechecked existing
  accessibility assertions in both configured projects.

### Tests and verification evidence

- `npm run lint` — passed.
- `npm run typecheck` — passed.
- `npm run test` — passed, **6 files / 68 tests**. Coverage includes the allowed-path
  matrix, external/protocol-relative/backslash/control/encoded/double-encoded/dot-
  segment attacks, bounded input, exact-origin validation, callback request-origin
  poisoning, Google/email/password/reset integration, outcome privacy and telemetry
  fail-open behavior.
- `npm run build` — passed locally; a Vercel `preview`-profile build with a valid
  HTTPS site origin also passed. A negative preview-profile build without
  `NEXT_PUBLIC_SITE_URL` failed during `next.config.ts` loading with
  `WAMBE_SITE_ORIGIN_MISCONFIGURED`, as required.
- `npm run test:e2e` — passed, **14/14** Chromium and Pixel 7 project tests,
  including hostile auth return-target fallback and all existing create/publish,
  lifecycle, landing-page and accessibility regressions.
- `npm audit --audit-level=high` — passed with **0 vulnerabilities**.
- IDE diagnostics and `git diff --check` report no errors. Final architecture review
  found no remaining actionable High or Medium issue in the frontend hardening.

### Contract assumptions, performance, limitations, risks, and rollback notes

- **Traceability:** FR-004/AC-005 are implemented by US3-FE-01; the privacy/fail-open
  portion of FR-011/AC-011 is implemented by US3-FE-02; unchanged UI/API behavior and
  the full browser suite support AC-015. QA and Security own final acceptance/finding
  closure.
- **Contracts:** OpenAPI, product-event schema, backend endpoints, auth provider
  choices and user-facing routes are unchanged. `NEXT_PUBLIC_SITE_URL` is the sole
  deployed redirect origin; staging must configure it as the canonical exact HTTPS
  origin. `/auth/`, `/auth/callback`, public `/e/**` and arbitrary same-origin routes
  are intentionally not post-auth destinations.
- **Performance:** redirect evaluation is synchronous, bounded to 2 KiB/two decode
  passes and adds no request, dependency, cache or client bundle waterfall.
- **Limitations/downstream evidence:** no isolated Vercel/Supabase staging resources
  exist yet, so real Google/email/reset callback exchange, canonical-domain mismatch,
  Vercel log-drain delivery and auth-callback alerts remain for QA/Security/Operations.
  Structured logs are the current sink; alert wiring is not claimed. Security must
  independently retest SEC-004 before closing the finding.
- **Risk:** a misbound staging domain now fails closed and can interrupt sign-in rather
  than redirecting through an observed host. Deployment validation and the explicit
  build guard make that configuration error visible before traffic.
- **Rollback:** restore the prior Vercel artifact/revision. These changes add no data
  migration, backend contract or persistent client state, so rollback requires no
  cleanup or down migration.

## QA

### Acceptance-criteria traceability

1. **AC-001 — BLOCKED.** The required pre-implementation baseline exists at
   `a9e0f165bd229df555ceddce4189229994cae97a`, but all US-003 implementation and QA
   changes are still uncommitted. The configured `origin` is not resolvable by the
   active GitHub CLI account, so there is no CI result for an immutable release SHA.
2. **AC-002 — LOCAL PASS / STAGING BLOCKED.** API and scanner tests prove the 16 KiB
   fixed/chunked boundary, malformed/ambiguous length rejection, `413` before HMAC and
   a `limit + 1` maximum read. QA added scanner HTTP-filter-chain coverage. Repeated
   staging bursts with Cloud Run memory/instance evidence remain unexecuted.
3. **AC-003 — LOCAL PASS / STAGING BLOCKED.** Validator and deploy-script matrices
   reject absent/blank/short/default HMAC, deployed internal job keys, wrong profile
   and audience mismatch. Secret Manager startup and old/new rotation smoke require
   real staging.
4. **AC-004 — LOCAL PASS / STAGING BLOCKED.** QA added signed-JWT HTTP tests proving
   `role=authenticated` plus UUID subject reaches `/api/v1/events`, while `anon`,
   `service_role`, missing role and malformed/missing subject receive `401`. Exact CORS
   allow/deny also passes. Real Supabase tokens and cross-owner staging requests remain.
5. **AC-005 — LOCAL PASS / SECURITY RETEST BLOCKED.** Pure, callback-route, component
   and browser tests reject protocol-relative, backslash, encoded, control and
   origin-poisoning targets to `/events`. The browser test is demo-navigation evidence,
   not a real PKCE callback; canonical Vercel/Supabase retest remains mandatory.
6. **AC-006 — LOCAL PASS / RELEASE EVIDENCE BLOCKED.** OpenAPI, both Maven suites,
   generated client, frontend checks, Chromium/Pixel 7 tests and both Docker images
   build successfully; both npm audits report zero vulnerabilities. GitHub CI,
   full-history Gitleaks, NVD scans, published digests, SBOM and provenance are absent.
7. **AC-007 — BLOCKED.** No isolated Supabase, Resend, Vercel or API staging services
   exist for Google/email/reset/refresh/linking, real JWT, CORS and RLS journeys.
8. **AC-008 — BLOCKED.** Local callback, destination-policy and storage tests pass, but
   signed quarantine upload, live ClamAV/EICAR, IAM/internal ingress and real object
   reconciliation require staging.
9. **AC-009 — LOCAL PARTIAL / STAGING BLOCKED.** Demo desktop/mobile create, pin,
   publish, lifecycle and accessibility regression passes. Restricted Maps, visibility
   metadata and external unfurl behavior are not executable without the public domain.
10. **AC-010 — LOCAL PARTIAL / STAGING BLOCKED.** Flyway/Testcontainers, retention and
    missing-job-credential tests pass. Real Cloud Scheduler OIDC dispatch/retention and
    staging database/object outcomes remain.
11. **AC-011 — LOCAL PARTIAL / STAGING BLOCKED.** Bounded metric labels, Prometheus
    series naming, privacy-safe callback outcomes and telemetry fail-open unit behavior
    pass. No telemetry deployment, callback/quota alert, alert delivery or sampled
    staging log privacy evidence exists.
12. **AC-012 — BLOCKED.** API, scanner and Vercel rollback plus Supabase restore/PITR
    RPO/RTO drills require deployed revisions and a Product Owner decision if paid
    capability is unavailable.
13. **AC-013 — BLOCKED.** All 42 staging checklist controls remain unchecked and no
    schema-valid staging evidence instance or waiver set exists.
14. **AC-014 — PASS FOR QA EXECUTION.** QA used synthetic/local data and made no
    provider, production, paid or destructive change.
15. **AC-015 — LOCAL PASS / STAGING REGRESSION BLOCKED.** Contracts are unchanged and
    the full local create/publish/lifecycle/accessibility suite passes. Demo mode does
    not substitute for the eventual non-demo staging regression.

### Test matrix and execution evidence

- **Environment:** Windows 10; Java 21.0.8; Node 22.19.0; npm 10.9.3; Docker
  Desktop client/server 29.5.2; PostgreSQL 16 Testcontainers; synthetic test fixtures
  only. Frontend browser tests run with `NEXT_PUBLIC_DEMO_MODE=true`.
- `scripts/verify.ps1 -IncludeBrowser -IncludeImages` — **PASS**:
  - OpenAPI lint passed and the generated TypeScript client built;
  - `wambe-api` clean verify passed **55/55** tests;
  - `media-scanner` clean verify passed **27/27** tests;
  - frontend lint, typecheck, production build and **68/68** unit/route/component
    tests passed;
  - Playwright passed **14/14** tests across Chromium and Pixel 7 projects, including
    accessibility checks; and
  - local `wambe-api:verify` and `media-scanner:verify` Docker images built. These local
    manifests are build evidence, not release registry digests/SBOM evidence.
- QA added and executed:
  - `HostJwtHttpIntegrationTest` — signed host JWT matrix at the Spring Security HTTP
    boundary plus exact-origin CORS allow/deny; **3/3 passed**; and
  - `ScanHttpSecurityIntegrationTest` — declared/chunked oversize `413` before
    controller and valid-size missing-HMAC `401`; **3/3 passed**.
- `check-deploy-env.ps1 -Environment staging` — passed with temporary synthetic values.
  Negative fixtures correctly rejected a known scanner secret, a deployed
  `INTERNAL_JOB_KEY` and mismatched scanner audience; values were restored afterward.
- Vercel preview-profile build — valid exact HTTPS `NEXT_PUBLIC_SITE_URL` passed;
  missing origin failed during config load with `WAMBE_SITE_ORIGIN_MISCONFIGURED`.
- `npm audit --audit-level=high` for web and `npm audit --audit-level=low` for the
  generated client — **0 vulnerabilities** in both.
- `git diff --check` and diagnostics for the two QA tests passed.
- **Not executed and not reported as pass:** live-provider tests, Cloud Run burst/memory,
  HMAC rotation, real OAuth/EICAR/Maps/Scheduler, alert/rollback/restore drills,
  full-history Gitleaks, NVD-backed Java analysis, GitHub CI/release workflows and
  schema-valid staging evidence.

### Defects and regression risk

- **QA-001 — High / release blocker — no immutable release candidate or accessible CI
  evidence.**
  - **Reproduction:** `git rev-parse HEAD` returns baseline
    `a9e0f165bd229df555ceddce4189229994cae97a`; `git status -sb` shows `main` ahead by
    one with all US-003 implementation/QA changes modified or untracked;
    `origin` is `https://github.com/yabdev/wambe.git`; with active GitHub CLI account
    `yabdevTM`, both repository lookup and run lookup return `404`.
  - **Expected:** one committed release SHA containing approved backend, frontend and QA
    tests; an accessible remote; required CI green against that exact SHA.
  - **Actual:** only the pre-implementation baseline is committed, the release changes
    have no immutable SHA, and CI evidence cannot be retrieved.
  - **Affected:** AC-001, AC-006 and AC-013. **Disposition:** OPEN; do not publish or
    claim staging readiness until the Product Owner confirms the intended repository/
    account, changes are committed, and CI passes.
- No Critical/High functional defect was reproduced in the executable local product
  boundaries after adding HTTP JWT/CORS and scanner-envelope integration coverage.
- **Environment blockers, not passed tests:** no staging providers or public domain;
  no independent Security retest; no NVD key/security workflow; no internal VPC/IAM
  evidence; and no rollback/PITR capability decision.
- **Regression risks:** demo-mode browser tests can drift from real auth/API behavior;
  callback tests do not perform a live PKCE exchange; callback/quota alerts are not
  implemented/tested; local callback fixtures do not exercise live ClamAV; and a free
  Supabase plan may block restore evidence.
- SEC-005–SEC-010 remain residual findings unless independently remediated or explicitly
  waived; QA has not inferred their closure from green local tests.

### Release recommendation

**Recommendation: LOCAL QUALITY GATES PASS; DO NOT DECLARE STAGING READY.**

The implementation is suitable to hand to independent Security review: contracts,
unit/integration boundaries, signed host JWT/CORS behavior, scanner envelope ordering,
frontend redirect hardening, browser regressions and local images all pass. Approval of
this QA report accepts the accuracy of that evidence; it does **not** approve a release,
close SEC findings or waive any staging control.

Before a staging-ready recommendation:

1. resolve QA-001, commit one release candidate and obtain accessible CI on that SHA;
2. run full-history Gitleaks and NVD-backed dependency checks, then publish digest-
   addressed images with retained SBOM/provenance;
3. provision isolated lowest-cost staging without production or paid changes unless the
   Product Owner explicitly authorizes them;
4. execute the 42-item staging checklist, including real auth/RLS, EICAR, Maps/metadata,
   Scheduler/retention, observability, rollback and restore;
5. have Security independently retest SEC-001–SEC-004 and SEC-008; and
6. produce schema-valid redacted staging evidence with every release-critical control
   `PASS` or explicitly `WAIVED` by the Product Owner.

## Security

### Scope and threat scenarios

**Review date:** 2026-07-15

**Reviewer:** Security Engineer persona, independent of implementation

**Authorization boundary:** local source, tests, generated local SBOMs and local Docker
images only. No external penetration test was attempted because no Wambe staging
environment or other explicitly authorized remote target exists.

The review covered the approved US-003 implementation and its inherited US-002
boundaries: SEC-001 through SEC-004 and SEC-008, authentication and authorization,
request validation, internal-service authentication, upload destinations, secrets,
privacy, cryptography, dependencies, configuration, browser controls, rate limiting,
audit/telemetry behavior and deployment exposure. It does not treat approval of this
report as staging or production authorization.

**Data classification**

- Supabase access/refresh tokens, service-role credentials, scanner HMAC values,
  Scheduler identity and signed object URLs are restricted secrets and must not appear
  in source, evidence, logs or metrics.
- Host identity, event titles, venue/address details, filenames and unpublished media
  are confidential tenant data. Staging may contain synthetic equivalents only.
- Public event metadata is intentionally public only for the approved lifecycle and
  visibility combinations; invite-only, hidden-location, unpublished and deleted
  content remain protected.
- Security evidence is internal operational metadata. It may contain commit/digest,
  route, bounded result/reason and safe request identifiers, but not request bodies,
  personal content or credentials.

**Trust boundaries and attack surface**

1. browser/anonymous client → Vercel Next.js routes and Supabase Auth;
2. authenticated host → public Cloud Run API → owner predicates and PostgreSQL RLS;
3. API → internal Cloud Run scanner → Cloud Run IAM plus request HMAC;
4. scanner → exact Supabase storage origin and exact API callback;
5. Scheduler → internal job endpoints using Google OIDC;
6. CI/operator → GitHub, Secret Manager and provider control planes; and
7. explicit local/test profiles, which are a lower-trust development boundary and must
   never become a deployed configuration.

Threat actors include anonymous availability/cost attackers, malicious or compromised
hosts attempting cross-tenant access, an attacker with a leaked internal secret, a
malicious upload, an OAuth phishing attacker, a supply-chain attacker, and an operator
or deployment mistake that exposes a development or internal boundary. Reviewed abuse
cases included oversized/chunked bodies, ambiguous lengths, forged/replayed HMAC input,
non-host JWT roles, malformed subjects, external/encoded return targets, scanner SSRF
and redirects, secret/default-profile drift, sensitive telemetry, vulnerable
dependencies and public scanner invocation.

### Checks and evidence

**Independent executable evidence**

- API targeted retest:
  `mvnw.cmd -B -Dtest=RequestEnvelopeFilterTest,ScannerHmacFilterTest,`
  `DeployedSecurityValidatorTest,HostJwtClaimValidatorTest,HostJwtDecoderTest,`
  `HostJwtHttpIntegrationTest,ScannerIdentityTokenConfigurationTest,`
  `HttpScannerDispatchAdapterTest,SecurityIntegrationTest test` — **PASS, 34/34**.
  This exercised fixed/chunked envelope behavior, HMAC ordering, deployed-profile
  startup, signed JWT role/subject rejection, exact CORS, identity-header construction
  and the existing authorization boundary.
- Scanner targeted retest:
  `mvnw.cmd -B -Dtest=RequestEnvelopeFilterTest,DispatchHmacFilterTest,`
  `DeployedSecurityValidatorTest,ScannerDestinationPolicyTest,`
  `ScanServiceDestinationTest,ScanHttpSecurityIntegrationTest test` — **PASS, 26/26**.
  It covered early fixed/chunked `413`, missing-HMAC `401`, startup fail-fast,
  scheme/host/port/path rejection, redirect non-follow and no-second-destination
  behavior.
- Frontend authentication retest with one Vitest worker — **PASS, 5 files / 65 tests**.
  It covered safe-path parsing, origin configuration, callback outcomes, route handling,
  OAuth/email/password/reset integration and hostile return targets.
- `npm audit --audit-level=low` — **PASS, 0 known vulnerabilities** for both
  `apps/web` and `packages/wambe-api-client`.
- Gitleaks **v8.30.1**, image
  `sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f`:
  full Git history scanned two commits with **no leaks**. A separate redacted worktree
  scan found twelve ignored generated `.next` artifacts plus one source finding in a
  synthetic scanner-secret test fixture; that source finding is SEC-014 and the
  worktree scan is therefore **FAIL**, not a clean-tree result.
- CycloneDX Maven plugin 2.9.1 produced runtime SBOMs. OSV-Scanner **v2.4.0**, image
  `sha256:5116601dedc01c1c580eb92371883ec052fc4c13c3fbc109d621a63ac416d475`,
  scanned 65 scanner packages with **no issue** and 157 API packages with **one Medium
  advisory**, SEC-011. Direct recursive POM resolution was inconclusive after Maven
  Central `429` responses and versionless BOM entries, so it is not counted as a pass.
- Static review found no `eval`, `dangerouslySetInnerHTML`, direct `innerHTML`,
  `document.write`, process execution, permissive credentialed CORS or dynamic SQL
  construction. The one native query remains fixed SQL with a bound RLS owner value.
- New telemetry uses fixed event/outcome/reason values and catches sink failure. New
  startup exceptions expose stable codes only; destination rejection does not echo a
  URL, signed query or body.

**Controls confirmed by review**

- Both internal JSON endpoints cap the body at 16,384 bytes before HMAC and business
  processing. Fixed lengths are validated and chunked reads use `readNBytes(limit + 1)`.
  Cached wrappers only clone the bounded byte array.
- Non-dev contexts reject blank, short and known scanner secrets; API rejects any
  deployed local job key; mixed deployed/dev contexts cannot bypass those checks.
- Host JWT validation composes issuer, signature/temporal, audience, exact
  `role=authenticated` and UUID-subject checks and returns neutral unauthorized results.
- OAuth navigation is bounded, exact-origin and path-allowlisted at initiation,
  callback, proxy and local navigation boundaries; unsafe input is not logged.
- Scanner destinations require the configured host/origin and path families, reject
  userinfo, IP literals and non-default deployed ports, and use redirect-disabled
  clients.
- HMAC-SHA-256 comparisons remain constant-time, timestamps remain bounded and scanner
  callbacks preserve the existing nonce/idempotent terminal-state controls.

**Incomplete or blocked evidence**

- No NVD API key is configured, so the required OWASP Dependency-Check run is **not
  executed**. The OSV result supplements but does not replace the approved NVD check.
- GitHub Actions cannot be resolved for the configured remote/account, and the release
  changes still have no immutable commit. CI and post-commit history scanning are
  unavailable.
- Cloud Run IAM/internal routing, Secret Manager rotation, real Google ID tokens,
  Supabase tokens/RLS/storage, OAuth/email reset, EICAR, Scheduler OIDC, observability,
  sensitive-log review, rollback and restore remain unexecuted staging controls.
- Rate limiting, browser CSP/security headers and the inherited database job role remain
  unchanged residual findings. No remote dynamic/fuzz test was authorized or possible.

### Findings and remediation

#### SEC-001 — High — REMEDIATED — bounded pre-authentication request handling

- **Likelihood/impact:** before remediation, reachable oversized requests had medium to
  high likelihood and high availability impact through proportional heap pressure.
- **Evidence and affected component:** API/scanner envelope filters now run before HMAC,
  bound fixed and chunked bodies to 16 KiB and return minimal `413`; 34 API and 26
  scanner security tests include filter-order and HTTP-boundary cases.
- **Remediation:** implemented as designed; retain the exact limit, filter ordering,
  internal scanner ingress, concurrency cap and rejection telemetry.
- **Retest status:** **implementation finding closed** by independent local review.
  Cloud Run burst/memory and effective internal-ingress evidence remain Operations
  checklist conditions and are not represented as passed.

#### SEC-002 — High — REMEDIATED — deployed secret fail-fast

- **Likelihood/impact:** the former omitted-secret path had medium likelihood and high
  integrity impact because a public default could authenticate scanner traffic.
- **Evidence and affected component:** both services moved known values to local/test
  profiles; deployed validators reject missing, blank, short and known values, API
  rejects deployed `INTERNAL_JOB_KEY`, and mixed-profile tests pass.
- **Remediation:** implemented as designed; generate and inject a provider-managed
  high-entropy secret, rotate both revisions together and never restore a local job key.
- **Retest status:** **implementation finding closed** by the startup matrix. Actual
  Secret Manager injection, rotation and only-new-secret smoke remain unexecuted
  staging evidence.

#### SEC-003 — Medium — REMEDIATED — host JWT role and subject boundary

- **Likelihood/impact:** exploitation required another accepted Supabase token class;
  impact would be high host-route authorization bypass.
- **Evidence and affected component:** `HostJwtClaimValidator` requires exact
  `authenticated` role and UUID subject; signed HTTP fixtures accept that role and
  reject missing, `anon`, `service_role` and malformed/missing subjects.
- **Remediation:** implemented as designed; preserve issuer/audience/signature/temporal
  validation and neutral `401` behavior.
- **Retest status:** **implementation finding closed**. Real staging Supabase token,
  refresh and cross-owner/RLS tests remain checklist evidence.

#### SEC-004 — Medium — REMEDIATED — OAuth callback open redirect

- **Likelihood/impact:** a crafted login link previously had medium phishing likelihood
  and medium post-authentication redirect impact.
- **Evidence and affected component:** the shared helper and authoritative callback
  reject protocol-relative, external, backslash, control, encoded-separator, dot-segment
  and oversized values; 65 focused frontend tests pass.
- **Remediation:** implemented as designed; keep the canonical HTTPS origin deployment
  guard and use the shared helper at every auth navigation boundary.
- **Retest status:** **implementation finding closed**. A real provider PKCE/Google/email
  callback on the canonical staging domain remains an Operations checklist condition.

#### SEC-008 — Medium — REMEDIATED — scanner destination policy

- **Likelihood/impact:** a compromised sender or secret formerly had medium combined
  likelihood and medium SSRF/arbitrary-callback impact.
- **Evidence and affected component:** `ScannerDestinationPolicy` validates all three
  URLs before any request and both scanner clients reject redirects; hostile
  scheme/host/port/path and redirect tests pass.
- **Remediation:** implemented as designed; retain exact configuration-derived origins,
  redirect-disabled clients and restrictive Cloud Run egress/IAM.
- **Retest status:** **implementation finding closed**. Live Supabase signed URLs,
  callback origin, DNS/egress and provider behavior remain staging evidence.

#### SEC-011 — Medium — OPEN — API SBOM contains CVE-2026-54515

- **Likelihood:** low in the reviewed code. The flaw requires case-insensitive property
  handling combined with per-property `@JsonIgnoreProperties`; repository/generated
  source searches found no `ACCEPT_CASE_INSENSITIVE_PROPERTIES` or that annotation
  combination.
- **Impact:** medium integrity impact if a future model adds the prerequisite pattern:
  ignored fields can become writable through mass assignment. Upstream CVSS is 5.3.
- **Evidence:** the API SBOM resolves
  `com.fasterxml.jackson.core:jackson-databind:2.21.4` through
  `org.openapitools:jackson-databind-nullable:0.2.8`. OSV reports
  `GHSA-5jmj-h7xm-6q6v` / `CVE-2026-54515`; Maven Central publishes patched 2.21.5.
- **Affected component:** `services/wambe-api` JSON binding dependency graph.
- **Remediation:** align the Jackson 2 dependency set on patched 2.21.5 or a compatible
  later Spring/OpenAPI dependency release; rerun full API tests, SBOM/OSV and the
  required NVD-backed scan.
- **Retest status:** **OPEN**; no dependency change was made during Security review.

#### SEC-012 — Medium — OPEN — scanner IAM/ID-token invariant is not deployed or proven

- **Likelihood:** medium configuration risk until an effective staging IAM policy
  exists. Application HMAC still protects the endpoint, but the scanner does not and is
  not expected to independently validate `X-Serverless-Authorization`; Cloud Run is the
  Google ID-token enforcement boundary.
- **Impact:** high if the scanner is accidentally granted public/broad invocation and
  the HMAC is later disclosed, because `/scan` would lose the intended independent IAM
  control.
- **Evidence:** API dispatch adds an exact-audience Google ID token, and the service
  template specifies internal ingress, but no staging service/IAM policy exists and no
  negative unauthenticated invocation has run. Documentation alone is not enforcement.
- **Affected component:** Cloud Run `media-scanner` ingress and IAM policy plus the
  API service account invoker binding.
- **Remediation:** codify and verify no `allUsers` invoker, scanner-only
  `roles/run.invoker` for the exact API service account, internal routing and
  exact-audience acceptance; record redacted `gcloud` policy and positive/negative call
  evidence. Do not make the scanner public as a routing workaround.
- **Retest status:** **BLOCKED** until isolated GCP staging exists.

#### SEC-013 — Low — OPEN — API runtime does not forbid deployed local storage

- **Likelihood:** low because the pre-deploy script requires an exact deployed profile
  and `STORAGE_TYPE=supabase`; exploitation requires bypassing that gate or activating a
  mixed local/deployed profile.
- **Impact:** medium confidentiality/integrity impact if the local profile and local
  adapter/controller are activated in a reachable environment, because
  `/dev-storage/**` is permit-all.
- **Evidence:** base API configuration defaults storage to `local`,
  `LocalStorageController` activates for `local` or `test`, and
  `DeployedSecurityValidator` does not inspect storage type or reject a dev profile
  paired with a deployed profile. Static deployment validation catches the approved
  path but runtime startup does not. This extends, and does not replace, SEC-009.
- **Affected component:** API profile/storage configuration and host security chain.
- **Remediation:** make deployed startup require Supabase storage and reject any active
  local/test profile in a deployed context; additionally scope the permit-all matcher
  to an actually active dev controller.
- **Retest status:** **OPEN**; no runtime guard test exists.

#### SEC-014 — Low security risk / release blocker — OPEN — synthetic fixture trips Gitleaks

- **Likelihood:** certain after the current tests are committed and the required
  Gitleaks workflow scans them.
- **Impact:** low confidentiality impact because the value is synthetic, but high
  release-process impact: Security CI fails and a broad suppression could hide a future
  real secret.
- **Evidence:** redacted Gitleaks worktree scan reports the generic API-key rule at
  `services/media-scanner/src/test/java/com/wambe/scanner/`
  `DeployedSecurityValidatorTest.java:83`. The same synthetic literal is also present in
  the API validator test, although this run reported only the scanner context. Ignored
  `.next`/`target` findings are generated and will not be committed.
- **Affected component:** deployed-secret tests and the GitHub `Security` workflow.
- **Remediation:** construct the non-secret test value at runtime in both services, or
  use a narrowly scoped reviewed false-positive disposition; never blanket-ignore the
  rule/path. Commit, then rerun Gitleaks against full history and the release SHA.
- **Retest status:** **OPEN**; current source scan fails.

**Carried findings outside the selected remediation scope**

- SEC-005 (Medium, API abuse/cost controls), SEC-006 (Medium, dormant broad database job
  role), SEC-007 (Medium, browser security headers) and SEC-009 (Medium, local storage
  exposure) remain **OPEN** with the likelihood, impact, evidence and remediation
  recorded in `../US-002-create-publish-event/ARTIFACTS.md#security`.
- SEC-010 (Low, deleted-slug existence disclosure) remains **OPEN** at the same source.
- Passing US-003's targeted controls does not waive or close these findings.

### Residual risk

**Critical findings:** 0 open.

**High findings:** 0 open.

**Medium findings:** 6 open — SEC-005, SEC-006, SEC-007, SEC-009, SEC-011 and SEC-012.

**Low findings:** 3 open — SEC-010, SEC-013 and SEC-014.

**Security recommendation: IMPLEMENTATION REMEDIATIONS VERIFIED; STAGING AND PRODUCTION
NO-GO.**

The original High findings SEC-001 and SEC-002 and the selected Medium findings SEC-003,
SEC-004 and SEC-008 are closed at the implementation boundary with independent local
evidence. No Critical or High implementation risk remains in the selected scope.

Staging readiness must not be claimed because SEC-014 will block post-commit secret
scanning; QA-001 still prevents immutable CI evidence; NVD-backed analysis is absent;
SEC-012 and every provider boundary are unverified; and the staging checklist remains
unexecuted. Operations must not deploy production or weaken scanner ingress to work
around private routing. Any paid control, unavailable release-critical control or risk
waiver returns to the Product Owner.

## Operations

**Review date:** 2026-07-16

**Persona:** DevOps / SRE

**Authorization boundary:** local source, workflows, delivery templates, documentation
and synthetic configuration only. No production action was authorized. No staging cloud
resource, provider account, DNS record, secret, paid plan or destructive operation was
created or changed.

**Readiness recommendation: STAGING AND PRODUCTION NO-GO.** Local delivery gates and
Operations-owned automation are ready for an immutable release candidate, but there is
no accessible repository/release SHA, green remote workflow, published artifact or
isolated staging environment. All 42 staging controls remain `NOT_RUN`.

### CI/CD and environment readiness

**Operations-owned delivery hardening**

- `.github/workflows/release-images.yml` is now staging-only. It requires successful
  `CI` and `Security` runs for the exact selected SHA before WIF authentication or image
  publication.
- The workflow performs one canonical deployment build, publishes SHA-tagged API and
  scanner images with OCI source/revision labels, SBOM and maximum provenance, validates
  both returned digests, and retains redacted digest/build metadata for 90 days.
- CI image jobs remain Dockerfile/build-definition checks. Staging must deploy and test
  the canonical release digests; a future production promotion must reuse those digests
  without rebuilding.
- Stable IDs were assigned to all 42 checklist controls. A schema-valid, all-`NOT_RUN`
  `STAGING_EVIDENCE.example.json` and mapping check were added. The example's zero SHA
  is deliberately not release evidence.
- CI now validates the evidence template against the v1 schema and verifies a one-to-one,
  duplicate-free checklist mapping.
- The operations handbook now distinguishes local implementation evidence, canonical
  release artifacts, deployed evidence and readiness. It documents the mandatory
  exact-SHA manual Security dispatch and registry attestation evidence.

**Release baseline and access audit**

- `main` remains at
  `a9e0f165bd229df555ceddce4189229994cae97a`, ahead of `origin/main` by one commit,
  with all US-003 implementation, QA, Security and Operations work still modified or
  untracked. There is no immutable US-003 release candidate.
- GitHub CLI authentication succeeds as `yabdevTM`, but the configured
  `https://github.com/yabdev/wambe.git` returns no repository access and
  `yabdevTM/wambe` does not exist. CI, Security, environment protection, WIF publication
  and workflow evidence therefore cannot run.
- `gcloud`, `vercel` and `supabase` CLIs are unavailable and no matching provider MCP
  integration exists. No authenticated staging control plane was available.
- Docker, Java, Node and npm are available locally. Local access does not substitute for
  provider IAM, Secret Manager, registry or deployment evidence.

**Executable local evidence**

- `scripts/verify.ps1 -IncludeImages` with the existing web install completed
  successfully: OpenAPI lint; API **55/55** tests; scanner **27/27** tests; generated
  client build; web lint/typecheck/build and **68/68** tests; and both local container
  images. The approved QA browser/accessibility result remains **14/14**; Operations did
  not represent it as a new staging run.
- The first scanner-image attempt encountered a transient Docker Hub metadata timeout;
  the isolated retry and the subsequent complete verification run both passed.
- `actionlint` 1.7.7 accepted all GitHub workflows. `promtool` 3.5.0 accepted all
  **10** Prometheus rules. YAML lint accepted both Cloud Run templates.
- AJV validation accepted the evidence example; the mapping script reports exactly
  **42** unique checklist controls. The script accepts checked or unchecked Markdown
  boxes without changing the evidence IDs.
- `check-deploy-env.ps1 -Environment staging` passed with ephemeral synthetic values
  that were restored immediately. This proves static validation only, not Secret Manager
  delivery or runtime startup.
- `git diff --check` and diagnostics for the Operations-owned files passed.

**Supply-chain disposition**

- QA-001 remains open: there is no commit containing US-003 and no accessible remote CI.
- SEC-014 remains a hard release blocker because the synthetic secret fixture will fail
  full-history Gitleaks after commit.
- SEC-011 remains open until Jackson is upgraded to the fixed line and API tests, SBOM,
  OSV and NVD-backed analysis pass.
- SEC-013 remains open because API runtime defence does not independently reject local
  storage or mixed development/deployed profiles.
- `NVD_API_KEY` is absent, so `Security` cannot produce required Java dependency
  evidence.
- SEC-012 remains deployment-blocked: scanner internal ingress, no-public-invoker
  policy, exact API-service-account invoker and exact-audience Google ID token have not
  been proven.

**Staging checklist disposition**

| Checklist area | Controls | PASS / WAIVED | Current disposition |
|---|---:|---:|---|
| Build and supply chain | 5 | 0 | `NOT_RUN`; no release SHA/workflows/digests |
| Security blockers | 5 | 0 | `NOT_RUN`; local closure is not deployed evidence |
| Authentication and authorization | 6 | 0 | `NOT_RUN`; providers absent |
| Storage and malware | 7 | 0 | `NOT_RUN`; no Supabase, live ClamAV or IAM path |
| Maps, metadata and sharing | 5 | 0 | `NOT_RUN`; no public staging origin |
| Jobs, migrations and recovery | 7 | 0 | `NOT_RUN`; no database/jobs/revisions/restore |
| Observability and support | 7 | 0 | `NOT_RUN`; no telemetry or alert delivery |
| **Total** | **42** | **0** | **Not staging-ready** |

### Deployment and rollback

- No deployment was attempted. The approved rollout is blocked before its first
  release-candidate step, so applying Cloud Run templates or provisioning downstream
  services would break evidence ordering.
- Cloud Run YAML parses successfully and encodes digest-addressed images, scale-to-zero,
  bounded maximum scale, scanner concurrency one, internal scanner ingress, separate
  service accounts, Secret Manager references and API Direct VPC egress. The manifests
  do not themselves apply IAM; effective invoker policies and absence of `allUsers` must
  be proven from the deployed scanner.
- The lowest-cost supported Direct VPC path and its region/quota/cost are unknown.
  Operations will not introduce a paid connector/load balancer or make the scanner
  public as a workaround.
- Once prerequisites pass, the sequence remains: publish canonical digests; provision
  isolated synthetic-only Supabase/GCP/Vercel/Resend/Maps/telemetry; deploy scanner
  internally; prove EICAR and clean files; deploy API at zero host traffic; run Flyway;
  deploy web with demo mode off; configure jobs/telemetry; then execute all checklist,
  rollback and restore evidence.
- Rollback is documented but unexecuted: shift scanner/API independently to prior Cloud
  Run revisions, restore the prior Vercel deployment, never down-migrate Flyway, rerun
  scan dispatch/retention, and smoke health/auth/publish/metadata/clean upload.
- No Supabase backup/PITR capability or measured RPO/RTO exists. If the selected free
  plan cannot perform the restore drill, Operations must record a blocker and obtain a
  focused Product Owner purchase or waiver decision.
- Capacity remains an unverified pilot assumption. Templates use API min/max scale
  `0/10`, concurrency `40`, 1 CPU/1 GiB and scanner min/max scale `0/3`, concurrency
  `1`, 2 CPU/2 GiB. With the API's default pool size `10`, the theoretical application
  connection ceiling is 100 before migration/administrative connections; actual
  database quotas must be known before selecting staging scale and pool limits.
- No monetary estimate is claimed because region, provider plans, quotas and traffic are
  unknown. Scale-to-zero and bounded scanner concurrency are cost controls, not proof of
  free operation.

### Monitoring, alerts, and runbooks

- Prometheus syntax validation confirms 10 backend/scanner alert rules covering API and
  scanner availability, publication errors, API latency, scan age/rejection,
  retention failure/miss and security-boundary/destination rejection.
- No Prometheus/logging deployment, scrape target, dashboard, alert route or delivery
  test exists. Auth-callback and provider-quota alerts remain absent, so AC-011 and
  `OPS-001` through `OPS-004` cannot pass.
- No deployed log sample proves exclusion of tokens, signed URLs, event titles,
  addresses, filenames or request bodies. No trace pipeline or storage-retention
  evidence exists.
- Pilot objectives remain unmeasured: API availability 99.5%, publish and auth callback
  success at least 95%, scan p95 age below 10 minutes, daily retention success and API
  p95 below two seconds. They are initial objectives, not contractual commitments.
- `docs/operations/README.md` remains the runbook source for API latency/outage,
  publication errors, scanner backlog/security anomalies, auth callback failure and
  retention failure. Alert `runbook` labels are stable slugs, but live routing and
  operator access remain to be configured.
- Telemetry must fail open. Draft save/publication behavior during an actual telemetry
  outage remains a staging test, not an inferred pass from local code.

### Support ownership and post-release checks

- The founder/Product Owner is primary staging operator, release authority and initial
  incident commander. No backup operator is named; this does not prevent staging
  execution but blocks production readiness.
- On-call contacts, provider recovery access and customer communication channels must be
  stored outside the public repository. None was created during this review.
- Severity 1 isolation/malware/credential/destructive-retention events halt rollout and
  escalate immediately. Severity 2 sustained publish/auth/scan failures begin rollback.
  Support must capture only environment, time, route, safe error code and request ID.
- Post-release verification cannot begin without deployed revisions. Required checks
  remain health, real auth/refresh/CORS/RLS, lean publish, media clean/EICAR paths,
  metadata redaction, Scheduler/retention, alert delivery, telemetry fail-open,
  rollback, restore and deleted-content reconciliation.

**Product Owner actions required to resume staging execution**

1. Choose the intended GitHub owner/repository and visibility, then explicitly authorize
   the release-candidate commit/push so QA-001 can be resolved.
2. Reopen backend implementation for SEC-011, SEC-013 and SEC-014 remediation, then
   repeat QA/Security evidence as required.
3. Configure the free NVD key and require exact-SHA `CI` and `Security` success.
4. Install/authenticate the provider CLIs or provide another approved authenticated
   staging control plane; no credentials should be pasted into chat.
5. Decide only when evidence requires it whether to fund VPC routing, Supabase
   restore/PITR, telemetry hosting or another unavailable release-critical control.

**Operations conclusion:** local delivery automation is improved and validated, but the
story goal is not met. Approval of this Operations report would accept the recorded
no-go assessment; it would not declare staging ready, waive any control or authorize
production.

## Final Product Owner Notes

On 2026-07-16, the Product Owner entered
`/sdlc-orchestrator APPROVE US-003 stage: done`. All required SDLC gates are approved,
the implementation profile is recorded as full-stack/backend-first, acceptance criteria
are mapped to QA evidence, relevant local checks and disclosed gaps are recorded, no
Critical or High security finding remains open, and delivery, rollback, monitoring,
support and known limitations are documented. Story US-003 is therefore **DONE**.

This decision closes the story workflow; it does not declare staging ready, waive a
release control or authorize production. QA-001, SEC-011 through SEC-014, unavailable
provider resources/NVD evidence, and every unexecuted staging control remain recorded.
The checklist is **0/42 PASS or WAIVED**. Staging or production work must first resolve
those blockers through an explicitly authorized follow-up or reopened stage.
