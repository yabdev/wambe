param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("staging", "production")]
    [string]$Environment
)

$ErrorActionPreference = "Stop"
$missing = [System.Collections.Generic.List[string]]::new()
$invalid = [System.Collections.Generic.List[string]]::new()

$required = @(
    "DATABASE_URL",
    "DATABASE_USER",
    "DATABASE_PASSWORD",
    "FLYWAY_USER",
    "FLYWAY_PASSWORD",
    "SUPABASE_URL",
    "SUPABASE_SERVICE_ROLE_KEY",
    "SUPABASE_JWKS_URI",
    "SUPABASE_ISSUER",
    "CORS_ALLOWED_ORIGINS",
    "PUBLIC_BASE_URL",
    "API_BASE_URL",
    "SCANNER_URL",
    "SCANNER_HMAC_SECRET",
    "SCHEDULER_JWKS_URI",
    "SCHEDULER_ISSUER",
    "SCHEDULER_AUDIENCE",
    "SCHEDULER_SUBJECT",
    "NEXT_PUBLIC_SUPABASE_URL",
    "NEXT_PUBLIC_SUPABASE_ANON_KEY",
    "NEXT_PUBLIC_WAMBE_API_URL",
    "NEXT_PUBLIC_GOOGLE_MAPS_API_KEY",
    "NEXT_PUBLIC_SITE_URL"
)

foreach ($name in $required) {
    $value = [Environment]::GetEnvironmentVariable($name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        $missing.Add($name)
    }
}

$scannerSecret = [Environment]::GetEnvironmentVariable("SCANNER_HMAC_SECRET")
if ($scannerSecret -eq "local-scanner-secret-change-me" -or
    (-not [string]::IsNullOrWhiteSpace($scannerSecret) -and $scannerSecret.Length -lt 32)) {
    $invalid.Add("SCANNER_HMAC_SECRET must be rotated and contain at least 32 characters")
}

if (-not [string]::IsNullOrWhiteSpace(
        [Environment]::GetEnvironmentVariable("INTERNAL_JOB_KEY"))) {
    $invalid.Add("INTERNAL_JOB_KEY must be empty; deployed jobs use Google OIDC")
}

if ([Environment]::GetEnvironmentVariable("STORAGE_TYPE") -ne "supabase") {
    $invalid.Add("STORAGE_TYPE must equal supabase")
}

if ([Environment]::GetEnvironmentVariable("NEXT_PUBLIC_DEMO_MODE") -eq "true") {
    $invalid.Add("NEXT_PUBLIC_DEMO_MODE must not be true")
}

$cors = [Environment]::GetEnvironmentVariable("CORS_ALLOWED_ORIGINS")
if ($cors -match "localhost|127\.0\.0\.1|\*") {
    $invalid.Add("CORS_ALLOWED_ORIGINS must contain only exact deployed HTTPS origins")
}

if ($missing.Count -gt 0) {
    Write-Error "$Environment is missing required variables: $($missing -join ', ')"
}
if ($invalid.Count -gt 0) {
    Write-Error "$Environment has invalid deployment settings: $($invalid -join '; ')"
}

Write-Host "$Environment deployment environment passed static validation."
