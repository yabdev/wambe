#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INCLUDE_BROWSER="${INCLUDE_BROWSER:-false}"
INCLUDE_IMAGES="${INCLUDE_IMAGES:-false}"

step() {
  printf '\n==> %s\n' "$1"
}

step "OpenAPI lint"
cd "$ROOT"
npx --yes @redocly/cli lint \
  docs/sdlc/stories/US-002-create-publish-event/contracts/openapi-v1.yaml

step "API verification"
cd "$ROOT/services/wambe-api"
./mvnw -B clean verify

step "Scanner verification"
cd "$ROOT/services/media-scanner"
./mvnw -B clean verify

step "API client"
cd "$ROOT/packages/wambe-api-client"
npm ci
npm run build

step "Web release checks"
cd "$ROOT/apps/web"
npm ci
npm run lint
npm run typecheck
npm test
npm run build

if [[ "$INCLUDE_BROWSER" == "true" ]]; then
  step "Browser and accessibility tests"
  npx playwright install chromium
  npm run test:e2e
fi

if [[ "$INCLUDE_IMAGES" == "true" ]]; then
  step "Container images"
  cd "$ROOT"
  docker build -f services/wambe-api/Dockerfile -t wambe-api:verify .
  docker build -f services/media-scanner/Dockerfile -t media-scanner:verify .
fi

printf '\nAll requested verification gates passed.\n'
