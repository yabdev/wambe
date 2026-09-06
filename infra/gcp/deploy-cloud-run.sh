#!/usr/bin/env bash
# Deploy the Wambe scanner and API to Cloud Run from digest-pinned images.
#
# Renders infra/gcp/cloud-run/*.service.example.yaml outside the repository, shows the
# diff against the live service, and asks for confirmation before each mutating
# `gcloud run services replace`. Order: scanner (internal ingress, API-only invoker),
# then API (public), then IAM bindings and Cloud Scheduler jobs.
#
# Production promotion uses this same script with the production PROJECT_ID,
# DEPLOY_ENV=production and the *same* digests that passed staging; nothing is rebuilt.
#
# Required:
#   PROJECT_ID
#   DEPLOY_ENV            staging | production (Spring profile and evidence label)
#   API_IMAGE_DIGEST      from release-images.json (with or without the sha256: prefix)
#   SCANNER_IMAGE_DIGEST  from release-images.json
#   SUPABASE_URL          https://<ref>.supabase.co
#   FRONTEND_ORIGIN       exact Vercel origin, e.g. https://wambe-staging.vercel.app
# Optional:
#   REGION                default europe-west1
#   SUPABASE_JWKS_URI     default ${SUPABASE_URL}/auth/v1/.well-known/jwks.json
#   SUPABASE_ISSUER       default ${SUPABASE_URL}/auth/v1
#   VPC_NETWORK           default 'default'
#   VPC_SUBNETWORK        default 'default'
#   ASSUME_YES=1          skip confirmation prompts
#   SKIP_SCHEDULER=1      do not create Cloud Scheduler jobs
set -euo pipefail

PROJECT_ID="${PROJECT_ID:?Set PROJECT_ID}"
DEPLOY_ENV="${DEPLOY_ENV:?Set DEPLOY_ENV to staging or production}"
REGION="${REGION:-europe-west1}"
API_IMAGE_DIGEST="${API_IMAGE_DIGEST:?Set API_IMAGE_DIGEST from release-images.json}"
SCANNER_IMAGE_DIGEST="${SCANNER_IMAGE_DIGEST:?Set SCANNER_IMAGE_DIGEST from release-images.json}"
SUPABASE_URL="${SUPABASE_URL:?Set SUPABASE_URL}"
FRONTEND_ORIGIN="${FRONTEND_ORIGIN:?Set FRONTEND_ORIGIN to the exact Vercel origin}"
SUPABASE_JWKS_URI="${SUPABASE_JWKS_URI:-${SUPABASE_URL%/}/auth/v1/.well-known/jwks.json}"
SUPABASE_ISSUER="${SUPABASE_ISSUER:-${SUPABASE_URL%/}/auth/v1}"
VPC_NETWORK="${VPC_NETWORK:-default}"
VPC_SUBNETWORK="${VPC_SUBNETWORK:-default}"

if [[ "$DEPLOY_ENV" != "staging" && "$DEPLOY_ENV" != "production" ]]; then
  echo "DEPLOY_ENV must be staging or production." >&2
  exit 1
fi
API_IMAGE_DIGEST="${API_IMAGE_DIGEST#sha256:}"
SCANNER_IMAGE_DIGEST="${SCANNER_IMAGE_DIGEST#sha256:}"
for digest in "$API_IMAGE_DIGEST" "$SCANNER_IMAGE_DIGEST"; do
  if [[ ! "$digest" =~ ^[a-f0-9]{64}$ ]]; then
    echo "Digest '$digest' is not a 64-character lowercase hex sha256 digest. Never deploy a tag." >&2
    exit 1
  fi
