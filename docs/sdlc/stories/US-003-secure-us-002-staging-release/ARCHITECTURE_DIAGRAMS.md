# US-003 Architecture Diagrams

Story: **Secure US-002 staging release**  
Architecture status: **Approved by the Product Owner on 2026-07-14**  
Inherited product contracts:
[`openapi-v1.yaml`](../US-002-create-publish-event/contracts/openapi-v1.yaml) and
[`product-events-v1.schema.json`](../US-002-create-publish-event/contracts/product-events-v1.schema.json)  
New operational contract: [`STAGING_EVIDENCE_SCHEMA.json`](STAGING_EVIDENCE_SCHEMA.json)

These diagrams describe the US-003 security and staging deltas. The Architecture section
of `ARTIFACTS.md` and the versioned contracts remain authoritative.

## 1. Scope and system context

```mermaid
flowchart LR
    Host["Wambe host<br/>synthetic staging identity"]
    Operator["Founder / operator<br/>staging and evidence"]
    PO["Product Owner<br/>cost, waiver and release authority"]
    Wambe["Existing Wambe platform<br/>Next.js + Spring Boot"]
    Providers["Isolated staging providers<br/>Vercel · GCP · Supabase · Resend · Maps"]
    Evidence["Redacted release evidence<br/>commit · digests · test results"]

    Host -->|"Existing auth/create/publish journeys"| Wambe
    Operator -->|"Authenticated CLI and provider consoles"| Providers
    Operator -->|"Deploys immutable staging artifacts"| Wambe
    Wambe --> Providers
    Wambe -->|"Safe identifiers and outcomes only"| Evidence
    Operator -->|"Requests decisions for paid/destructive/waived work"| PO
    Evidence -->|"Staging-readiness review"| PO
```

**Boundary:** US-003 changes security controls and staging operations. It adds no host
feature, public API version, product database table, production deployment or custom
operator dashboard.

## 2. Isolated staging deployment and trust boundaries

```mermaid
flowchart TB
    subgraph Internet["Public internet"]
        Browser["Host browser<br/>synthetic accounts only"]
        Crawler["External preview crawler"]
    end

    subgraph GitHub["GitHub"]
        Actions["CI + Security + release workflows"]
        WIF["Workload Identity Federation"]
    end

    subgraph Vercel["Vercel staging project"]
        Web["Next.js<br/>demo mode off"]
    end

    subgraph GCP["Dedicated GCP staging project"]
        API["Cloud Run: wambe-api<br/>public HTTPS API"]
        Scanner["Cloud Run: media-scanner<br/>internal ingress · concurrency 1"]
        PrivateRoute["Same-project private route<br/>Direct VPC egress or approved alternative"]
        Scheduler["Cloud Scheduler<br/>dedicated OIDC identity"]
        Secrets["Secret Manager<br/>rotated staging values"]
        Registry["Artifact Registry<br/>digest-addressed images"]
    end

    subgraph Supabase["Dedicated Supabase staging project"]
        Auth["Auth + JWKS<br/>Google + verified email/password"]
        DB[("PostgreSQL<br/>Flyway + RLS")]
        Quarantine[("Private quarantine bucket")]
        Active[("Private active bucket")]
    end

    subgraph External["Staging-only external configuration"]
        Resend["Resend SMTP"]
        Maps["Google Maps<br/>origin-restricted key"]
        Telemetry["OTel / Grafana / Umami<br/>fail-open"]
    end

    Browser --> Web
    Crawler --> Web
    Web --> Auth
    Web -->|"Bearer JWT<br/>role=authenticated"| API
    Web --> Maps
    API --> DB
    API --> Quarantine
    API --> Active
    API -->|"Google ID token + HMAC"| PrivateRoute
    PrivateRoute -->|"Bounded JSON"| Scanner
    Scanner -->|"Allowlisted signed read / preview write"| Quarantine
    Scanner -->|"Exact callback URL<br/>HMAC + bounded JSON"| API
    Scheduler -->|"OIDC exact subject + audience"| API
    Secrets -. "runtime injection" .-> API
    Secrets -. "runtime injection" .-> Scanner
    API -. "non-blocking telemetry" .-> Telemetry
    Auth --> Resend
    Actions --> WIF --> Registry
    Registry --> API
    Registry --> Scanner
```

**Separation:** staging and production share no project, database, bucket, OAuth
credential, HMAC secret, service account or real host data. The scanner has no public
invoker: the API service account has scanner-only `roles/run.invoker`, obtains a Google
ID token for the exact scanner audience and sends it separately from the payload HMAC.

## 3. SEC-001 bounded request envelope

