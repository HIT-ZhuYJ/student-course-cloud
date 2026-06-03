param(
    [string]$JenkinsWar = "D:\Jenkins\jenkins.war",
    [string]$JenkinsHome = "$env:USERPROFILE\.jenkins",
    [int]$HttpPort = 8090,
    [string]$JobName = "student-course-cloud-cicd",
    [string]$RepoPath = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
)

$ErrorActionPreference = "Stop"

function Test-PortOpen {
    param([int]$Port)
    $client = New-Object Net.Sockets.TcpClient
    try {
        $client.Connect("127.0.0.1", $Port)
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "===== $Message =====" -ForegroundColor Cyan
}

if (-not (Test-Path $JenkinsWar)) {
    throw "Jenkins war was not found: $JenkinsWar"
}

if (-not (Test-Path $JenkinsHome)) {
    New-Item -ItemType Directory -Path $JenkinsHome | Out-Null
}

$jenkinsfile = Join-Path $RepoPath "Jenkinsfile"
if (-not (Test-Path $jenkinsfile)) {
    throw "Jenkinsfile was not found: $jenkinsfile"
}

Write-Step "Create or update Jenkins pipeline job"
$jobDir = Join-Path $JenkinsHome "jobs\$JobName"
New-Item -ItemType Directory -Force -Path $jobDir | Out-Null

$pipelineScript = Get-Content $jenkinsfile -Raw -Encoding UTF8
$pipelineScript = $pipelineScript.Replace("D:/demo/YunSoftwareSystem", ($RepoPath -replace "\\", "/"))
$encodedScript = [System.Security.SecurityElement]::Escape($pipelineScript)

$configXml = @"
<?xml version='1.1' encoding='UTF-8'?>
<flow-definition plugin="workflow-job">
  <actions/>
  <description>Student Course Cloud CI/CD pipeline. It builds current code, rebuilds images, redeploys to Kubernetes and runs validation.</description>
  <keepDependencies>false</keepDependencies>
  <properties/>
  <definition class="org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition" plugin="workflow-cps">
    <script>$encodedScript</script>
    <sandbox>true</sandbox>
  </definition>
  <triggers/>
  <disabled>false</disabled>
</flow-definition>
"@

Set-Content -Path (Join-Path $jobDir "config.xml") -Value $configXml -Encoding UTF8

Write-Step "Start Jenkins if needed"
if (Test-PortOpen -Port $HttpPort) {
    Write-Host "Jenkins is already reachable on http://localhost:$HttpPort"
} else {
    $env:JENKINS_HOME = $JenkinsHome
    $args = @(
        "-jar", $JenkinsWar,
        "--httpPort=$HttpPort",
        "--httpListenAddress=0.0.0.0"
    )
    Start-Process -FilePath "java" -ArgumentList $args -WorkingDirectory (Split-Path $JenkinsWar) -WindowStyle Hidden

    $deadline = (Get-Date).AddMinutes(3)
    do {
        Start-Sleep -Seconds 5
        if (Test-PortOpen -Port $HttpPort) {
            Write-Host "Jenkins started: http://localhost:$HttpPort" -ForegroundColor Green
            break
        }
    } while ((Get-Date) -lt $deadline)

    if (-not (Test-PortOpen -Port $HttpPort)) {
        throw "Jenkins did not become reachable on port $HttpPort within the timeout."
    }
}

Write-Host ""
Write-Host "Pipeline job path: $jobDir"
Write-Host "Open: http://localhost:$HttpPort/job/$JobName/"
Write-Host "Use Build with Parameters, set SSH_PASSWORD to the VM password, then run the pipeline."
