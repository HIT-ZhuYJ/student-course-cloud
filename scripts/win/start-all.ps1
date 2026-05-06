param(
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "123888",
    [string]$JwtSecret = "local-demo-secret-change-me",
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
    $log = Join-Path $LogDir "$ModuleName.log"

    Write-Host "Starting $ModuleName ..."
    $job = Start-Job -Name $ModuleName -ScriptBlock {
        param($RootPath, $JarPath, $LogPath, $User, $Password, $Secret)
        Set-Location $RootPath
        $env:MYSQL_USER = $User
        $env:MYSQL_PASSWORD = $Password
        $env:JWT_SECRET = $Secret
        & java -jar $JarPath *> $LogPath
    } -ArgumentList $Root, $jar, $log, $MysqlUser, $MysqlPassword, $JwtSecret

    Write-Host "  job id: $($job.Id), log: $log"
    Start-Sleep -Seconds $DelaySeconds
}

Test-CommandExists "java"
Test-CommandExists "mvn"

if (-not $SkipBuild) {
    Write-Host "Building backend modules ..."
    Push-Location $Root
    mvn clean package -DskipTests
    Pop-Location
}

Start-JavaService "eureka-service" 12
Start-JavaService "student-service" 4
Start-JavaService "course-service" 4
Start-JavaService "teacher-service" 4
Start-JavaService "enrollment-service" 4
Start-JavaService "gateway-service" 6

if (-not $SkipFrontend) {
    Test-CommandExists "npm.cmd"
    $frontendDir = Join-Path $Root "frontend"
    $frontendLog = Join-Path $LogDir "frontend.log"

    if (-not (Test-Path (Join-Path $frontendDir "node_modules"))) {
        Write-Host "Installing frontend dependencies ..."
        Push-Location $frontendDir
        npm.cmd install
        Pop-Location
    }

    Write-Host "Starting frontend ..."
    $frontendJob = Start-Job -Name "frontend" -ScriptBlock {
        param($FrontendPath, $LogPath)
        Set-Location $FrontendPath
        & npm.cmd run dev *> $LogPath
    } -ArgumentList $frontendDir, $frontendLog
    Write-Host "  job id: $($frontendJob.Id), log: $frontendLog"
}

Write-Host ""
Write-Host "Startup commands submitted."
Write-Host "Eureka:   http://localhost:8761"
Write-Host "Gateway:  http://localhost:8080"
Write-Host "Frontend: http://localhost:5173"
Write-Host ""
Write-Host "Use Get-Job to view background jobs."
Write-Host "Use Stop-Job *; Remove-Job * to stop jobs started by this script."
