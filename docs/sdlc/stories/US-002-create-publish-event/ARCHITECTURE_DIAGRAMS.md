# US-002 Architecture Diagrams

Story: **Create and publish a Wambe event**  
Architecture status: **Approved by the Product Owner on 2026-07-13**  
Companion visual board: [`ARCHITECTURE_DIAGRAMS.html`](./ARCHITECTURE_DIAGRAMS.html)  
Contracts: [`contracts/openapi-v1.yaml`](./contracts/openapi-v1.yaml) and
[`contracts/product-events-v1.schema.json`](./contracts/product-events-v1.schema.json)

These diagrams elaborate the architecture decisions in `ARTIFACTS.md`. The contracts and
architecture text remain authoritative if a diagram becomes stale.

## 1. System context

```mermaid
flowchart LR
    Host["Host<br/>creates and manages Wambes"]
    Guest["Guest / social crawler<br/>metadata-only in US-002"]
    Wambe["Wambe platform<br/>Next.js + Spring Boot"]
    Maps["Google Maps Platform"]
    Auth["Supabase Auth"]
    Resend["Resend SMTP"]

    Host -->|"HTTPS"| Wambe
    Guest -->|"Public or private-link slug"| Wambe
    Wambe -->|"OAuth, email/password, JWT"| Auth
    Auth -->|"Verification and reset email"| Resend
    Wambe -->|"Place search and pin"| Maps
```

**Boundary:** RSVP, guest authentication, invitations, QR passes, payments, and full
guest event pages are outside US-002.

## 2. Container and trust-boundary view

```mermaid
flowchart LR
    subgraph Browser["Untrusted browser"]
        UI["Next.js host UI / PWA"]
        Client["Generated TypeScript API client"]
        MapsWidget["Google Maps widget"]
        UI --> Client
        UI --> MapsWidget
    end

    subgraph Vercel["Vercel"]
        Web["Next.js App Router<br/>auth callback + /e/{slug} metadata HTML"]
    end

    subgraph GCP["Google Cloud"]
        API["Spring Boot API<br/>Spring Security + MVC + JPA"]
        Scanner["ClamAV scanner<br/>isolated Cloud Run service"]
        Scheduler["Cloud Scheduler<br/>OIDC-authenticated jobs"]
    end

    subgraph Supabase["Supabase managed boundary"]
        Auth["Auth + JWKS"]
        DB[("PostgreSQL<br/>Flyway + RLS")]
        Quarantine[("Private quarantine bucket")]
        Active[("Private active bucket")]
    end

    subgraph Telemetry["Isolated self-hosted telemetry"]
        Umami["Umami"]
        OTel["OTel Collector"]
        Grafana["Grafana / Loki / Tempo / Prometheus"]
        OTel --> Grafana
    end

    UI --> Web
    Web --> Auth
    Client -->|"Bearer Supabase JWT"| API
    API -->|"JWKS validation"| Auth
    API -->|"JDBC as wambe_api"| DB
    API -->|"Signed upload/read operations"| Quarantine
    API -->|"Signed active access"| Active
    API -->|"Signed scan request"| Scanner
    Scanner -->|"Short-lived read URL"| Quarantine
    Scanner -->|"HMAC result callback"| API
    Scheduler -->|"OIDC internal request"| API
    UI -. "Consent-gated analytics" .-> Umami
    API -. "Fail-open OTLP" .-> OTel
```

**Rules:** the browser never receives service-role, database, scanner, or telemetry
credentials. The scanner has no database credentials. Spring is stateless and does not
store Supabase refresh tokens.

## 3. Spring Boot component view

