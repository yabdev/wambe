param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("staging", "production")]
    [string]$Environment
)

$ErrorActionPreference = "Stop"
$missing = [System.Collections.Generic.List[string]]::new()
$invalid = [System.Collections.Generic.List[string]]::new()

function Test-ExactHttpsOrigin {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $false
    }
    $uri = $null
    if (-not [Uri]::TryCreate($Value, [UriKind]::Absolute, [ref]$uri)) {
        return $false
    }
    return $uri.Scheme -eq "https" -and
        [string]::IsNullOrEmpty($uri.UserInfo) -and
        $uri.IsDefaultPort -and
        $uri.AbsolutePath -eq "/" -and
        [string]::IsNullOrEmpty($uri.Query) -and
        [string]::IsNullOrEmpty($uri.Fragment)
}

$required = @(
    "SPRING_PROFILES_ACTIVE",
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
    "SCANNER_AUDIENCE",
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
if ($scannerSecret -in @(
        "local-scanner-secret-change-me",
        "test-scanner-secret-change-me"
    ) -or
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

$activeProfile = [Environment]::GetEnvironmentVariable("SPRING_PROFILES_ACTIVE")
if ($activeProfile -ne $Environment) {
    $invalid.Add("SPRING_PROFILES_ACTIVE must equal $Environment")
}

$scannerUrl = [Environment]::GetEnvironmentVariable("SCANNER_URL")
$scannerAudience = [Environment]::GetEnvironmentVariable("SCANNER_AUDIENCE")
if (-not (Test-ExactHttpsOrigin $scannerUrl)) {
    $invalid.Add("SCANNER_URL must be an exact HTTPS service origin")
}
if (-not (Test-ExactHttpsOrigin $scannerAudience)) {
    $invalid.Add("SCANNER_AUDIENCE must be an exact HTTPS service origin")
}
if (-not [string]::IsNullOrWhiteSpace($scannerUrl) -and
    -not [string]::IsNullOrWhiteSpace($scannerAudience) -and
    $scannerUrl.TrimEnd("/") -cne $scannerAudience.TrimEnd("/")) {
    $invalid.Add("SCANNER_AUDIENCE must exactly match SCANNER_URL")
}

foreach ($originName in @("SUPABASE_URL", "API_BASE_URL", "PUBLIC_BASE_URL", "NEXT_PUBLIC_SITE_URL")) {
    if (-not (Test-ExactHttpsOrigin (
            [Environment]::GetEnvironmentVariable($originName)))) {
        $invalid.Add("$originName must be an exact HTTPS origin")
    }
}

if ([Environment]::GetEnvironmentVariable("NEXT_PUBLIC_DEMO_MODE") -eq "true") {
    $invalid.Add("NEXT_PUBLIC_DEMO_MODE must not be true")
}

$cors = [Environment]::GetEnvironmentVariable("CORS_ALLOWED_ORIGINS")
if (-not (Test-ExactHttpsOrigin $cors) -or $cors -match "localhost|127\.0\.0\.1|\*") {
    $invalid.Add("CORS_ALLOWED_ORIGINS must contain only exact deployed HTTPS origins")
}

if ($missing.Count -gt 0) {
    Write-Error "$Environment is missing required variables: $($missing -join ', ')"
}
if ($invalid.Count -gt 0) {
    Write-Error "$Environment has invalid deployment settings: $($invalid -join '; ')"
}

Write-Host "$Environment deployment environment passed static validation."
