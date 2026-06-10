param(
    [string]$MasterHost = "192.168.40.129",
    [string]$SshUser = "zyj",
    [string]$SshPassword,
    [string]$JenkinsWar = "D:\Jenkins\jenkins.war",
    [string]$PluginSource = "$env:USERPROFILE\.jenkins\plugins",
    [string]$RemoteJenkinsHome = "/home/zyj/jenkins-home",
    [string]$RemoteJenkinsWar = "/home/zyj/jenkins.war",
    [int]$HttpPort = 8080,
    [string]$JobName = "student-course-cloud-main-release-cicd",
    [string]$RepoPath = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path,
    [switch]$SkipPluginUpload
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "===== $Message =====" -ForegroundColor Cyan
}

function ConvertTo-Base64Utf8 {
    param([string]$Value)
    return [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($Value))
}

if (-not $SshPassword) {
    $secure = Read-Host "SSH password for $SshUser@$MasterHost" -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $SshPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

if (-not (Test-Path $JenkinsWar)) {
    throw "Jenkins war was not found: $JenkinsWar"
}

$jenkinsfile = Join-Path $RepoPath "Jenkinsfile"
if (-not (Test-Path $jenkinsfile)) {
    throw "Jenkinsfile was not found: $jenkinsfile"
}

$pipelineScript = Get-Content $jenkinsfile -Raw -Encoding UTF8

$env:SCMS_MASTER_HOST = $MasterHost
$env:SCMS_SSH_USER = $SshUser
$env:SCMS_SSH_PASSWORD = $SshPassword
$env:SCMS_JENKINS_WAR = (Resolve-Path $JenkinsWar).Path
$env:SCMS_PLUGIN_SOURCE = $PluginSource
$env:SCMS_SKIP_PLUGIN_UPLOAD = if ($SkipPluginUpload) { "true" } else { "false" }
$env:SCMS_REMOTE_HOME = $RemoteJenkinsHome
$env:SCMS_REMOTE_WAR = $RemoteJenkinsWar
$env:SCMS_HTTP_PORT = [string]$HttpPort
$env:SCMS_JOB_NAME = $JobName
$env:SCMS_PIPELINE_B64 = ConvertTo-Base64Utf8 $pipelineScript

$python = @'
import base64
import html
import os
import pathlib
import posixpath
import sys

try:
    import paramiko
except ImportError:
    raise SystemExit("Python package 'paramiko' is required. Install it with: python -m pip install --user paramiko")

host = os.environ["SCMS_MASTER_HOST"]
user = os.environ["SCMS_SSH_USER"]
password = os.environ["SCMS_SSH_PASSWORD"]
local_war = pathlib.Path(os.environ["SCMS_JENKINS_WAR"])
plugin_source = pathlib.Path(os.environ["SCMS_PLUGIN_SOURCE"])
skip_plugins = os.environ.get("SCMS_SKIP_PLUGIN_UPLOAD") == "true"
remote_home = os.environ["SCMS_REMOTE_HOME"]
remote_war = os.environ["SCMS_REMOTE_WAR"]
http_port = os.environ["SCMS_HTTP_PORT"]
job_name = os.environ["SCMS_JOB_NAME"]
pipeline = base64.b64decode(os.environ["SCMS_PIPELINE_B64"]).decode("utf-8")

def mkdir_p(sftp, path):
    parts = []
    cur = path
    while cur not in ("", "/"):
        parts.append(cur)
        cur = posixpath.dirname(cur)
    for part in reversed(parts):
        try:
            sftp.mkdir(part)
        except OSError:
            pass

job_xml = """<?xml version='1.1' encoding='UTF-8'?>
<flow-definition plugin="workflow-job">
  <actions/>
  <description>Student Course Cloud CI/CD pipeline running on k8s-master. It checks out main, runs tests, builds images, pushes the registry, deploys to Kubernetes, validates, and updates the fixed release branch.</description>
  <keepDependencies>false</keepDependencies>
  <properties/>
  <definition class="org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition" plugin="workflow-cps">
    <script>{}</script>
    <sandbox>true</sandbox>
  </definition>
  <triggers/>
  <disabled>false</disabled>
</flow-definition>
""".format(html.escape(pipeline))

jenkins_config_xml = """<?xml version='1.1' encoding='UTF-8'?>
<hudson>
  <disabledAdministrativeMonitors/>
  <numExecutors>2</numExecutors>
  <mode>NORMAL</mode>
  <useSecurity>false</useSecurity>
  <authorizationStrategy class="hudson.security.AuthorizationStrategy$Unsecured"/>
  <securityRealm class="hudson.security.SecurityRealm$None"/>
  <disableRememberMe>false</disableRememberMe>
  <projectNamingStrategy class="jenkins.model.ProjectNamingStrategy$DefaultProjectNamingStrategy"/>
  <workspaceDir>${JENKINS_HOME}/workspace/${ITEM_FULL_NAME}</workspaceDir>
  <buildsDir>${ITEM_ROOTDIR}/builds</buildsDir>
  <viewsTabBar class="hudson.views.DefaultViewsTabBar"/>
  <myViewsTabBar class="hudson.views.DefaultMyViewsTabBar"/>
  <clouds/>
  <quietPeriod>5</quietPeriod>
  <scmCheckoutRetryCount>0</scmCheckoutRetryCount>
  <views>
    <hudson.model.AllView>
      <owner class="hudson" reference="../../.."/>
      <name>all</name>
      <filterExecutors>false</filterExecutors>
      <filterQueue>false</filterQueue>
      <properties class="hudson.model.View$PropertyList"/>
    </hudson.model.AllView>
  </views>
  <primaryView>all</primaryView>
  <slaveAgentPort>-1</slaveAgentPort>
  <label></label>
  <nodeProperties/>
  <globalNodeProperties/>
</hudson>
"""

service = f"""[Unit]
Description=Jenkins on k8s-master
After=network.target

[Service]
Type=simple
User={user}
Group={user}
Environment=JENKINS_HOME={remote_home}
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
Environment=PATH=/opt/node20/bin:/usr/local/bin:/usr/bin:/bin
EnvironmentFile=-/home/{user}/jenkins.env
WorkingDirectory=/home/{user}
ExecStart=/usr/bin/java -Djenkins.install.runSetupWizard=false -jar {remote_war} --httpPort={http_port} --httpListenAddress=0.0.0.0
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
"""

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(hostname=host, username=user, password=password, timeout=20, banner_timeout=20, auth_timeout=20)
sftp = ssh.open_sftp()
for directory in [remote_home, remote_home + "/plugins", remote_home + "/jobs/" + job_name]:
    mkdir_p(sftp, directory)

print(f"Uploading Jenkins war to {remote_war}")
sftp.put(str(local_war), remote_war)

if not skip_plugins:
    plugins = sorted(plugin_source.glob("*.jpi")) if plugin_source.exists() else []
    print(f"Uploading {len(plugins)} Jenkins plugin files")
    for idx, plugin in enumerate(plugins, 1):
        if idx == 1 or idx == len(plugins) or idx % 10 == 0:
            print(f"  plugin {idx}/{len(plugins)} {plugin.name}")
        sftp.put(str(plugin), remote_home + "/plugins/" + plugin.name)

with sftp.file(remote_home + "/config.xml", "w") as f:
    f.write(jenkins_config_xml)
with sftp.file(remote_home + "/jobs/" + job_name + "/config.xml", "w") as f:
    f.write(job_xml)
with sftp.file("/tmp/jenkins-master.service", "w") as f:
    f.write(service)

env_path = f"/home/{user}/jenkins.env"
try:
    sftp.stat(env_path)
except OSError:
    with sftp.file(env_path, "w") as f:
        f.write("GITHUB_USER=HIT-ZhuYJ\n# GITHUB_TOKEN=fill_your_github_pat_here\n")
    sftp.chmod(env_path, 0o600)
sftp.close()

cmd = f"""set -e
printf '%s\\n' '{password}' | sudo -S mv /tmp/jenkins-master.service /etc/systemd/system/jenkins-master.service
printf '%s\\n' '{password}' | sudo -S systemctl daemon-reload
printf '%s\\n' '{password}' | sudo -S systemctl enable --now jenkins-master.service
printf '%s\\n' '{password}' | sudo -S systemctl restart jenkins-master.service
sleep 8
systemctl is-active jenkins-master.service
"""
stdin, stdout, stderr = ssh.exec_command(cmd, get_pty=True, timeout=120)
out = stdout.read().decode("utf-8", "replace")
err = stderr.read().decode("utf-8", "replace")
rc = stdout.channel.recv_exit_status()
ssh.close()
print(out)
if err.strip():
    print(err, file=sys.stderr)
raise SystemExit(rc)
'@

$temp = New-TemporaryFile
try {
    Set-Content -Path $temp -Value $python -Encoding UTF8
    Write-Step "Deploy Jenkins on k8s-master"
    python $temp
} finally {
    Remove-Item -Force $temp -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "Jenkins URL: http://$MasterHost`:$HttpPort/job/$JobName/"
Write-Host "Credentials required inside Jenkins:"
Write-Host "  - k8s-node-ssh: SSH username/password for Kubernetes nodes"
Write-Host "  - harbor-credentials: registry username/password"
Write-Host "  - github-release-credentials: GitHub username/PAT for release branch push"
Write-Host "Optional environment file on master: /home/$SshUser/jenkins.env"
