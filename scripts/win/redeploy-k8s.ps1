param(
    [string]$MasterHost = "192.168.40.129",
    [string[]]$WorkerHosts = @("192.168.40.130", "192.168.40.131"),
    [string]$SshUser = "zyj",
    [string]$SshPassword = "",
    [string]$Namespace = "student-course",
    [string]$RemoteRoot = "/tmp/scms-redeploy",
    [string]$MysqlRootPassword = "123888",
    [switch]$SkipBackendBuild,
    [switch]$SkipFrontendBuild,
    [switch]$SkipImageBuild,
    [switch]$SkipImageUpload,
    [switch]$ApplyDemoData,
    [switch]$RunSmokeTest,
    [switch]$RunDegradationTest,
    [switch]$RestartObservability,
    [switch]$KeepImageArchive
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$ImageArchive = Join-Path $Root "deploy-images.tar"
$WorkerHosts = @(
    $WorkerHosts |
        ForEach-Object { $_ -split "," } |
        ForEach-Object { $_.Trim() } |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
)

$Images = @(
    @{ Name = "yunsoftwaresystem-config-service:latest"; File = "config-service/Dockerfile" },
    @{ Name = "yunsoftwaresystem-eureka-service:latest"; File = "eureka-service/Dockerfile" },
    @{ Name = "yunsoftwaresystem-gateway-service:latest"; File = "gateway-service/Dockerfile" },
    @{ Name = "yunsoftwaresystem-student-service:latest"; File = "student-service/Dockerfile" },
    @{ Name = "yunsoftwaresystem-course-service:latest"; File = "course-service/Dockerfile" },
    @{ Name = "yunsoftwaresystem-teacher-service:latest"; File = "teacher-service/Dockerfile" },
    @{ Name = "yunsoftwaresystem-enrollment-service:latest"; File = "enrollment-service/Dockerfile" },
    @{ Name = "yunsoftwaresystem-frontend:latest"; File = "frontend/Dockerfile" }
)

$CoreDeployments = @(
    "config-service",
    "eureka-service",
    "student-service",
    "course-service",
    "teacher-service",
    "enrollment-service",
    "gateway-service",
    "frontend",
    "nginx"
)

$ObservabilityDeployments = @(
    "prometheus",
    "grafana",
    "elasticsearch",
    "kibana"
)

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "===== $Message =====" -ForegroundColor Cyan
}

function Test-CommandExists {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Command '$Name' was not found. Please install it or add it to PATH."
    }
}

function ConvertTo-PlainText {
    param([securestring]$Secure)
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function Ensure-Paramiko {
    Test-CommandExists "python"
    $check = & python -c "import paramiko" 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Python package 'paramiko' was not found. Installing it with pip ..."
        & python -m pip install --user paramiko
    }
}

