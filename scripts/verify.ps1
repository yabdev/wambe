param(
    [switch]$IncludeBrowser,
    [switch]$IncludeImages
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

function Invoke-Checked {
    param(
        [string]$Name,
        [string]$Directory,
        [scriptblock]$Command
    )

    Write-Host "`n==> $Name"
    Push-Location $Directory
    try {
        & $Command
        if ($LASTEXITCODE -ne 0) {
            throw "$Name failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

Invoke-Checked "OpenAPI lint" $root {
    npx --yes @redocly/cli lint "docs/sdlc/stories/US-002-create-publish-event/contracts/openapi-v1.yaml"
}
Invoke-Checked "API verification" "$root/services/wambe-api" {
    .\mvnw.cmd clean verify
}
Invoke-Checked "Scanner verification" "$root/services/media-scanner" {
    .\mvnw.cmd clean verify
}
Invoke-Checked "API client" "$root/packages/wambe-api-client" {
    npm ci
    if ($LASTEXITCODE -eq 0) { npm run build }
}
Invoke-Checked "Web release checks" "$root/apps/web" {
    # Prefer a clean install in CI. Locally, a running Next.js process can lock the
    # Windows SWC binary and make `npm ci` fail with EPERM; skip reinstall when asked.
    if ($env:SKIP_NPM_CI -eq "true") {
        if (-not (Test-Path "node_modules\.bin\eslint.cmd")) {
            throw "SKIP_NPM_CI=true but apps/web/node_modules is incomplete; stop Next.js and run npm ci"
        }
    } else {
        npm ci
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
    npm run lint
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    npm run typecheck
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    npm test
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    npm run build
}

if ($IncludeBrowser) {
    Invoke-Checked "Browser and accessibility tests" "$root/apps/web" {
        npx playwright install chromium
        if ($LASTEXITCODE -eq 0) { npm run test:e2e }
    }
}

if ($IncludeImages) {
    Invoke-Checked "API image" $root {
        docker build -f services/wambe-api/Dockerfile -t wambe-api:verify .
    }
    Invoke-Checked "Scanner image" $root {
        docker build -f services/media-scanner/Dockerfile -t media-scanner:verify .
    }
}

Write-Host "`nAll requested verification gates passed."
