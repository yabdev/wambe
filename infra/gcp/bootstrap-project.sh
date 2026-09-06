#!/usr/bin/env bash
# Bootstrap an isolated Wambe project on Google Cloud (staging first; production later
# with a different PROJECT_ID).
#
# Idempotent and safe to re-run. It creates project plumbing only: APIs, the Artifact
# Registry repository, runtime/publisher service accounts, Workload Identity Federation
# for GitHub Actions, Secret Manager secrets, and the GitHub environment variables
# consumed by `.github/workflows/release-images.yml`.
#
# It deploys nothing. Cloud Run services are created by `deploy-cloud-run.sh` after the
# release workflow has published digest-pinned images.
#
# Run from Google Cloud Shell (gcloud pre-installed) or any shell with `gcloud` and `gh`
# authenticated. Billing must already be linked to the project: Cloud Run and Artifact
# Registry refuse to enable without it, so the script stops there rather than paying.
#
# Required:
#   PROJECT_ID            existing project id (staging and production must differ)
# Optional:
#   REGION                default europe-west1
#   GITHUB_REPO           default yabdev/wambe
#   GITHUB_ENVIRONMENT    default staging (release-images.yml only publishes to staging)
#   SKIP_SECRET_PROMPTS   set to 1 to create secrets without adding versions
#   VPC_NETWORK           default 'default' (checked only)
set -euo pipefail

PROJECT_ID="${PROJECT_ID:?Set PROJECT_ID to the target project id}"
REGION="${REGION:-europe-west1}"
GITHUB_REPO="${GITHUB_REPO:-yabdev/wambe}"
GITHUB_ENVIRONMENT="${GITHUB_ENVIRONMENT:-staging}"
AR_REPO="wambe"
POOL="github"
PROVIDER="github"

log() { printf '\n==> %s\n' "$*"; }

log "Selecting project $PROJECT_ID"
gcloud config set project "$PROJECT_ID" --quiet >/dev/null
PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"

log "Checking billing"
if billing_enabled="$(gcloud billing projects describe "$PROJECT_ID" --format='value(billingEnabled)' 2>/dev/null)"; then
  if [[ "$billing_enabled" != "True" ]]; then
    echo "Billing is not linked to $PROJECT_ID. Link a billing account in the console, then re-run." >&2
    echo "Stopping before any paid action (Product Owner decision required)." >&2
    exit 1
  fi
else
  echo "  Could not query billing state; API enablement below fails safely if billing is missing."
fi

log "Enabling APIs"
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  cloudscheduler.googleapis.com \
  iam.googleapis.com \
  iamcredentials.googleapis.com \
  sts.googleapis.com \
  compute.googleapis.com \
  --quiet

log "Artifact Registry repository '$AR_REPO' in $REGION"
if ! gcloud artifacts repositories describe "$AR_REPO" --location="$REGION" >/dev/null 2>&1; then
  gcloud artifacts repositories create "$AR_REPO" \
    --repository-format=docker \
    --location="$REGION" \
    --description="Wambe digest-pinned release images"
fi

ensure_service_account() {
  local name="$1" display="$2"
  local email="${name}@${PROJECT_ID}.iam.gserviceaccount.com"
  if ! gcloud iam service-accounts describe "$email" >/dev/null 2>&1; then
    gcloud iam service-accounts create "$name" --display-name="$display"
  fi
}

log "Service accounts"
ensure_service_account wambe-api "Wambe API runtime"
ensure_service_account media-scanner "Wambe media scanner runtime"
ensure_service_account wambe-scheduler "Wambe Cloud Scheduler invoker"
ensure_service_account github-publisher "GitHub Actions image publisher"
API_SA="wambe-api@${PROJECT_ID}.iam.gserviceaccount.com"
SCANNER_SA="media-scanner@${PROJECT_ID}.iam.gserviceaccount.com"
SCHEDULER_SA="wambe-scheduler@${PROJECT_ID}.iam.gserviceaccount.com"
PUBLISHER_SA="github-publisher@${PROJECT_ID}.iam.gserviceaccount.com"

log "Workload Identity Federation for GitHub Actions ($GITHUB_REPO only)"
if ! gcloud iam workload-identity-pools describe "$POOL" --location=global >/dev/null 2>&1; then
  gcloud iam workload-identity-pools create "$POOL" --location=global --display-name="GitHub Actions"
fi
if ! gcloud iam workload-identity-pools providers describe "$PROVIDER" \
      --location=global --workload-identity-pool="$POOL" >/dev/null 2>&1; then
  gcloud iam workload-identity-pools providers create-oidc "$PROVIDER" \
    --location=global \
    --workload-identity-pool="$POOL" \
    --display-name="GitHub" \
    --issuer-uri="https://token.actions.githubusercontent.com" \
    --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner" \
    --attribute-condition="assertion.repository == '${GITHUB_REPO}'"