```mermaid
sequenceDiagram
    autonumber
    participant Caller
    participant Limit as Path-scoped envelope filter
    participant HMAC as HMAC filter
    participant JSON as JSON validation
    participant Handler

    Caller->>Limit: POST callback or /scan
    alt fixed-body length absent/invalid or greater than 16 KiB
        Limit-->>Caller: 413 minimal response
    else valid chunked or fixed length within limit
        Limit->>Limit: Read at most 16 KiB + 1
        alt stream exceeds 16 KiB
            Limit-->>Caller: 413 minimal response
        else bounded body
            Limit->>HMAC: Replayable bounded bytes
            alt signature headers/timestamp/HMAC invalid
                HMAC-->>Caller: 401 neutral response
            else authenticated
                HMAC->>JSON: Parse strict JSON + Bean Validation
                alt malformed or invalid
                    JSON-->>Caller: Existing validation response
                else valid
                    JSON->>Handler: Process request
                    Handler-->>Caller: Existing success response
                end
            end
        end
    end
```

The filter checks `Content-Length` as a fast path but never trusts it as the only limit.
A fixed body without a valid length receives `413`; a valid chunked body uses the bounded
read and receives `413` when over limit. Neither service calls `readAllBytes()` on an
attacker-controlled raw stream.

## 4. SEC-002 deployed-secret startup gate

```mermaid
flowchart TD
    A["Application starts"] --> B{"Active profile local or test?"}
    B -->|"Yes"| C["Explicit development defaults allowed"]
    B -->|"No: staging or production"| D{"SCANNER_HMAC_SECRET present?"}
    D -->|"No / blank"| X["Fail startup<br/>no listening service"]
    D -->|"Yes"| E{"At least 32 characters<br/>and not a known default?"}
    E -->|"No"| X
    E -->|"Yes"| F{"API INTERNAL_JOB_KEY empty?"}
    F -->|"No"| X
    F -->|"Yes / scanner N/A"| G["Bind security filters"]
    G --> H["Health probe may become ready"]
```

Known local defaults move to explicit `local`/`test` configuration. Secret Manager
injects the same rotated HMAC into API and scanner; logs and health output never reveal
the value.

## 5. SEC-003 host JWT decision

```mermaid
flowchart LR
    A["Bearer token"] --> B{"Signature, issuer and expiry valid?"}
    B -->|"No"| Z["401 neutral"]
    B -->|"Yes"| C{"aud contains authenticated?"}
    C -->|"No"| Z
    C -->|"Yes"| D{"role exactly authenticated?"}
    D -->|"Missing / anon / service_role / other"| Z
    D -->|"Yes"| E{"sub is UUID?"}
    E -->|"No"| Z
    E -->|"Yes"| F["Authenticated host principal"]
    F --> G["Owner predicate + transaction-local RLS"]
```

The claim policy applies only to host routes. Scheduler OIDC and scanner HMAC retain
their separate security chains and identities.

## 6. SEC-004 OAuth return-target decision

```mermaid
flowchart TD
    A["Raw next value"] --> B{"Present and non-empty?"}
    B -->|"No"| Safe["Use /events"]
    B -->|"Yes"| C{"Starts with exactly one /?"}
    C -->|"No"| Safe
    C -->|"Yes"| D{"Contains backslash, control character<br/>or encoded separator after bounded decode?"}
    D -->|"Yes"| Safe
    D -->|"No"| E["Resolve against configured staging site origin"]
    E --> F{"Resolved origin exactly equals configured origin?"}
    F -->|"No"| Safe
    F -->|"Yes"| G{"Path is /events, /events/* or /auth?"}
    G -->|"No"| Safe
    G -->|"Yes"| H["Return normalized internal path + query"]
    H --> I["Exchange PKCE code and redirect"]
    Safe --> I
```

The same pure `safeRedirectPath` function is used before creating OAuth redirect URLs
and again in the callback. The callback is authoritative. Hash fragments are discarded,
and unsafe input is never echoed in error copy.

## 7. SEC-008 scanner destination policy

```mermaid
sequenceDiagram
    autonumber
    participant API
    participant Scanner
    participant Policy as Destination policy
    participant Storage as Supabase storage
    participant Callback as Exact API callback

    API->>Scanner: HMAC-signed ScanRequest
    Scanner->>Policy: Validate readUrl, previewWriteUrl, callbackUrl
    Policy->>Policy: HTTPS only; no userinfo; default port
    Policy->>Policy: exact configured storage host and path prefix
    Policy->>Policy: exact API callback origin and path
    Policy->>Policy: reject IP literals, private/link-local hosts and redirects
    alt any destination rejected
        Scanner-->>API: No outbound request; safe scan failure
    else all destinations accepted
        Scanner->>Storage: GET quarantined object (10 MiB cap)
        Scanner->>Storage: PUT preview when applicable
        Scanner->>Callback: HMAC result callback
    end
```

Application allowlisting and a supported internal API-to-scanner route are required.
Additional egress firewall restriction is optional defence-in-depth; any paid connector,
load balancer or firewall commitment requires a separate Product Owner decision.

## 8. Media retry and idempotency behavior

```mermaid
stateDiagram-v2
    state "Scan job" as JOB {
        [*] --> pending
        pending --> leased: Scheduler leases
        failed --> leased: Retry after 1m / 5m / 20m
        leased --> completed: Callback accepted
        leased --> failed: Dispatch or callback fails
    }
    state "Media object" as MEDIA {
        [*] --> quarantine
        quarantine --> scanning: Upload completion verified
        scanning --> active: Matching clean callback
        scanning --> rejected: Matching rejection callback
        active --> active: Matching terminal replay
        rejected --> rejected: Matching terminal replay
        active --> conflict: Conflicting callback
        rejected --> conflict: Conflicting callback
    }
```