done
for origin in "$SUPABASE_URL" "$FRONTEND_ORIGIN"; do
  if [[ ! "$origin" =~ ^https://[A-Za-z0-9.-]+$ ]]; then
    echo "'$origin' must be an exact https origin without path, port or trailing slash." >&2
    exit 1
  fi
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATE_DIR="${SCRIPT_DIR}/cloud-run"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

log() { printf '\n==> %s\n' "$*"; }
confirm() {
  if [[ "${ASSUME_YES:-0}" == "1" ]]; then return 0; fi
  printf '%s [y/N] ' "$1"
  read -r answer
  [[ "$answer" == "y" || "$answer" == "Y" ]]
}

gcloud config set project "$PROJECT_ID" --quiet >/dev/null
PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
API_SA="wambe-api@${PROJECT_ID}.iam.gserviceaccount.com"
SCANNER_SA="media-scanner@${PROJECT_ID}.iam.gserviceaccount.com"
SCHEDULER_SA="wambe-scheduler@${PROJECT_ID}.iam.gserviceaccount.com"
# The API compares SCHEDULER_SUBJECT with the ID token's `sub` claim, which for a
# service account is its numeric uniqueId, not its email.
SCHEDULER_SUBJECT="$(gcloud iam service-accounts describe "$SCHEDULER_SA" --format='value(uniqueId)')"
if [[ ! "$SCHEDULER_SUBJECT" =~ ^[0-9]+$ ]]; then
  echo "Could not resolve the numeric uniqueId of $SCHEDULER_SA; run bootstrap-project.sh first." >&2
  exit 1
fi

# Cloud Run deterministic URLs: https://<service>-<project-number>.<region>.run.app
API_ORIGIN="https://wambe-api-${PROJECT_NUMBER}.${REGION}.run.app"
SCANNER_ORIGIN="https://media-scanner-${PROJECT_NUMBER}.${REGION}.run.app"

log "Checking every referenced secret has an enabled version"
for secret in wambe-database-url wambe-database-user wambe-database-password \
              wambe-flyway-user wambe-flyway-password wambe-supabase-service-role \
              wambe-scanner-hmac; do
  if [[ -z "$(gcloud secrets versions list "$secret" --filter='state=ENABLED' --format='value(name)' --limit=1 2>/dev/null)" ]]; then
    echo "Secret '$secret' has no enabled version; run bootstrap-project.sh or add a version first." >&2
    exit 1
  fi
done

render() {
  local template="$1" output="$2"
  sed \
    -e "s|REGION-docker\.pkg\.dev/PROJECT_ID/|${REGION}-docker.pkg.dev/${PROJECT_ID}/|" \
    -e "s|@sha256:API_IMAGE_DIGEST|@sha256:${API_IMAGE_DIGEST}|" \
    -e "s|@sha256:SCANNER_IMAGE_DIGEST|@sha256:${SCANNER_IMAGE_DIGEST}|" \
    -e "s|serviceAccountName: API_SERVICE_ACCOUNT|serviceAccountName: ${API_SA}|" \
    -e "s|serviceAccountName: SCANNER_SERVICE_ACCOUNT|serviceAccountName: ${SCANNER_SA}|" \
    -e "s|\"network\":\"VPC_NETWORK\"|\"network\":\"${VPC_NETWORK}\"|" \
    -e "s|\"subnetwork\":\"VPC_SUBNETWORK\"|\"subnetwork\":\"${VPC_SUBNETWORK}\"|" \
    -e "s|value: staging$|value: ${DEPLOY_ENV}|" \
    -e "s|value: SUPABASE_URL$|value: \"${SUPABASE_URL}\"|" \
    -e "s|value: SUPABASE_JWKS_URI$|value: \"${SUPABASE_JWKS_URI}\"|" \
    -e "s|value: SUPABASE_ISSUER$|value: \"${SUPABASE_ISSUER}\"|" \
    -e "s|value: FRONTEND_ORIGIN$|value: \"${FRONTEND_ORIGIN}\"|" \
    -e "s|value: API_ORIGIN$|value: \"${API_ORIGIN}\"|" \
    -e "s|value: PRIVATE_SCANNER_ORIGIN$|value: \"${SCANNER_ORIGIN}\"|" \
    -e "s|value: SCHEDULER_SERVICE_ACCOUNT_UNIQUE_ID$|value: \"${SCHEDULER_SUBJECT}\"|" \
    "$template" > "$output"
  if grep -nE 'PROJECT_ID|IMAGE_DIGEST|_SERVICE_ACCOUNT(_UNIQUE_ID)?$|VPC_|_ORIGIN$|value: SUPABASE_' "$output"; then
    echo "Unrendered placeholder remains in $output" >&2
    exit 1
  fi
}

log "Rendering $DEPLOY_ENV manifests into $WORK_DIR (deleted on exit, never committed)"
render "${TEMPLATE_DIR}/media-scanner.service.example.yaml" "${WORK_DIR}/media-scanner.yaml"
render "${TEMPLATE_DIR}/wambe-api.service.example.yaml" "${WORK_DIR}/wambe-api.yaml"

show_diff() {
  local service="$1" rendered="$2"
  if gcloud run services describe "$service" --region="$REGION" --format=export > "${WORK_DIR}/${service}.current.yaml" 2>/dev/null; then
    log "Diff for '$service' (left: live, right: rendered)"
    diff -u "${WORK_DIR}/${service}.current.yaml" "$rendered" || true
  else
    log "'$service' does not exist yet; rendered manifest:"
    cat "$rendered"
  fi
}

wait_for_health() {
  local url="$1" attempt
  for attempt in $(seq 1 12); do
    if [[ "$(curl -s -o /dev/null -w '%{http_code}' "$url" || true)" == "200" ]]; then
      echo "  healthy: $url"
      return 0
    fi
    sleep 5
  done
  echo "  not healthy after 60s: $url (check the revision logs before continuing)" >&2
  return 1
}

# ---- Scanner: internal ingress, the API service account is the only invoker ---------
show_diff media-scanner "${WORK_DIR}/media-scanner.yaml"
if confirm "Apply media-scanner to $PROJECT_ID/$REGION ($DEPLOY_ENV)?"; then
  gcloud run services replace "${WORK_DIR}/media-scanner.yaml" --region="$REGION" --quiet
  gcloud run services add-iam-policy-binding media-scanner --region="$REGION" \
    --member="serviceAccount:${API_SA}" --role="roles/run.invoker" --quiet >/dev/null
  if gcloud run services get-iam-policy media-scanner --region="$REGION" --format=json | grep -q allUsers; then
    echo "media-scanner has a public invoker binding; remove it before continuing (MEDIA-006)." >&2
    exit 1
  fi
  echo "media-scanner ingress: $(gcloud run services describe media-scanner --region="$REGION" --format='value(metadata.annotations."run.googleapis.com/ingress")')"
  echo "media-scanner revision: $(gcloud run services describe media-scanner --region="$REGION" --format='value(status.latestReadyRevisionName)')"
else
  echo "Skipped media-scanner."
fi

# ---- API: public ingress; Flyway migrates on startup with the migration principal ----
show_diff wambe-api "${WORK_DIR}/wambe-api.yaml"
if confirm "Apply wambe-api to $PROJECT_ID/$REGION ($DEPLOY_ENV)? Flyway migrates the database on startup."; then
  gcloud run services replace "${WORK_DIR}/wambe-api.yaml" --region="$REGION" --quiet
  gcloud run services add-iam-policy-binding wambe-api --region="$REGION" \
    --member="allUsers" --role="roles/run.invoker" --quiet >/dev/null
  gcloud run services add-iam-policy-binding wambe-api --region="$REGION" \
    --member="serviceAccount:${SCHEDULER_SA}" --role="roles/run.invoker" --quiet >/dev/null
  echo "wambe-api revision: $(gcloud run services describe wambe-api --region="$REGION" --format='value(status.latestReadyRevisionName)')"
  wait_for_health "${API_ORIGIN}/actuator/health" || true
else
  echo "Skipped wambe-api."
fi

# ---- Cloud Scheduler: OIDC with the dedicated service account, exact API audience ----
if [[ "${SKIP_SCHEDULER:-0}" != "1" ]]; then
  ensure_job() {
    local name="$1" schedule="$2" path="$3"
    if gcloud scheduler jobs describe "$name" --location="$REGION" >/dev/null 2>&1; then
      echo "  $name exists"
      return
    fi
    gcloud scheduler jobs create http "$name" \
      --location="$REGION" \
      --schedule="$schedule" \
      --time-zone="Etc/UTC" \
      --uri="${API_ORIGIN}${path}" \
      --http-method=POST \
      --oidc-service-account-email="$SCHEDULER_SA" \
      --oidc-token-audience="$API_ORIGIN" \
      --quiet
  }
  if confirm "Create Cloud Scheduler jobs (scan dispatch every minute, retention daily 02:15 UTC)?"; then
    log "Cloud Scheduler jobs"
    ensure_job wambe-scan-dispatch "* * * * *" "/api/v1/internal/jobs/scan-dispatch"
    ensure_job wambe-retention "15 2 * * *" "/api/v1/internal/jobs/retention"
    echo "Force one run each and record the redacted result as RECOVERY-002/RECOVERY-003 evidence:"
    echo "  gcloud scheduler jobs run wambe-scan-dispatch --location=$REGION"
    echo "  gcloud scheduler jobs run wambe-retention --location=$REGION"
  fi
fi

cat <<SUMMARY

$DEPLOY_ENV deploy finished for $PROJECT_ID.

  API origin (NEXT_PUBLIC_WAMBE_API_URL = this + /api/v1):  $API_ORIGIN
  Scanner origin (internal only):                            $SCANNER_ORIGIN
  Frontend origin allowed by CORS:                           $FRONTEND_ORIGIN
  Scheduler subject (service account uniqueId):              $SCHEDULER_SUBJECT
  API image digest:                                        sha256:$API_IMAGE_DIGEST
  Scanner image digest:                                      sha256:$SCANNER_IMAGE_DIGEST

Record revision names and digests in the evidence file. Roll a service back
independently with:
  gcloud run services update-traffic <service> --region=$REGION --to-revisions=<previous-revision>=100
SUMMARY