function New-HelperScript {
    $helper = Join-Path ([System.IO.Path]::GetTempPath()) "scms-k8s-redeploy-helper.py"
    $code = @'
import base64
import json
import os
import posixpath
import stat
import sys
import time
import uuid

import paramiko


def connect(cfg):
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(
        hostname=cfg["host"],
        username=cfg["user"],
        password=cfg["password"],
        timeout=20,
        banner_timeout=20,
        auth_timeout=20,
    )
    return client


def mkdir_p(sftp, path):
    if not path or path == "/":
        return
    parts = []
    current = path
    while current not in ("", "/"):
        parts.append(current)
        current = posixpath.dirname(current)
    for item in reversed(parts):
        try:
            sftp.mkdir(item)
        except OSError:
            pass


def upload_file(cfg):
    client = connect(cfg)
    try:
        sftp = client.open_sftp()
        mkdir_p(sftp, posixpath.dirname(cfg["remote"]))
        started = time.time()

        def progress(sent, total):
            now = time.time()
            if now - progress.last > 15 or sent == total:
                print(f"upload {cfg['host']} {sent / 1024 / 1024:.1f}/{total / 1024 / 1024:.1f} MB", flush=True)
                progress.last = now

        progress.last = started
        sftp.put(cfg["local"], cfg["remote"], callback=progress)
        sftp.close()
    finally:
        client.close()


def upload_tree(cfg):
    client = connect(cfg)
    try:
        sftp = client.open_sftp()
        local_root = cfg["local"]
        remote_root = cfg["remote"]
        mkdir_p(sftp, remote_root)
        count = 0
        for root, _, files in os.walk(local_root):
            rel = os.path.relpath(root, local_root).replace("\\", "/")
            remote_dir = remote_root if rel == "." else posixpath.join(remote_root, rel)
            mkdir_p(sftp, remote_dir)
            for filename in files:
                local_path = os.path.join(root, filename)
                remote_path = posixpath.join(remote_dir, filename)
                sftp.put(local_path, remote_path)
                count += 1
        sftp.close()
        print(f"uploaded {count} files to {cfg['host']}:{remote_root}")
    finally:
        client.close()


def run_bash(cfg):
    client = connect(cfg)
    remote_script = f"/tmp/scms-redeploy-{uuid.uuid4().hex}.sh"
    try:
        sftp = client.open_sftp()
        script = cfg["script"].replace("\r\n", "\n")
        with sftp.file(remote_script, "w") as f:
            f.write(script)
        sftp.chmod(remote_script, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
        sftp.close()

        command = f"bash {remote_script}"
        if cfg.get("sudo"):
            command = f"sudo -S bash {remote_script}"

        stdin, stdout, stderr = client.exec_command(command, get_pty=bool(cfg.get("sudo")), timeout=cfg.get("timeout", 600))
        if cfg.get("sudo"):
            stdin.write(cfg["password"] + "\n")
            stdin.flush()

        while not stdout.channel.exit_status_ready():
            if stdout.channel.recv_ready():
                sys.stdout.write(stdout.channel.recv(65535).decode("utf-8", errors="replace"))
                sys.stdout.flush()
            if stderr.channel.recv_stderr_ready():
                sys.stderr.write(stderr.channel.recv_stderr(65535).decode("utf-8", errors="replace"))
                sys.stderr.flush()
            time.sleep(0.2)

        while stdout.channel.recv_ready():
            sys.stdout.write(stdout.channel.recv(65535).decode("utf-8", errors="replace"))
        while stderr.channel.recv_stderr_ready():
            sys.stderr.write(stderr.channel.recv_stderr(65535).decode("utf-8", errors="replace"))

        code = stdout.channel.recv_exit_status()
        if code != 0:
            raise SystemExit(code)
    finally:
        try:
            client.exec_command(f"rm -f {remote_script}", timeout=10)
        except Exception:
            pass
        client.close()


def main():
    op = sys.argv[1]
    if len(sys.argv) > 2:
        with open(sys.argv[2], "r", encoding="utf-8-sig") as f:
            cfg = json.load(f)
    else:
        cfg = json.load(sys.stdin)
    if op == "upload-file":
        upload_file(cfg)
    elif op == "upload-tree":
        upload_tree(cfg)
    elif op == "run-bash":
        run_bash(cfg)
    else:
        raise SystemExit(f"unknown op: {op}")


if __name__ == "__main__":
    main()
'@
    Set-Content -Path $helper -Value $code -Encoding UTF8
    return $helper
}

function Invoke-Helper {
    param(
        [string]$Operation,
        [hashtable]$Config,
        [string]$Helper
    )
    $json = $Config | ConvertTo-Json -Depth 20 -Compress
    $configFile = Join-Path $env:TEMP ("scms-helper-{0}.json" -f ([guid]::NewGuid().ToString("N")))
    try {
        Set-Content -Path $configFile -Value $json -Encoding UTF8
        & python $Helper $Operation $configFile
        if ($LASTEXITCODE -ne 0) {
            throw "Helper operation '$Operation' failed with exit code $LASTEXITCODE."
        }
    } finally {
        Remove-Item -Path $configFile -Force -ErrorAction SilentlyContinue
    }
}

function Invoke-RemoteBash {
    param(
        [string]$HostName,
        [string]$Script,
        [string]$Helper,
        [int]$TimeoutSeconds = 600,
        [switch]$Sudo
    )
    Invoke-Helper -Operation "run-bash" -Helper $Helper -Config @{
        host = $HostName
        user = $SshUser
        password = $SshPassword
        script = $Script
        timeout = $TimeoutSeconds
        sudo = [bool]$Sudo
    }
}

function Upload-File {
    param(
        [string]$HostName,
        [string]$Local,
        [string]$Remote,
        [string]$Helper
    )
    Invoke-Helper -Operation "upload-file" -Helper $Helper -Config @{
        host = $HostName
        user = $SshUser
        password = $SshPassword
        local = $Local
        remote = $Remote
    }
}

function Upload-Tree {
    param(
        [string]$HostName,
        [string]$Local,
        [string]$Remote,
        [string]$Helper
    )
    Invoke-Helper -Operation "upload-tree" -Helper $Helper -Config @{
        host = $HostName
        user = $SshUser
        password = $SshPassword
        local = $Local
        remote = $Remote
    }
}

function Invoke-JsonRequest {
    param(
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [string]$Token = ""
    )
    $headers = @{ Accept = "application/json" }
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers.Authorization = "Bearer $Token"
    }
    $params = @{
        Method = $Method
        Uri = $Url
        Headers = $headers
        UseBasicParsing = $true
        TimeoutSec = 30
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = ($Body | ConvertTo-Json -Depth 10)
    }

    try {
        return Invoke-WebRequest @params
    } catch {
        if ($_.Exception.Response) {
            return $_.Exception.Response
        }
        throw
    }
}

