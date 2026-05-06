param(
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "123888",
    [string]$JwtSecret = "local-demo-secret-change-me",
    [string]$ConfigServerUrl = "http://localhost:8888",
    [string]$SpringProfile = "local",
    [switch]$SkipBuild,
    [switch]$SkipFrontend
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$LogDir = Join-Path $Root "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Test-CommandExists {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Command '$Name' was not found. Please install it or add it to PATH."
    }
}

function Get-ServiceJar {
    param([string]$ModuleName)
    $targetDir = Join-Path $Root "$ModuleName\target"
    $jar = Get-ChildItem -Path $targetDir -Filter "$ModuleName-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike "*.original" } |
        Select-Object -First 1

    if (-not $jar) {
        throw "Jar file for $ModuleName was not found. Run mvn clean package -DskipTests first."
    }

    return $jar.FullName
}

function Start-JavaService {
    param(
        [string]$ModuleName,
        [int]$DelaySeconds = 3
    )

    $jar = Get-ServiceJar $ModuleName
    $outLog = Join-Path $LogDir "$ModuleName.out.log"
    $errLog = Join-Path $LogDir "$ModuleName.err.log"
    $pidFile = Join-Path $LogDir "$ModuleName.pid"

    Write-Host "Starting $ModuleName ..."
    $process = Start-Process -FilePath "java" `
        -ArgumentList @("-jar", $jar) `
        -WorkingDirectory $Root `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog `
        -WindowStyle Hidden `
        -PassThru
    Set-Content -Path $pidFile -Value $process.Id
    Write-Host "  pid: $($process.Id), out: $outLog, err: $errLog"
    Start-Sleep -Seconds $DelaySeconds
}

Test-CommandExists "java"
Test-CommandExists "mvn"

$env:MYSQL_USER = $MysqlUser
$env:MYSQL_PASSWORD = $MysqlPassword
$env:JWT_SECRET = $JwtSecret
$env:CONFIG_SERVER_URL = $ConfigServerUrl

if (-not $SkipBuild) {
    Write-Host "Building backend modules ..."
    Push-Location $Root
    mvn clean package -DskipTests
    Pop-Location
}

$env:SPRING_PROFILES_ACTIVE = "native"
Start-JavaService "config-service" 10
$env:SPRING_PROFILES_ACTIVE = $SpringProfile
Start-JavaService "eureka-service" 12
Start-JavaService "student-service" 4
Start-JavaService "course-service" 4
Start-JavaService "teacher-service" 4
Start-JavaService "enrollment-service" 4
Start-JavaService "gateway-service" 6

if (-not $SkipFrontend) {
    Test-CommandExists "npm.cmd"
    $frontendDir = Join-Path $Root "frontend"
    $frontendOutLog = Join-Path $LogDir "frontend.out.log"
    $frontendErrLog = Join-Path $LogDir "frontend.err.log"
    $frontendPidFile = Join-Path $LogDir "frontend.pid"

    if (-not (Test-Path (Join-Path $frontendDir "node_modules"))) {
        Write-Host "Installing frontend dependencies ..."
        Push-Location $frontendDir
        npm.cmd install
        Pop-Location
    }

    Write-Host "Starting frontend ..."
    $frontendProcess = Start-Process -FilePath "npm.cmd" `
        -ArgumentList @("run", "dev") `
        -WorkingDirectory $frontendDir `
        -RedirectStandardOutput $frontendOutLog `
        -RedirectStandardError $frontendErrLog `
        -WindowStyle Hidden `
        -PassThru
    Set-Content -Path $frontendPidFile -Value $frontendProcess.Id
    Write-Host "  pid: $($frontendProcess.Id), out: $frontendOutLog, err: $frontendErrLog"
}

Write-Host ""
Write-Host "Startup commands submitted."
Write-Host "Config:   http://localhost:8888"
Write-Host "Eureka:   http://localhost:8761"
Write-Host "Gateway:  http://localhost:8080"
Write-Host "Frontend: http://localhost:5173"
Write-Host ""
Write-Host "PID files are in $LogDir."
Write-Host "Use scripts\win\stop-all.ps1 to stop services started by this script."
