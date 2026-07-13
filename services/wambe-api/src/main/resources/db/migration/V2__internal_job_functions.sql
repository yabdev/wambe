CREATE OR REPLACE FUNCTION public.scanner_media_owner(requested_media_id uuid)
RETURNS uuid
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT owner_id
    FROM event_media
    WHERE id = requested_media_id
      AND storage_status IN ('scanning', 'active', 'rejected')
$$;

REVOKE ALL ON FUNCTION public.scanner_media_owner(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.scanner_media_owner(uuid) TO wambe_api;

CREATE OR REPLACE FUNCTION public.lease_due_scan_jobs(batch_size integer)
RETURNS TABLE (job_id uuid, media_id uuid, owner_id uuid)
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
    WITH due AS (
        SELECT id
        FROM scan_jobs
        WHERE status IN ('pending', 'failed')
          AND next_attempt_at <= now()
          AND (lease_expires_at IS NULL OR lease_expires_at <= now())
        ORDER BY next_attempt_at
        FOR UPDATE SKIP LOCKED
        LIMIT LEAST(GREATEST(batch_size, 1), 50)
    )
    UPDATE scan_jobs job
       SET status = 'leased',
           attempts = attempts + 1,
           lease_expires_at = now() + interval '2 minutes',
           updated_at = now()
      FROM due
     WHERE job.id = due.id
    RETURNING job.id, job.media_id, job.owner_id
$$;

REVOKE ALL ON FUNCTION public.lease_due_scan_jobs(integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.lease_due_scan_jobs(integer) TO wambe_api;

CREATE OR REPLACE FUNCTION public.retention_storage_paths(run_at timestamptz)
RETURNS TABLE (quarantine_path text, active_path text, preview_path text)
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT media.quarantine_path, media.active_path, media.preview_path
    FROM event_media media
    JOIN events event ON event.id = media.event_id
    WHERE (event.status = 'draft' AND event.last_saved_at < run_at - interval '30 days')
       OR (media.storage_status = 'deleted' AND media.deleted_at < run_at - interval '30 days')
$$;

REVOKE ALL ON FUNCTION public.retention_storage_paths(timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.retention_storage_paths(timestamptz) TO wambe_api;

CREATE OR REPLACE FUNCTION public.run_wambe_retention(run_at timestamptz)
RETURNS TABLE (examined bigint, affected bigint)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    examined_count bigint := 0;
    affected_count bigint := 0;
    changed bigint := 0;
BEGIN
    SELECT count(*) INTO examined_count
    FROM events
    WHERE status = 'draft' AND last_saved_at < run_at - interval '30 days';

    DELETE FROM product_events
    WHERE event_id IN (
        SELECT id FROM events
        WHERE status = 'draft' AND last_saved_at < run_at - interval '30 days'
    );
    DELETE FROM creation_sessions
    WHERE event_id IN (
        SELECT id FROM events
        WHERE status = 'draft' AND last_saved_at < run_at - interval '30 days'
    );
    DELETE FROM scanner_callback_nonces
    WHERE media_id IN (
        SELECT media.id
        FROM event_media media
        JOIN events event ON event.id = media.event_id
        WHERE event.status = 'draft' AND event.last_saved_at < run_at - interval '30 days'
    );
    DELETE FROM scan_jobs
    WHERE media_id IN (
        SELECT media.id
        FROM event_media media
        JOIN events event ON event.id = media.event_id
        WHERE event.status = 'draft' AND event.last_saved_at < run_at - interval '30 days'
    );
    DELETE FROM event_media
    WHERE event_id IN (
        SELECT id FROM events
        WHERE status = 'draft' AND last_saved_at < run_at - interval '30 days'
    );
    DELETE FROM events
    WHERE status = 'draft' AND last_saved_at < run_at - interval '30 days';
    GET DIAGNOSTICS changed = ROW_COUNT;
    affected_count := affected_count + changed;

    DELETE FROM idempotency_keys WHERE expires_at <= run_at;
    GET DIAGNOSTICS changed = ROW_COUNT;
    affected_count := affected_count + changed;

    DELETE FROM scanner_callback_nonces WHERE expires_at <= run_at;
    GET DIAGNOSTICS changed = ROW_COUNT;
    affected_count := affected_count + changed;

    DELETE FROM scanner_callback_nonces
    WHERE media_id IN (
        SELECT id FROM event_media
        WHERE storage_status = 'deleted'
          AND deleted_at < run_at - interval '30 days'
    );
    DELETE FROM scan_jobs
    WHERE media_id IN (
        SELECT id FROM event_media
        WHERE storage_status = 'deleted'
          AND deleted_at < run_at - interval '30 days'
    );
    DELETE FROM event_media
    WHERE storage_status = 'deleted'
      AND deleted_at < run_at - interval '30 days';
    GET DIAGNOSTICS changed = ROW_COUNT;
    affected_count := affected_count + changed;

    RETURN QUERY SELECT examined_count, affected_count;
END
$$;

REVOKE ALL ON FUNCTION public.run_wambe_retention(timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.run_wambe_retention(timestamptz) TO wambe_api;
