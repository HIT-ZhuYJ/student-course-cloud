pipeline {
  agent any

  options {
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '10'))
    disableConcurrentBuilds()
  }

  parameters {
    string(name: 'LOCAL_WORKSPACE', defaultValue: 'D:/demo/YunSoftwareSystem', description: 'Repository path on the Jenkins machine. Leave empty when using SCM checkout.')
    string(name: 'MASTER_HOST', defaultValue: '192.168.40.129', description: 'Kubernetes master SSH host.')
    string(name: 'WORKER_HOSTS', defaultValue: '192.168.40.130,192.168.40.131', description: 'Comma-separated Kubernetes worker SSH hosts.')
    string(name: 'SSH_USER', defaultValue: 'zyj', description: 'SSH user for Kubernetes nodes.')
    string(name: 'SSH_PASSWORD', defaultValue: '123456', description: 'SSH password for Kubernetes nodes.')
    booleanParam(name: 'APPLY_DEMO_DATA', defaultValue: true, description: 'Apply demo SQL data.')
    booleanParam(name: 'RUN_SMOKE_TEST', defaultValue: true, description: 'Run API smoke test after deployment.')
    booleanParam(name: 'RUN_DEGRADATION_TEST', defaultValue: false, description: 'Run load-balancing and degradation test.')
    booleanParam(name: 'RESTART_OBSERVABILITY', defaultValue: true, description: 'Restart Prometheus, Grafana, Elasticsearch, Kibana and Logstash.')
    booleanParam(name: 'SKIP_IMAGE_UPLOAD', defaultValue: false, description: 'Skip image archive upload/import when images are already on worker nodes.')
  }

  stages {
    stage('Prepare Workspace') {
      steps {
        script {
          if (params.LOCAL_WORKSPACE?.trim()) {
            echo "Using local repository: ${params.LOCAL_WORKSPACE}"
          } else {
            checkout scm
          }
        }
      }
    }

    stage('Build And Redeploy') {
      steps {
        script {
          if (!params.SSH_PASSWORD?.trim()) {
            error 'SSH_PASSWORD is required. Open "Build with Parameters" and set it to the Kubernetes VM SSH password.'
          }

          def repoDir = params.LOCAL_WORKSPACE?.trim() ?: env.WORKSPACE
          dir(repoDir) {
            withEnv([
              "SCMS_MASTER_HOST=${params.MASTER_HOST}",
              "SCMS_WORKER_HOSTS=${params.WORKER_HOSTS}",
              "SCMS_SSH_USER=${params.SSH_USER}",
              "SCMS_SSH_PASSWORD=${params.SSH_PASSWORD}",
              "SCMS_APPLY_DEMO_DATA=${params.APPLY_DEMO_DATA}",
              "SCMS_RUN_SMOKE_TEST=${params.RUN_SMOKE_TEST}",
              "SCMS_RUN_DEGRADATION_TEST=${params.RUN_DEGRADATION_TEST}",
              "SCMS_RESTART_OBSERVABILITY=${params.RESTART_OBSERVABILITY}",
              "SCMS_SKIP_IMAGE_UPLOAD=${params.SKIP_IMAGE_UPLOAD}"
            ]) {
              powershell '''
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($env:SCMS_SSH_PASSWORD)) {
    throw "SCMS_SSH_PASSWORD is empty. Re-run Build with Parameters and fill SSH_PASSWORD."
}

$redeployArgs = @(
    "-ExecutionPolicy", "Bypass",
    "-File", "scripts\\win\\redeploy-k8s.ps1",
    "-MasterHost", $env:SCMS_MASTER_HOST,
    "-WorkerHosts", $env:SCMS_WORKER_HOSTS,
    "-Namespace", "student-course",
    "-SshUser", $env:SCMS_SSH_USER,
    "-SshPassword", $env:SCMS_SSH_PASSWORD
)

if ($env:SCMS_APPLY_DEMO_DATA -eq "true") {
    $redeployArgs += "-ApplyDemoData"
}
if ($env:SCMS_RUN_SMOKE_TEST -eq "true") {
    $redeployArgs += "-RunSmokeTest"
}
if ($env:SCMS_RUN_DEGRADATION_TEST -eq "true") {
    $redeployArgs += "-RunDegradationTest"
}
if ($env:SCMS_RESTART_OBSERVABILITY -eq "true") {
    $redeployArgs += "-RestartObservability"
}
if ($env:SCMS_SKIP_IMAGE_UPLOAD -eq "true") {
    $redeployArgs += "-SkipImageUpload"
}

& powershell @redeployArgs
'''
            }
          }
        }
      }
    }
  }

  post {
    always {
      echo 'CI/CD pipeline finished. Check the stage log for build, deployment and validation details.'
    }
  }
}
