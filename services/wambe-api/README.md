# Wambe API

Spring Boot 4.1 / Java 21 implementation of US-002, generated contract-first from
`docs/sdlc/stories/US-002-create-publish-event/contracts/openapi-v1.yaml`.

## Local prerequisites

- Java 21
- Docker Desktop (integration tests and local PostgreSQL)

Maven does not need to be installed; use the checked-in wrapper.

## Run

From the repository root:

```powershell
docker compose up -d postgres
$env:SPRING_PROFILES_ACTIVE = "local"
$env:DATABASE_USER = "wambe_api"
$env:DATABASE_PASSWORD = "wambe_local"
$env:FLYWAY_USER = "postgres"
$env:FLYWAY_PASSWORD = "postgres"
cd services/wambe-api
.\mvnw.cmd spring-boot:run
```

Health: `GET http://localhost:8080/actuator/health`.

The local profile exposes a development-only object endpoint under `/dev-storage/**`.
Production uses `STORAGE_TYPE=supabase`; never expose a Supabase service-role key to the
browser.

## Verify

```powershell
.\mvnw.cmd clean verify
npx @redocly/cli lint ..\..\docs\sdlc\stories\US-002-create-publish-event\contracts\openapi-v1.yaml
```

The tests use PostgreSQL 16 through Testcontainers. They cover migration startup,
transaction-local RLS isolation on a reused pool connection, event create/update/publish
and idempotent replay, publication validation, security boundaries, and scanner HMAC
freshness/signature validation.

## Required production configuration

| Variable | Purpose |
|---|---|
| `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` | Supabase PostgreSQL application login |
| `FLYWAY_USER`, `FLYWAY_PASSWORD` | Migration principal; separate from the RLS application login |
| `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` | Server-only Auth Admin and Storage access |
| `SUPABASE_JWKS_URI`, `SUPABASE_ISSUER` | Supabase JWT validation |
| `CORS_ALLOWED_ORIGINS` | Exact frontend origins |
| `STORAGE_TYPE=supabase` | Select private Supabase storage adapter |
| `SCANNER_URL`, `SCANNER_HMAC_SECRET` | Malware scanner dispatch and callback authentication |
| `SCHEDULER_JWKS_URI`, `SCHEDULER_ISSUER`, `SCHEDULER_AUDIENCE`, `SCHEDULER_SUBJECT` | Cloud Scheduler/Run OIDC validation |
| `PUBLIC_BASE_URL`, `API_BASE_URL` | Canonical host page and API callback bases |

`INTERNAL_JOB_KEY` is only for local development. Set it to an empty value in deployed
environments so internal jobs require the configured Google OIDC identity.

## Deployment and rollback

Build the Cloud Run image from the repository root:

```powershell
docker build -f services/wambe-api/Dockerfile -t wambe-api:local .
```

Flyway migrations are forward-only. Roll back the Cloud Run revision independently;
do not delete migration history or reverse a migration in place. Schema changes in this
story are additive, and the previous API revision can continue reading the baseline
tables.
