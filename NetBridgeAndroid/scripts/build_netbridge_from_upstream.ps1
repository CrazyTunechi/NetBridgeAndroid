$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Work = if ($env:NETBRIDGE_WORK_DIR) { $env:NETBRIDGE_WORK_DIR } else { Join-Path $Root 'build-source' }
$Out = if ($env:NETBRIDGE_OUTPUT_DIR) { $env:NETBRIDGE_OUTPUT_DIR } else { Join-Path $Root 'out' }
$Repo = 'https://github.com/MushroomSquad/tg-ws-proxy-android.git'
$Tag = 'v1.2.1'
$Checkout = Join-Path $Work 'tg-ws-proxy-android'
New-Item -ItemType Directory -Force -Path $Work, $Out | Out-Null
if (-not (Test-Path (Join-Path $Checkout '.git'))) {
    git clone --depth 1 --branch $Tag $Repo $Checkout
    if ($LASTEXITCODE -ne 0) { throw 'git clone failed' }
} else {
    Write-Host "Using existing checkout: $Checkout"
}
$ActualTag = git -C $Checkout describe --tags --exact-match HEAD 2>$null
if ($LASTEXITCODE -ne 0 -or $ActualTag.Trim() -ne $Tag) {
    throw "Checkout must be exactly at upstream tag $Tag (found: $ActualTag). Move/remove $Checkout and rerun for a clean checkout."
}
$UpstreamSha = git -C $Checkout rev-parse HEAD
if ($LASTEXITCODE -ne 0) { throw 'Could not resolve upstream commit SHA' }
python (Join-Path $Root 'scripts/prepare_netbridge.py') $Checkout
if ($LASTEXITCODE -ne 0) { throw 'Branding patch failed' }
Push-Location $Checkout
try {
    .\gradlew.bat assembleDebug --no-daemon
    if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed' }
    $Apk = Join-Path $Out 'NetBridge-debug.apk'
    Copy-Item 'app/build/outputs/apk/debug/app-debug.apk' $Apk -Force
    @("Upstream tag: $Tag", "Upstream commit: $UpstreamSha", 'Build variant: debug') | Set-Content (Join-Path $Out 'BUILD-INFO.txt')
    $Hash = Get-FileHash $Apk -Algorithm SHA256
    "$($Hash.Hash.ToLower())  NetBridge-debug.apk" | Set-Content (Join-Path $Out 'SHA256SUMS.txt')
} finally { Pop-Location }
Write-Host "Build complete: $(Join-Path $Out 'NetBridge-debug.apk')"
Write-Host "Upstream source: $Tag ($UpstreamSha)"
Write-Host 'Debug-signed derivative; device testing is still required.'