```mermaid
flowchart TB
    HTTP["Spring MVC controllers<br/>OpenAPI delegates"]
    Security["Spring Security<br/>JWT, CORS, principal"]
    Common["Common web<br/>request ID, errors, idempotency"]

    EventApp["Event application services<br/>draft, publish, lifecycle"]
    MediaApp["Media application services<br/>intent, complete, promotion"]
    SessionApp["Creation session services<br/>KPI milestones"]
    IdentityApp["Identity-link service"]
    Jobs["Retention and scan dispatch handlers"]

    EventDomain["Event domain<br/>state and publication rules"]
    Persistence["JPA repositories<br/>explicit owner predicates"]
    RLS["Transaction RLS context<br/>set_config local"]

    Storage["Supabase storage adapter"]
    AuthAdmin["Supabase Auth admin adapter"]
    ScanAdapter["Scanner HMAC adapter"]
    Telemetry["OTel/product-event adapters"]

    Security --> HTTP
    Common --> HTTP
    HTTP --> EventApp
    HTTP --> MediaApp
    HTTP --> SessionApp
    HTTP --> IdentityApp
    HTTP --> Jobs
    EventApp --> EventDomain
    MediaApp --> EventDomain
    EventApp --> Persistence
    MediaApp --> Persistence
    SessionApp --> Persistence
    IdentityApp --> Persistence
    Jobs --> Persistence
    Persistence --> RLS
    MediaApp --> Storage
    IdentityApp --> AuthAdmin
    MediaApp --> ScanAdapter
    Jobs --> ScanAdapter
    EventApp -. "After commit" .-> Telemetry
```

Controllers stay thin. Application services own `@Transactional` boundaries. JPA
entities use `@Version`; Hibernate uses `ddl-auto=validate`; Flyway exclusively owns
schema and RLS migrations.

## 4. Deployment view

```mermaid
flowchart TB
    subgraph Users["Internet"]
        Browser["Host browser"]
        Crawler["Guest / social crawler"]
    end

    subgraph Edge["Frontend"]
        Vercel["Vercel<br/>Next.js deployment"]
    end

    subgraph Google["Google Cloud project"]
        ApiRun["Cloud Run: wambe-api<br/>Spring Boot container<br/>0 minimum instances initially"]
        ScanRun["Cloud Run: media-scanner<br/>ClamAV container<br/>scale to zero"]
        Sched["Cloud Scheduler"]
        Secrets["Secret Manager"]
        Logs["Cloud logging bridge"]
    end

    subgraph Data["Supabase project"]
        Pooler["Supavisor / PostgreSQL endpoint"]
        Postgres[("PostgreSQL")]
        SupaAuth["Auth + JWKS"]
        Storage[("Private storage")]
        Pooler --> Postgres
    end

    subgraph Ops["Private telemetry VPS"]
        Collector["OTel Collector"]
        Observability["Grafana stack"]
        Analytics["Umami"]
        Collector --> Observability
    end

    Browser --> Vercel
    Crawler --> Vercel
    Browser -->|"HTTPS API + Bearer JWT"| ApiRun
    Vercel --> SupaAuth
    Vercel -->|"Server-render metadata request"| ApiRun
    ApiRun --> Pooler
    ApiRun --> SupaAuth
    ApiRun --> Storage
    ApiRun --> ScanRun
    ScanRun --> Storage
    Sched -->|"OIDC"| ApiRun
    Secrets --> ApiRun
    Secrets --> ScanRun
    ApiRun -. "OTLP" .-> Collector
    Vercel -. "Consent-gated" .-> Analytics
    ApiRun -. "Redacted logs" .-> Logs
```

Development, staging, and production use separate Supabase, Vercel, and Google Cloud
environments. API and scanner revisions roll back independently.

## 5. Logical database ERD

The ERD is implementation-directed but not a substitute for Flyway migrations. UUIDs,
timestamps, enum/check constraints, foreign keys, indexes, and RLS policies must be
defined by migration.