if ([string]::IsNullOrWhiteSpace($SshPassword)) {
    $securePassword = Read-Host "SSH password for $SshUser" -AsSecureString
    $SshPassword = ConvertTo-PlainText $securePassword
}

Write-Step "Preflight"
Test-CommandExists "mvn"
Test-CommandExists "npm.cmd"
Test-CommandExists "docker"
Ensure-Paramiko
$Helper = New-HelperScript
Write-Host "Workspace: $Root"
Write-Host "Master:    $MasterHost"
Write-Host "Workers:   $($WorkerHosts -join ', ')"

if (-not $SkipBackendBuild) {
    Write-Step "Build backend"
    Push-Location $Root
    mvn clean package
    Pop-Location
}

if (-not $SkipFrontendBuild) {
    Write-Step "Build frontend"
    Push-Location (Join-Path $Root "frontend")
    npm.cmd ci
    npm.cmd run build
    Pop-Location
}

if (-not $SkipImageBuild) {
    Write-Step "Build Docker images"
    Push-Location $Root
    foreach ($image in $Images) {
        Write-Host "Building $($image.Name) ..."
        docker build -t $image.Name -f $image.File .
    }
    Pop-Location
}

if (-not $SkipImageUpload) {
    Write-Step "Save Docker image archive"
    if (Test-Path $ImageArchive) {
        Remove-Item $ImageArchive -Force
    }
    $imageNames = $Images | ForEach-Object { $_.Name }
    docker save -o $ImageArchive @imageNames
    $archiveSize = [Math]::Round((Get-Item $ImageArchive).Length / 1MB, 1)
    Write-Host "Created $ImageArchive ($archiveSize MB)"

    Write-Step "Upload and import images on workers"
    foreach ($worker in $WorkerHosts) {
        $remoteArchive = "/tmp/scms-deploy-images.tar"
        Write-Host "Uploading image archive to $worker ..."
        Upload-File -HostName $worker -Local $ImageArchive -Remote $remoteArchive -Helper $Helper
        Write-Host "Importing images on $worker ..."
        Invoke-RemoteBash -HostName $worker -Helper $Helper -TimeoutSeconds 900 -Sudo -Script @"
set -e
ctr -n k8s.io images import $remoteArchive
crictl images | grep yunsoftwaresystem || true
"@
    }
}

Write-Step "Upload Kubernetes files to master"
Upload-Tree -HostName $MasterHost -Local (Join-Path $Root "k8s") -Remote "$RemoteRoot/k8s" -Helper $Helper
Upload-Tree -HostName $MasterHost -Local (Join-Path $Root "config-repo") -Remote "$RemoteRoot/config-repo" -Helper $Helper
Upload-Tree -HostName $MasterHost -Local (Join-Path $Root "scripts\sql") -Remote "$RemoteRoot/scripts/sql" -Helper $Helper

