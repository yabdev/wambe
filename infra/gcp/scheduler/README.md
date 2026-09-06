# Cloud Scheduler jobs

Create these jobs only after the API is deployed and the dedicated Scheduler service
account has Cloud Run invoker permission. The API validates Google issuer, exact
audience, and exact service-account subject. Do not set `INTERNAL_JOB_KEY`.

The API's `SCHEDULER_SUBJECT` must be the service account's numeric `uniqueId`
(`gcloud iam service-accounts describe "${SCHEDULER_SERVICE_ACCOUNT}" --format='value(uniqueId)'`):
Google ID tokens carry that ID in `sub` and the email only in `email`, so configuring
the email rejects every job with an invalid-subject error.

Suggested pilot schedules:

- scan dispatch: every minute, UTC
- retention: daily at 02:15 UTC

Review quotas and cost before applying. Replace every `${...}` placeholder:

```bash
gcloud scheduler jobs create http wambe-scan-dispatch \
  --location="${REGION}" \
  --schedule="* * * * *" \
  --time-zone="Etc/UTC" \
  --uri="${API_ORIGIN}/api/v1/internal/jobs/scan-dispatch" \
  --http-method=POST \
  --oidc-service-account-email="${SCHEDULER_SERVICE_ACCOUNT}" \
  --oidc-token-audience="${API_ORIGIN}"

gcloud scheduler jobs create http wambe-retention \
  --location="${REGION}" \
  --schedule="15 2 * * *" \
  --time-zone="Etc/UTC" \
  --uri="${API_ORIGIN}/api/v1/internal/jobs/retention" \
  --http-method=POST \
  --oidc-service-account-email="${SCHEDULER_SERVICE_ACCOUNT}" \
  --oidc-token-audience="${API_ORIGIN}"
```

After creation, force each job once in staging. Record the HTTP result, correlated API
request ID, affected row/object counts, and confirmation that requests signed with the
local job key are rejected. Retention must first run against synthetic staging data;
never copy production host content into staging.