```mermaid
erDiagram
    AUTH_USERS {
        uuid id PK
        text email
        timestamptz created_at
    }

    HOST_PROFILES {
        uuid id PK,FK
        text verified_email_snapshot
        text display_name
        timestamptz created_at
        timestamptz updated_at
    }

    IDENTITY_LINK_AUDIT {
        uuid id PK
        uuid host_id FK
        text provider
        text provider_subject_hash
        timestamptz verified_email_proof_at
        text actor
        text result
        timestamptz created_at
    }

    EVENTS {
        uuid id PK
        uuid owner_id FK
        uuid client_creation_key
        text event_type
        text title
        timestamptz starts_at
        text timezone
        text status
        text visibility
        text venue_name
        text venue_display_address
        text venue_place_id
        decimal venue_latitude
        decimal venue_longitude
        timestamptz venue_confirmed_at
        text dress_code_notes
        text slug
        bigint version
        timestamptz published_at
        timestamptz unpublished_at
        timestamptz deleted_at
        timestamptz last_saved_at
        timestamptz created_at
        timestamptz updated_at
    }

    EVENT_MEDIA {
        uuid id PK
        uuid event_id FK
        uuid owner_id FK
        text role
        text filename
        text claimed_mime
        text detected_mime
        bigint size_bytes
        text quarantine_path
        text active_path
        text preview_path
        text storage_status
        text scan_result
        text rejection_code
        timestamptz deleted_at
        timestamptz created_at
        timestamptz updated_at
    }

    SCAN_JOBS {
        uuid id PK
        uuid media_id FK
        text status
        int attempts
        timestamptz next_attempt_at
        timestamptz lease_expires_at
        text last_error_code
        timestamptz created_at
        timestamptz updated_at
    }

    CREATION_SESSIONS {
        uuid id PK
        uuid event_id FK
        uuid owner_id FK
        timestamptz opened_at
        timestamptz first_published_at
        text eligibility
        text device_class
        text network_quality
        jsonb milestone_summary
    }

    PRODUCT_EVENTS {
        uuid id PK
        uuid owner_id FK
        uuid event_id FK
        uuid creation_session_id FK
        text name
        text schema_version
        timestamptz occurred_at
        timestamptz received_at
        jsonb allowed_properties
        text consent_category
    }

    IDEMPOTENCY_KEYS {
        uuid owner_id FK
        text route
        uuid idempotency_key
        text request_hash
        int response_status
        jsonb response_body
        timestamptz created_at
        timestamptz expires_at
    }

    SCANNER_CALLBACK_NONCES {
        uuid nonce PK
        uuid media_id FK
        timestamptz request_timestamp
        text body_digest
        timestamptz consumed_at
        timestamptz expires_at
    }

    AUDIT_LOG {
        uuid id PK
        uuid owner_id FK
        uuid resource_id
        text actor
        text action
        text resource_type
        text outcome
        jsonb safe_metadata
        timestamptz created_at
    }

    AUTH_USERS ||--|| HOST_PROFILES : "has profile"
    AUTH_USERS ||--o{ IDENTITY_LINK_AUDIT : "records links"
    AUTH_USERS ||--o{ EVENTS : "owns"
    AUTH_USERS ||--o{ CREATION_SESSIONS : "owns KPI sessions"
    AUTH_USERS ||--o{ PRODUCT_EVENTS : "owns product events"
    AUTH_USERS ||--o{ IDEMPOTENCY_KEYS : "scopes"
    AUTH_USERS ||--o{ AUDIT_LOG : "owns activity"
    EVENTS ||--o{ EVENT_MEDIA : "has"
    EVENT_MEDIA ||--o| SCAN_JOBS : "queues"
    EVENT_MEDIA ||--o{ SCANNER_CALLBACK_NONCES : "protects callbacks"
    EVENTS ||--o{ CREATION_SESSIONS : "measured by"
    EVENTS ||--o{ PRODUCT_EVENTS : "emits"
    CREATION_SESSIONS ||--o{ PRODUCT_EVENTS : "groups"
```

### Required keys and indexes

- Unique `events.slug` where slug is not null.
- Partial unique `(events.owner_id, events.client_creation_key)` where the client key is
  not null.
