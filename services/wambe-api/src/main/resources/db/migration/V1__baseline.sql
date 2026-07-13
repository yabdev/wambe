CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'wambe_api') THEN
        CREATE ROLE wambe_api NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'wambe_jobs') THEN
        CREATE ROLE wambe_jobs NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
    END IF;
END
$$;

CREATE TABLE host_profiles (
    id uuid PRIMARY KEY,
    verified_email_snapshot text NOT NULL,
    display_name text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE identity_link_audit (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id uuid NOT NULL,
    provider text NOT NULL,
    provider_subject_hash text NOT NULL,
    verified_email_proof_at timestamptz NOT NULL,
    actor text NOT NULL,
    result text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE events (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL,
    client_creation_key uuid,
    event_type text,
    title text,
    starts_at timestamptz,
    timezone text NOT NULL DEFAULT 'Africa/Lagos',
    status text NOT NULL DEFAULT 'draft'
        CHECK (status IN ('draft', 'published', 'unpublished', 'deleted')),
    visibility text
        CHECK (visibility IS NULL OR visibility IN ('public', 'private_link', 'invite_only', 'hidden_location')),
    venue_name text,
    venue_display_address text,
    venue_place_id text,
    venue_latitude numeric(9,6),
    venue_longitude numeric(9,6),
    venue_confirmed_at timestamptz,
    dress_code_notes text,
    slug text,
    version bigint NOT NULL DEFAULT 0,
    published_at timestamptz,
    unpublished_at timestamptz,
    deleted_at timestamptz,
    last_saved_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT venue_latitude_range CHECK (venue_latitude IS NULL OR venue_latitude BETWEEN -90 AND 90),
    CONSTRAINT venue_longitude_range CHECK (venue_longitude IS NULL OR venue_longitude BETWEEN -180 AND 180)
);

CREATE UNIQUE INDEX events_slug_uq ON events (slug) WHERE slug IS NOT NULL;
CREATE UNIQUE INDEX events_owner_creation_key_uq
    ON events (owner_id, client_creation_key) WHERE client_creation_key IS NOT NULL;
CREATE INDEX events_owner_status_updated_idx ON events (owner_id, status, updated_at DESC);
CREATE INDEX events_abandoned_draft_idx
    ON events (last_saved_at) WHERE status = 'draft';

CREATE TABLE event_media (
    id uuid PRIMARY KEY,
    event_id uuid NOT NULL REFERENCES events(id),
    owner_id uuid NOT NULL,
    role text NOT NULL CHECK (role IN ('invitation', 'aso_ebi')),
    filename text NOT NULL,
    claimed_mime_type text NOT NULL,
    detected_mime_type text,
    size_bytes bigint NOT NULL CHECK (size_bytes BETWEEN 1 AND 10485760),
    quarantine_path text NOT NULL,
    active_path text,
    preview_path text,
    storage_status text NOT NULL
        CHECK (storage_status IN ('quarantine', 'scanning', 'active', 'rejected', 'deleted')),
    rejection_code text,
    object_sha256 text,
    preview_sha256 text,
    deleted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX event_media_event_idx ON event_media (owner_id, event_id, created_at);
CREATE INDEX event_media_deleted_idx
    ON event_media (deleted_at) WHERE storage_status = 'deleted';

CREATE TABLE scan_jobs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    media_id uuid NOT NULL REFERENCES event_media(id),
    owner_id uuid NOT NULL,
    status text NOT NULL CHECK (status IN ('pending', 'leased', 'completed', 'failed')),
    attempts integer NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL DEFAULT now(),
    lease_expires_at timestamptz,
    last_error_code text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX scan_jobs_media_uq ON scan_jobs (media_id);
CREATE INDEX scan_jobs_due_idx ON scan_jobs (next_attempt_at)
    WHERE status IN ('pending', 'failed');

CREATE TABLE creation_sessions (
    id uuid PRIMARY KEY,
    event_id uuid NOT NULL REFERENCES events(id),
    owner_id uuid NOT NULL,
    opened_at timestamptz NOT NULL,
    first_published_at timestamptz,
    eligibility text NOT NULL
        CHECK (eligibility IN ('eligible_first_time', 'resumed_draft', 'staff_assisted')),
    device_class text NOT NULL DEFAULT 'unknown',
    network_quality text NOT NULL DEFAULT 'unknown',
    milestone_summary jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX creation_sessions_event_idx ON creation_sessions (owner_id, event_id, opened_at);

CREATE TABLE product_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id uuid NOT NULL,
    event_id uuid NOT NULL REFERENCES events(id),
    creation_session_id uuid NOT NULL REFERENCES creation_sessions(id),
    name text NOT NULL,
    schema_version text NOT NULL DEFAULT '1.0',
    occurred_at timestamptz NOT NULL,
    received_at timestamptz NOT NULL DEFAULT now(),
    allowed_properties jsonb NOT NULL DEFAULT '{}'::jsonb,
    consent_category text NOT NULL DEFAULT 'essential'
);

CREATE INDEX product_events_session_idx
    ON product_events (owner_id, creation_session_id, received_at);

CREATE TABLE idempotency_keys (
    owner_id uuid NOT NULL,
    route text NOT NULL,
    idempotency_key uuid NOT NULL,
    request_hash text NOT NULL,
    response_status integer,
    response_body jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    PRIMARY KEY (owner_id, route, idempotency_key)
);

CREATE INDEX idempotency_expiry_idx ON idempotency_keys (expires_at);

CREATE TABLE scanner_callback_nonces (
    nonce uuid PRIMARY KEY,
    media_id uuid NOT NULL REFERENCES event_media(id),
    owner_id uuid NOT NULL,
    request_timestamp timestamptz NOT NULL,
    body_digest text NOT NULL,
    consumed_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL
);

CREATE INDEX scanner_nonce_expiry_idx ON scanner_callback_nonces (expires_at);

CREATE TABLE audit_log (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id uuid NOT NULL,
    actor text NOT NULL,
    action text NOT NULL,
    resource_type text NOT NULL,
    resource_id uuid,
    outcome text NOT NULL,
    safe_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX audit_log_owner_created_idx ON audit_log (owner_id, created_at DESC);

GRANT USAGE ON SCHEMA public TO wambe_api, wambe_jobs;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO wambe_api;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO wambe_jobs;

DO $$
DECLARE
    table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'host_profiles', 'identity_link_audit', 'events', 'event_media', 'scan_jobs',
        'creation_sessions', 'product_events', 'idempotency_keys',
        'scanner_callback_nonces', 'audit_log'
    ]
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', table_name);
    END LOOP;
END
$$;

CREATE POLICY host_profiles_owner ON host_profiles TO wambe_api
    USING (id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);
CREATE POLICY identity_link_audit_owner ON identity_link_audit TO wambe_api
    USING (host_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid)
    WITH CHECK (host_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

DO $$
DECLARE
    table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'events', 'event_media', 'scan_jobs', 'creation_sessions', 'product_events',
        'idempotency_keys', 'scanner_callback_nonces', 'audit_log'
    ]
    LOOP
        EXECUTE format(
            'CREATE POLICY %I_owner ON %I TO wambe_api
             USING (owner_id = NULLIF(current_setting(''app.current_user_id'', true), '''')::uuid)
             WITH CHECK (owner_id = NULLIF(current_setting(''app.current_user_id'', true), '''')::uuid)',
            table_name, table_name
        );
        EXECUTE format(
            'CREATE POLICY %I_jobs ON %I TO wambe_jobs USING (true) WITH CHECK (true)',
            table_name, table_name
        );
    END LOOP;
END
$$;

CREATE OR REPLACE FUNCTION public.safe_event_metadata(requested_slug text)
RETURNS TABLE (
    slug text,
    title text,
    starts_at timestamptz,
    visibility text,
    indexable boolean,
    venue_name text,
    venue_address text,
    preview_path text,
    deleted boolean
)
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT e.slug,
           e.title,
           e.starts_at,
           e.visibility,
           e.visibility = 'public',
           e.venue_name,
           e.venue_display_address,
           (
               SELECT m.preview_path
               FROM event_media m
               WHERE m.event_id = e.id
                 AND m.role = 'invitation'
                 AND m.storage_status = 'active'
               ORDER BY m.created_at
               LIMIT 1
           ),
           e.status = 'deleted'
    FROM events e
    WHERE e.slug = requested_slug
      AND (
          (e.status = 'published' AND e.visibility IN ('public', 'private_link'))
          OR e.status = 'deleted'
      )
    LIMIT 1
$$;

REVOKE ALL ON FUNCTION public.safe_event_metadata(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.safe_event_metadata(text) TO wambe_api;
