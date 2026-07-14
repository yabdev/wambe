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

### API, data, migration, and compatibility notes

### Tests and verification evidence

### Observability, limitations, risks, and rollback notes

## Implementation — Frontend

### Changed files and completed frontend/integration slices

### Components, states, accessibility, and responsive behavior

### Tests and verification evidence

### Contract assumptions, performance, limitations, risks, and rollback notes

## QA

### Acceptance-criteria traceability

### Test matrix and execution evidence

### Defects and regression risk

### Release recommendation

## Security

### Scope and threat scenarios

### Checks and evidence

### Findings and remediation

### Residual risk

## Operations

### CI/CD and environment readiness

### Deployment and rollback

### Monitoring, alerts, and runbooks

### Support ownership and post-release checks

## Final Product Owner Notes