- Unique `scan_jobs.media_id`.
- Unique `(idempotency_keys.owner_id, route, idempotency_key)`.
- Unique `scanner_callback_nonces.nonce`.
- Index `(events.owner_id, status, updated_at)`.
- Index pending `scan_jobs` by `next_attempt_at`.
- Index abandoned drafts by `last_saved_at` and deleted media by `deleted_at`.
- `auth.users` is Supabase-managed; Flyway owns Wambe application tables and policies.
- RLS compares owner columns with transaction-local
  `current_setting('app.current_user_id', true)`.

## 6. Authentication sequence

```mermaid
sequenceDiagram
    autonumber
    actor Host
    participant Web as Next.js / Supabase SSR
    participant Auth as Supabase Auth
    participant API as Spring Boot API
    participant JWKS as Supabase JWKS
    participant DB as PostgreSQL + RLS

    Host->>Web: Choose Google OAuth or email/password
    Web->>Auth: Start PKCE sign-in / registration
    Auth-->>Web: Authorization code or verified session
    Web->>Auth: Exchange code with PKCE verifier
    Auth-->>Web: Short-lived access token + refresh session
    Web-->>Host: Authenticated host UI

    Host->>API: API request with Bearer access token
    API->>JWKS: Resolve/cache signing key when required
    JWKS-->>API: Public signing keys
    API->>API: Validate signature, issuer, expiry and audience
    API->>DB: BEGIN; set_config(owner ID, transaction-local)
    API->>DB: Owner-scoped query under RLS
    DB-->>API: Host-owned result
    API-->>Host: Response

    alt Access token expired
        API-->>Host: 401
        Host->>Web: Ask Supabase client to refresh
        Web->>Auth: Refresh session
        Auth-->>Web: New access token
        Web-->>Host: Updated client session
        Host->>API: Retry once with new Bearer token
    end
```

Spring does not process passwords, own login callbacks, store refresh tokens, or accept
anon/service-role tokens on host endpoints.

## 7. Draft, autosave, and publish sequence

```mermaid
sequenceDiagram
    autonumber
    actor Host
    participant UI as Next.js editor
    participant API as Spring Boot API
    participant Idem as Idempotency service
    participant DB as PostgreSQL
    participant Telemetry as OTel exporter

    Host->>UI: Open creation form
    UI->>API: POST /events + Idempotency-Key
    API->>Idem: Claim owner + route + key + request hash
    API->>DB: Transaction: create draft and creation session
    DB-->>API: Event version 1 + session ID
    API-->>UI: 201 EventWithSession

    loop Debounced autosave
        UI->>API: PATCH /events/{id} + If-Match + same-operation key
        API->>Idem: Detect replay or claim key
        API->>DB: Owner-scoped update with optimistic version
        alt Current version
            DB-->>API: Updated event and incremented version
            API-->>UI: 200 saved event
        else Stale version
            DB-->>API: Version conflict
            API-->>UI: 409 EVENT_VERSION_CONFLICT + current state
        end
    end

    Host->>UI: Publish
    UI->>API: POST /events/{id}/publish + If-Match + key
    API->>DB: BEGIN + transaction-local owner context
    API->>DB: Validate required fields, future time, venue and media
    API->>DB: Assign immutable slug; set published_at and status
    API->>DB: Complete creation session; write product event and audit
    API->>DB: COMMIT
    API-->>UI: 200 canonical URL + share eligibility
    API-->>Telemetry: Export trace/metrics after commit and fail open
```

The transaction is all-or-nothing. A failed validation or database write leaves the
event unpublished. A replay with the same key and request hash returns the cached result.

## 8. Media upload and malware-scanning sequence