Write-Step "Apply Kubernetes resources and SQL upgrades"
$demoDataLine = ""
if ($ApplyDemoData) {
    $demoDataLine = "kubectl exec -i -n $Namespace mysql-0 -- mysql -uroot -p$MysqlRootPassword < $RemoteRoot/scripts/sql/05-demo-data.sql"
}
Invoke-RemoteBash -HostName $MasterHost -Helper $Helper -TimeoutSeconds 300 -Script @"
set -e
cd $RemoteRoot
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secrets.yaml
kubectl create configmap config-repo -n $Namespace --from-file=config-repo --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap mysql-init-sql -n $Namespace --from-file=scripts/sql --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap prometheus-conf -n $Namespace --from-file=prometheus.yml=k8s/files/prometheus.yml --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap grafana-datasource -n $Namespace --from-file=prometheus.yml=k8s/files/grafana-datasource.yml --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap grafana-dashboard-provider -n $Namespace --from-file=provider.yml=k8s/files/grafana-dashboard-provider.yml --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap grafana-dashboard-scms -n $Namespace --from-file=scms-dashboard.json=k8s/files/scms-dashboard.json --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap nginx-conf -n $Namespace --from-file=nginx.conf=k8s/files/nginx.conf --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap logstash-pipeline -n $Namespace --from-file=logstash.conf=k8s/files/logstash.conf --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f k8s/apps.yaml
kubectl apply -f k8s/frontend-nginx.yaml
kubectl apply -f k8s/observability.yaml
kubectl apply -f k8s/elk.yaml
kubectl exec -i -n $Namespace mysql-0 -- mysql -uroot -p$MysqlRootPassword < scripts/sql/06-upgrade-course-schedule-weeks.sql
$demoDataLine
"@

Write-Step "Restart core deployments"
$deploymentList = $CoreDeployments -join " "
Invoke-RemoteBash -HostName $MasterHost -Helper $Helper -TimeoutSeconds 1800 -Script @"
set -e
for d in $deploymentList; do
  echo "restart deployment/`$d"
  kubectl rollout restart deployment/`$d -n $Namespace
  kubectl rollout status deployment/`$d -n $Namespace --timeout=420s
done
"@

if ($RestartObservability) {
    Write-Step "Restart observability deployments"
    $obsList = $ObservabilityDeployments -join " "
    Invoke-RemoteBash -HostName $MasterHost -Helper $Helper -TimeoutSeconds 1200 -Script @"
set -e
for d in $obsList; do
  echo "restart deployment/`$d"
  kubectl rollout restart deployment/`$d -n $Namespace
  kubectl rollout status deployment/`$d -n $Namespace --timeout=420s
done
kubectl rollout restart daemonset/logstash -n $Namespace
kubectl rollout status daemonset/logstash -n $Namespace --timeout=300s
"@
}

Write-Step "Cluster health"
Invoke-RemoteBash -HostName $MasterHost -Helper $Helper -TimeoutSeconds 180 -Script @"
set -e
kubectl get pods -n $Namespace -o wide
kubectl get deploy,statefulset,daemonset -n $Namespace
kubectl get endpoints -n $Namespace
"@

Write-Step "External endpoint checks"
$externalChecks = @(
    "http://$MasterHost:30080/",
    "http://$MasterHost:30080/api/courses",
    "http://$MasterHost:30081/actuator/health",
    "http://$MasterHost:30061/",
    "http://$MasterHost:30090/-/healthy",
    "http://$MasterHost:30300/api/health",
    "http://$MasterHost:30601/api/status"
)
foreach ($url in $externalChecks) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 15
        Write-Host "[OK] $url -> HTTP $($response.StatusCode)"
    } catch {
        Write-Warning "[FAIL] $url -> $($_.Exception.Message)"
        throw
    }
}

