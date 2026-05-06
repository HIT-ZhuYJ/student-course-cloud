param(
    [string]$MysqlHost = "localhost",
    [int]$MysqlPort = 3306,
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "123888"
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$SqlDir = Join-Path $Root "scripts\sql"

$files = @(
    "00-create-databases.sql",
    "01-student-service.sql",
    "02-course-service.sql",
    "03-teacher-service.sql",
    "04-enrollment-service.sql",
    "05-demo-data.sql"
)

function Test-CommandExists {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Command '$Name' was not found. Please install it or add it to PATH."
    }
}

Test-CommandExists "mysql"

foreach ($file in $files) {
    $path = Join-Path $SqlDir $file
    if (-not (Test-Path $path)) {
        throw "SQL file not found: $path"
    }

    Write-Host "Executing $file ..."
    Get-Content -Raw -Encoding UTF8 $path | & mysql -h $MysqlHost -P $MysqlPort -u $MysqlUser "-p$MysqlPassword"

    if ($LASTEXITCODE -ne 0) {
        throw "Failed to execute $file"
    }
}

Write-Host "Database initialization completed."
