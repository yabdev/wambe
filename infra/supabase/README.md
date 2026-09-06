# Supabase staging project

One isolated Supabase project for staging (free tier). Never reuse it for production.
Companion to Phase 2 of `docs/operations/mvp-launch-runbook.md`.

## 1. Create the project

- Organisation: the founder's Supabase org. Region: closest to the GCP region used
  for Cloud Run (the API talks to PostgreSQL on every request).
- Save the generated database password; it is the Flyway (migration) password.
- Note the project reference `<ref>` from the dashboard URL. Every value below derives
  from it:

| Value | Where it goes |
|---|---|
| `https://<ref>.supabase.co` | `SUPABASE_URL` (Cloud Run env, rendered by `deploy-cloud-run.sh`) and `NEXT_PUBLIC_SUPABASE_URL` (Vercel) |
| `https://<ref>.supabase.co/auth/v1/.well-known/jwks.json` | `SUPABASE_JWKS_URI` (default derived by `deploy-cloud-run.sh`) |
| `https://<ref>.supabase.co/auth/v1` | `SUPABASE_ISSUER` (default derived by `deploy-cloud-run.sh`) |
| anon / publishable key | `NEXT_PUBLIC_SUPABASE_ANON_KEY` (Vercel only) |
| service-role key | Secret Manager `wambe-supabase-service-role` only; never in Vercel, the browser or evidence files |

## 2. Authentication

1. **JWT signing keys**: the API validates tokens through the JWKS endpoint, which
   requires an asymmetric signing key. In *Authentication → JWT Keys* make sure the
   current key is asymmetric (ECC P-256 or RSA). If the project still uses the legacy
   HS256 shared secret, rotate to an asymmetric key before testing sign-in
   (`AUTH-003` fails closed otherwise).
2. **Google provider**: create an OAuth web client in the staging GCP project
   (*APIs & Services → Credentials*) with the authorised redirect URI
   `https://<ref>.supabase.co/auth/v1/callback`, then paste its client ID/secret into
   *Authentication → Providers → Google*.
3. **Email provider**: keep email/password enabled with *Confirm email* on. The
   built-in SMTP is rate-limited to a handful of messages per hour, which is enough
   for staging; configure Resend (or another SMTP) under *Authentication → SMTP*
   before any real host traffic (`AUTH-002`).
4. **URL configuration**: *Site URL* = the exact Vercel staging origin. *Redirect
   URLs* = `https://<vercel-origin>/auth/callback` and
   `http://localhost:3000/auth/callback`. The web app builds every callback from
   `NEXT_PUBLIC_SITE_URL`, so the two must match exactly (`AUTH-001`, `SECURITY-005`).

## 3. Database roles and buckets

Run `01-app-roles.sql` in the SQL editor after replacing the placeholder password.
It creates the RLS application login `wambe_api` and the two private media buckets.
Flyway creates everything else on the first API start.

Connection values for Secret Manager (created empty by `infra/gcp/bootstrap-staging.sh`):

| Secret | Value |
|---|---|
| `wambe-database-url` | `jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require` (Supavisor **session** mode host from *Project Settings → Database*; port 5432, not 6543) |
| `wambe-database-user` | `wambe_api.<ref>` |
| `wambe-database-password` | the password chosen in `01-app-roles.sql` |
| `wambe-flyway-user` | `postgres.<ref>` |
| `wambe-flyway-password` | the project database password |

Use the pooler rather than the direct host: the free tier's direct host is IPv6-only
and Cloud Run egress is IPv4. Session mode is required because the API relies on
per-connection settings that transaction pooling breaks. Keep
`DATABASE_POOL_SIZE` at the default until the pooler's connection quota is measured
(the architecture asks for `maxInstances × pool` below 70% of quota).

## 4. Storage

`01-app-roles.sql` already inserted `media-quarantine` and `media-active` as private
buckets. Verify in *Storage* that neither is public and that no bucket policy grants
anon or authenticated access; only the service-role key touches objects (`MEDIA-001`,
`MEDIA-005`).

## 5. Known free-tier limits (Product Owner decisions)

- No point-in-time recovery or scheduled backups on the free tier. `RECOVERY-007`
  needs either the Pro plan or an explicit PO waiver for staging.
- Projects pause after a week of inactivity; a paused project fails every API health
  check. Unpause before running QA or the pilot.
- Built-in SMTP rate limits will block `AUTH-002` under load; Resend's free tier is
  the planned mitigation.