if ($RunSmokeTest) {
    Write-Step "API smoke test"
    Invoke-RemoteBash -HostName $MasterHost -Helper $Helper -TimeoutSeconds 120 -Script @"
set -e
pkill -f 'kubectl port-forward.*1808' || true
nohup kubectl port-forward -n $Namespace --address 0.0.0.0 svc/student-service 18081:8081 >/tmp/scms-pf-student.log 2>&1 &
nohup kubectl port-forward -n $Namespace --address 0.0.0.0 svc/course-service 18082:8082 >/tmp/scms-pf-course.log 2>&1 &
nohup kubectl port-forward -n $Namespace --address 0.0.0.0 svc/teacher-service 18083:8083 >/tmp/scms-pf-teacher.log 2>&1 &
sleep 5
cat /tmp/scms-pf-student.log /tmp/scms-pf-course.log /tmp/scms-pf-teacher.log
"@
    try {
        & powershell -ExecutionPolicy Bypass -File (Join-Path $Root "scripts\win\api-smoke-test.ps1") `
            -BaseUrl "http://$MasterHost:30080" `
            -StudentServiceUrl "http://$MasterHost:18081" `
            -CourseServiceUrl "http://$MasterHost:18082" `
            -TeacherServiceUrl "http://$MasterHost:18083"
        if ($LASTEXITCODE -ne 0) {
            throw "API smoke test failed with exit code $LASTEXITCODE."
        }
    } finally {
        Invoke-RemoteBash -HostName $MasterHost -Helper $Helper -TimeoutSeconds 30 -Script "pkill -f 'kubectl port-forward.*1808' || true"
    }
}

if ($RunDegradationTest) {
    Write-Step "Load balancing and degradation test"
    try {
        Invoke-RemoteBash -HostName $MasterHost -Helper $Helper -TimeoutSeconds 420 -Script @"
set -e
kubectl scale deployment/course-service -n $Namespace --replicas=2
kubectl rollout status deployment/course-service -n $Namespace --timeout=300s
kubectl get endpoints -n $Namespace course-service
"@
        for ($i = 1; $i -le 5; $i++) {
            $response = Invoke-WebRequest -Uri "http://$MasterHost:30080/api/courses" -UseBasicParsing -TimeoutSec 15
            Write-Host "[OK] course list attempt $i -> HTTP $($response.StatusCode)"
        }

        Invoke-RemoteBash -HostName $MasterHost -Helper $Helper -TimeoutSeconds 420 -Script @"
set -e
kubectl scale deployment/course-service -n $Namespace --replicas=0
kubectl rollout status deployment/course-service -n $Namespace --timeout=180s || true
kubectl get endpoints -n $Namespace course-service
"@
        $login = Invoke-JsonRequest -Method "POST" -Url "http://$MasterHost:30080/api/auth/login" -Body @{ username = "student001"; password = "student123" }
        $token = (($login.Content | ConvertFrom-Json).data.token)
        $degraded = Invoke-JsonRequest -Method "POST" -Url "http://$MasterHost:30080/api/enrollments" -Token $token -Body @{ studentId = 1; courseId = 1 }
        $body = ""
        try {
            $reader = [System.IO.StreamReader]::new($degraded.GetResponseStream())
            $body = $reader.ReadToEnd()
        } catch {
            $body = $degraded.Content
        }
        Write-Host "Degradation response: HTTP $([int]$degraded.StatusCode) $body"
        if ([int]$degraded.StatusCode -ne 503 -or $body -notmatch "course-service is unavailable") {
            throw "Expected course-service degradation response was not returned."
        }
    } finally {
        Invoke-RemoteBash -HostName $MasterHost -Helper $Helper -TimeoutSeconds 420 -Script @"
set -e
kubectl scale deployment/course-service -n $Namespace --replicas=1
kubectl rollout status deployment/course-service -n $Namespace --timeout=360s
kubectl get pods -n $Namespace -l app=course-service -o wide
"@
    }
}

Write-Step "Cleanup"
if (-not $KeepImageArchive -and (Test-Path $ImageArchive)) {
    Remove-Item $ImageArchive -Force
    Write-Host "Removed $ImageArchive"
}
if (-not $SkipImageUpload) {
    foreach ($worker in $WorkerHosts) {
        Invoke-RemoteBash -HostName $worker -Helper $Helper -TimeoutSeconds 60 -Script "rm -f /tmp/scms-deploy-images.tar"
    }
}

Write-Host ""
Write-Host "Kubernetes redeploy completed successfully." -ForegroundColor Green
Write-Host "Frontend: http://$MasterHost:30080"
Write-Host "Eureka:   http://$MasterHost:30061"
