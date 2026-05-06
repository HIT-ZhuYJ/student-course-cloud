param(
    [switch]$KeepFrontend
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$LogDir = Join-Path $Root "logs"

$services = @(
    "gateway-service",
    "enrollment-service",
    "teacher-service",
    "course-service",
    "student-service",
    "eureka-service",
    "config-service"
)

if (-not $KeepFrontend) {
    $services = @("frontend") + $services
}

foreach ($service in $services) {
    $pidFile = Join-Path $LogDir "$service.pid"
    if (-not (Test-Path $pidFile)) {
        Write-Host "$service pid file not found, skip."
        continue
    }

    $pidValue = Get-Content $pidFile | Select-Object -First 1
    if (-not $pidValue) {
        Write-Host "$service pid file is empty, skip."
        continue
    }

    $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
    if ($process) {
        Write-Host "Stopping $service pid=$pidValue ..."
        Stop-Process -Id $pidValue -Force
    } else {
        Write-Host "$service pid=$pidValue is not running."
    }
}

Write-Host "Stop command completed."
