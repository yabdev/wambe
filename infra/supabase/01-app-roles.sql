-- Wambe staging: run once in the Supabase SQL editor BEFORE the first API deployment.
--
-- Flyway V1__baseline.sql creates `wambe_api` as NOLOGIN when the role is missing, so
-- the LOGIN role must exist first (mirrors infra/local/postgres/01-app-role.sql).
-- Replace the placeholder password with a long random value and never commit it.
-- The Supavisor pooler login name for this role is `wambe_api.<project-ref>`.

CREATE ROLE wambe_api LOGIN PASSWORD 'REPLACE_WITH_A_LONG_RANDOM_PASSWORD'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;

-- Private buckets used by the API's Supabase storage adapter
-- (STORAGE_QUARANTINE_BUCKET / STORAGE_ACTIVE_BUCKET defaults). Objects are only ever
-- read or written with the server-side service-role key; browsers receive short-lived
-- signed URLs. The 10 MiB limit matches the scanner's download cap.
INSERT INTO storage.buckets (id, name, public, file_size_limit)
VALUES
    ('media-quarantine', 'media-quarantine', false, 10485760),
    ('media-active', 'media-active', false, 10485760)
ON CONFLICT (id) DO NOTHING;