```mermaid
sequenceDiagram
    autonumber
    actor Host
    participant UI as Next.js editor
    participant API as Spring Boot API
    participant Q as Private quarantine storage
    participant Jobs as Scan jobs
    participant Scanner as Cloud Run ClamAV
    participant A as Private active storage

    Host->>UI: Select JPG, PNG, WebP or PDF up to 10 MiB
    UI->>API: POST /events/{id}/media/intents
    API->>API: Validate owner, role, claimed type and size
    API->>Q: Create short-lived signed upload operation
    API-->>UI: Media ID + signed upload URL
    UI->>Q: Direct private upload
    Q-->>UI: Upload complete
    UI->>API: POST /events/{eventId}/media/{mediaId}/complete
    API->>Q: Verify object exists and expected size
    API->>Jobs: Insert unique durable scan job
    API-->>UI: 202 scanning

    Jobs->>API: Lease due scan job
    API->>Scanner: Signed one-file scan request with preview-write URL
    Scanner->>Q: Download through short-lived read URL
    Scanner->>Scanner: Detect magic bytes, parse safely, ClamAV scan

    alt Clean and allowed
        Scanner->>Q: Write sanitized preview through signed URL
        Scanner->>API: HMAC callback: clean + nonce + object/preview digests
        API->>A: Promote verified original and preview
        API->>Q: Delete quarantine object
        UI->>API: GET /events/{eventId}/media
        API-->>UI: Active + preview status
    else Unsafe, corrupt, mismatched or failed
        Scanner->>API: HMAC callback: rejected + safe reason code
        API->>Q: Delete quarantine object
        UI->>API: GET /events/{eventId}/media
        API-->>UI: Rejected + retry guidance
    end
```

Callbacks are at-least-once, timestamped, nonce-protected, and idempotent. Publication is
blocked while attached media is not clean and active.

## 9. Event and media lifecycle states

### Event lifecycle

```mermaid
stateDiagram-v2
    [*] --> draft: Create
    draft --> draft: Autosave
    draft --> published: Publish after validation
    published --> published: Edit published event
    published --> unpublished: Unpublish
    unpublished --> published: Republish with same slug
    draft --> deleted: Delete
    published --> deleted: Delete
    unpublished --> deleted: Delete
    deleted --> [*]: Terminal
```

The slug is assigned once on first publication and remains stable through unpublish and
republish. Deleted is terminal.

### Media lifecycle

```mermaid
stateDiagram-v2
    [*] --> quarantine: Create intent and upload
    quarantine --> scanning: Complete upload
    scanning --> active: Allowed type and clean scan
    scanning --> rejected: Unsafe, invalid, corrupt or scan failure
    rejected --> [*]: Remove quarantine object
    active --> deleted: Host or event deletion
    deleted --> [*]: Purge within 30 days
```

## 10. Visibility and metadata decision flow

```mermaid
flowchart TD
    Request["Guest or crawler requests Next.js /e/{slug}"] --> API["Next.js calls Spring public metadata API"]
    API --> Exists{"Published event exists?"}
    Exists -- No --> Missing["Neutral 404 or 410<br/>no OG metadata"]
    Exists -- Yes --> Mode{"Visibility"}
    Mode -- public --> Public["Safe metadata<br/>indexable"]
    Mode -- private_link --> Private["Safe metadata<br/>noindex"]
    Mode -- invite_only --> Blocked["Neutral unavailable<br/>sharing blocked"]
    Mode -- hidden_location --> Blocked
    Public --> Render["Render canonical /e/{slug} HTML"]
    Private --> Render
```

Full guest access enforcement is a downstream story. US-002 must not leak protected-mode
metadata while that capability is absent.

## Diagram-to-decision traceability

| View | Primary decisions/contracts |
|---|---|
| System context | Scope boundary, ADR-001–006 |
| Containers | ADR-001, ADR-002, ADR-004, ADR-006 |
| Spring components | ADR-008, ADR-010, implementation slices S-01–S-08 |
| Deployment | ADR-001, ADR-004, ADR-006, rollout/rollback plan |
| ERD | Architecture data model, ADR-008, ADR-010, Flyway migrations |
| Authentication | ADR-002, OpenAPI `bearerAuth`, AC-001/AC-011 |
| Draft/publish | OpenAPI event endpoints, ADR-008, AC-002–005/AC-014 |
| Media scan | ADR-004, media endpoints, AC-006 |
| Lifecycles | Event/media invariants, AC-007–010 |
| Visibility flow | ADR-007, public metadata endpoint, AC-013 |