fi
WIF_PROVIDER="projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL}/providers/${PROVIDER}"
gcloud iam service-accounts add-iam-policy-binding "$PUBLISHER_SA" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL}/attribute.repository/${GITHUB_REPO}" \
  --quiet >/dev/null
gcloud artifacts repositories add-iam-policy-binding "$AR_REPO" \
  --location="$REGION" \
  --member="serviceAccount:${PUBLISHER_SA}" \
  --role="roles/artifactregistry.writer" \
  --quiet >/dev/null

log "Secret Manager secrets (names match infra/gcp/cloud-run/*.service.example.yaml)"
API_SECRETS="wambe-database-url wambe-database-user wambe-database-password wambe-flyway-user wambe-flyway-password wambe-supabase-service-role wambe-scanner-hmac"
for secret in $API_SECRETS; do
  if ! gcloud secrets describe "$secret" >/dev/null 2>&1; then
    gcloud secrets create "$secret" --replication-policy=automatic --quiet
  fi
  gcloud secrets add-iam-policy-binding "$secret" \
    --member="serviceAccount:${API_SA}" \
    --role="roles/secretmanager.secretAccessor" \
    --quiet >/dev/null
done
gcloud secrets add-iam-policy-binding wambe-scanner-hmac \
  --member="serviceAccount:${SCANNER_SA}" \
  --role="roles/secretmanager.secretAccessor" \
  --quiet >/dev/null

secret_has_version() {
  [[ -n "$(gcloud secrets versions list "$1" --filter='state=ENABLED' --format='value(name)' --limit=1)" ]]
}

if ! secret_has_version wambe-scanner-hmac; then
  log "Generating a rotated scanner HMAC secret (64 characters, never printed)"
  openssl rand -base64 48 | tr -d '\n' | gcloud secrets versions add wambe-scanner-hmac --data-file=- >/dev/null
fi

if [[ "${SKIP_SECRET_PROMPTS:-0}" != "1" ]]; then
  log "Supabase values from infra/supabase/README.md (input hidden; blank skips a secret)"
  for secret in wambe-database-url wambe-database-user wambe-database-password \
                wambe-flyway-user wambe-flyway-password wambe-supabase-service-role; do
    if secret_has_version "$secret"; then
      echo "  $secret already has a version"
      continue
    fi
    printf '  %s: ' "$secret"
    IFS= read -rs value
    echo
    if [[ -n "$value" ]]; then
      printf '%s' "$value" | gcloud secrets versions add "$secret" --data-file=- >/dev/null
    fi
    unset value
  done
fi

log "Direct VPC egress prerequisites"
VPC_NETWORK="${VPC_NETWORK:-default}"
if gcloud compute networks describe "$VPC_NETWORK" >/dev/null 2>&1; then
  echo "  network '$VPC_NETWORK' exists; deploy-cloud-run.sh uses subnetwork '${VPC_SUBNETWORK:-default}' in $REGION"
else
  echo "  network '$VPC_NETWORK' is missing; create it or set VPC_NETWORK/VPC_SUBNETWORK before deploying" >&2
fi

log "GitHub '$GITHUB_ENVIRONMENT' environment variables for release-images.yml"
set_github_variable() {
  local name="$1" value="$2"
  if command -v gh >/dev/null 2>&1; then
    gh variable set "$name" --repo "$GITHUB_REPO" --env "$GITHUB_ENVIRONMENT" --body "$value"
  else
    printf '  set manually on the GitHub %s environment: %s=%s\n' "$GITHUB_ENVIRONMENT" "$name" "$value"
  fi
}
set_github_variable GCP_PROJECT_ID "$PROJECT_ID"
set_github_variable GCP_REGION "$REGION"
set_github_variable GCP_WORKLOAD_IDENTITY_PROVIDER "$WIF_PROVIDER"
set_github_variable GCP_ARTIFACT_PUBLISHER_SERVICE_ACCOUNT "$PUBLISHER_SA"

cat <<SUMMARY

Bootstrap complete for $PROJECT_ID ($PROJECT_NUMBER) in $REGION.

  API runtime SA:        $API_SA
  Scanner runtime SA:    $SCANNER_SA
  Scheduler SA:          $SCHEDULER_SA
  Publisher SA:          $PUBLISHER_SA
  WIF provider:          $WIF_PROVIDER
  Image repository:      ${REGION}-docker.pkg.dev/${PROJECT_ID}/${AR_REPO}

Next: dispatch the 'Release images' workflow on the release SHA, download the
release-images-staging-<sha> artifact, then run infra/gcp/deploy-cloud-run.sh with the
recorded digests. A secret without a version blocks deployment; add one with
  printf '%s' '<value>' | gcloud secrets versions add <name> --data-file=-
SUMMARY