The existing two-minute lease, capped retry schedule, nonce consumption, matching
terminal replay and idempotent storage promotion remain unchanged.

## 9. Evidence data model

```mermaid
erDiagram
    STAGING_EVIDENCE ||--|| RELEASE : identifies
    STAGING_EVIDENCE ||--|{ CONTROL_EVIDENCE : contains
    CONTROL_EVIDENCE o|--o| DEFECT_REFERENCE : "requires when FAIL"
    CONTROL_EVIDENCE o|--o| DECISION_REFERENCE : "requires when WAIVED"

    STAGING_EVIDENCE {
        string schemaVersion
        string storyId
        string environment
    }
    RELEASE {
        string commit
        string apiImageDigest
        string scannerImageDigest
        string vercelDeploymentId
    }
    CONTROL_EVIDENCE {
        string controlId
        enum status
        datetime timestampUtc
        enum reviewerRole
        string reviewerReference
        string_array sourceReferences
        string outcome
    }
    DEFECT_REFERENCE {
        string defectReference
    }
    DECISION_REFERENCE {
        string decisionReference
    }
```

The JSON schema validates structure, not redaction. Review and secret scanning remain
mandatory. A final readiness recommendation cannot contain `NOT_RUN`, `IN_PROGRESS`,
`FAIL` or `BLOCKED` for a release-critical control unless a separate explicit waiver is
recorded.

## 10. Immutable delivery and independent assurance

```mermaid
flowchart LR
    Commit["Committed source SHA"] --> CI["CI<br/>contracts · Java · web · browser · images"]
    Commit --> Security["Security workflow<br/>Gitleaks · npm · NVD"]
    CI --> Publish["Build once<br/>SBOM + provenance"]
    Security --> Publish
    Publish --> Digests["API + scanner digests"]
    Commit --> Vercel["Immutable Vercel deployment"]
    Digests --> Stage["Deploy isolated staging"]
    Vercel --> Stage
    Stage --> QA["Independent QA"]
    Stage --> SecReview["Independent Security retest"]
    QA --> Evidence["Versioned redacted evidence"]
    SecReview --> Evidence
    Evidence --> Decision{"All critical controls PASS<br/>or explicit PO WAIVE?"}
    Decision -->|"No"| Rework["Defect, blocker or PO decision"]
    Decision -->|"Yes"| Ready["Staging-ready handoff"]
    Ready --> Production["Separate production decision<br/>outside US-003"]
```

Implementation authors provide evidence but cannot close SEC findings. QA owns
acceptance/regression; Security owns independent SEC-001–004 closure; Operations owns
provider and rollback evidence.

## 11. Rollout and rollback

```mermaid
flowchart TD
    A["Deploy scanner digest<br/>internal ingress · API-only invoker"] --> B["Verify private route, ID token, HMAC and destination policy"]
    B --> C["Deploy API digest<br/>verify health, JWT, CORS, RLS and jobs"]
    C --> D["Deploy Vercel staging build<br/>demo mode off"]
    D --> E["Execute auth, media, Maps, metadata and telemetry checks"]
    E --> F["Run independent QA/Security and rollback drill"]
    F --> G{"Critical failure?"}
    G -->|"No"| H["Record staging-ready evidence"]
    G -->|"Yes"| I["Stop exposure"]
    I --> J["Shift API/scanner independently to prior Cloud Run revisions"]
    J --> K["Restore prior Vercel deployment"]
    K --> L["No Flyway down migration"]
    L --> M["Reconcile scanning media and rerun retention"]
    M --> N["Smoke health, auth, publish, metadata and clean upload"]
```

If the HMAC may be compromised, rotate Secret Manager and deploy both Java services
before resuming. A Supabase restore drill uses synthetic data and must prove that deleted
content does not return to an active surface.

## 12. Implementation dependency graph

```mermaid
flowchart LR
    Baseline["Commit US-002 baseline"] --> B1["[BE] SEC-001<br/>bounded envelopes"]
    Baseline --> B2["[BE] SEC-002<br/>startup secret gate"]
    Baseline --> B3["[BE] SEC-003<br/>JWT role"]
    Baseline --> B4["[BE] SEC-008<br/>destination policy"]
    Baseline --> B5["[BE] Private scanner<br/>Google ID token"]
    B1 --> BE["Backend gate"]
    B2 --> BE
    B3 --> BE
    B4 --> BE
    B5 --> BE
    BE --> F1["[FE] SEC-004<br/>safe redirect"]
    F1 --> FE["Frontend gate"]
    FE --> QA["QA join"]
    QA --> Security["Security retest"]
    Security --> Operations["Staging provisioning,<br/>checklist and rollback"]
    Operations --> PO["PO staging-readiness decision"]
```

Architecture proposes `fullstack` and `backend-first`: backend closes the two High
findings and stabilizes the trust boundary before the small frontend redirect slice.
