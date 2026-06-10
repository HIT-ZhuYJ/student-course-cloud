pipeline {
  agent any

  options {
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '10'))
    disableConcurrentBuilds()
  }

  environment {
    PATH = "/opt/node20/bin:/usr/local/bin:/usr/bin:/bin:${env.PATH}"
    NPM_CONFIG_REGISTRY = 'https://registry.npmmirror.com'
    REPO_URL = 'https://github.com/HIT-ZhuYJ/student-course-cloud.git'
    SOURCE_BRANCH = 'main'
    GITHUB_COM_RESOLVE_CANDIDATES = 'github.com:443:140.82.114.4 github.com:443:140.82.113.4 github.com:443:140.82.121.4 github.com:443:20.205.243.166'
    MASTER_HOST = '192.168.40.129'
    NAMESPACE = 'student-course'
    IMAGE_REGISTRY = '192.168.40.129:5000'
    IMAGE_PROJECT = 'student-course'
    IMAGE_PULL_SECRET = 'scms-registry-secret'
    RELEASE_BRANCH = 'release/k8s-deployed'
  }

  stages {
    stage('Checkout main') {
      steps {
        sh '''#!/usr/bin/env bash
set -euo pipefail
find "$WORKSPACE" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
git config --global http.version HTTP/1.1
git config --global --unset-all http.curloptResolve || true
for resolve in $GITHUB_COM_RESOLVE_CANDIDATES; do
  for i in 1 2; do
    echo "Clone attempt $i with $resolve"
    if git -c "http.curloptResolve=$resolve" clone --depth 1 --branch "$SOURCE_BRANCH" "$REPO_URL" "$WORKSPACE"; then
      git rev-parse --short HEAD
      git status --short
      exit 0
    fi
    find "$WORKSPACE" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
    sleep 8
  done
done
exit 1
'''
      }
    }

    stage('Prepare image tag') {
      steps {
        script {
          env.SHORT_COMMIT = sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true).trim()
          env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.SHORT_COMMIT}"
          echo "Image tag: ${env.IMAGE_TAG}"
        }
      }
    }

    stage('Backend mvn test') {
      steps {
        sh 'mvn -U test'
      }
    }

    stage('Frontend build test') {
      steps {
        dir('frontend') {
          sh 'npm ci --registry=https://registry.npmmirror.com && npm run build'
        }
      }
    }

    stage('Package jars and prepare images') {
      steps {
        sh '''#!/usr/bin/env bash
set -euo pipefail
mvn -s docker/maven/settings.xml -DskipTests package
rm -rf ci-artifacts
mkdir -p ci-artifacts/frontend-dist
for module in config-service eureka-service gateway-service student-service course-service teacher-service enrollment-service; do
  cp "$module/target/$module-0.0.1-SNAPSHOT.jar" "ci-artifacts/$module.jar"
done
cp -R frontend/dist/. ci-artifacts/frontend-dist/
write_service_dockerfile() {
  module="$1"
  port="$2"
  cat > "$module/Dockerfile.ci" <<EOF
FROM 192.168.40.129:5000/student-course/maven:3.9.9-eclipse-temurin-17
WORKDIR /app
RUN mkdir -p /logs
COPY ci-artifacts/${module}.jar /app/app.jar
EOF
  if [ "$module" = "config-service" ]; then
    echo "COPY config-repo /app/config-repo" >> "$module/Dockerfile.ci"
  fi
  cat >> "$module/Dockerfile.ci" <<EOF
EXPOSE ${port}
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
EOF
}
write_service_dockerfile config-service 8888
write_service_dockerfile eureka-service 8761
write_service_dockerfile gateway-service 8080
write_service_dockerfile student-service 8081
write_service_dockerfile course-service 8082
write_service_dockerfile teacher-service 8083
write_service_dockerfile enrollment-service 8084
cat > frontend/Dockerfile.ci <<'EOF'
FROM 192.168.40.129:5000/student-course/maven:3.9.9-eclipse-temurin-17
WORKDIR /app
COPY ci-artifacts/frontend-dist/ /app/
RUN cat > /tmp/StaticServer.java <<'JAVA'
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class StaticServer {
  private static final Path ROOT = Path.of("/app").toAbsolutePath().normalize();
  public static void main(String[] args) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
    server.createContext("/", StaticServer::handle);
    server.start();
  }
  private static void handle(HttpExchange exchange) throws IOException {
    String rawPath = exchange.getRequestURI().getPath();
    Path target = ROOT.resolve(rawPath.substring(1)).normalize();
    if (!target.startsWith(ROOT) || Files.isDirectory(target) || !Files.exists(target)) {
      target = ROOT.resolve("index.html");
    }
    String name = target.getFileName().toString();
    String type = name.endsWith(".js") ? "application/javascript" :
      name.endsWith(".css") ? "text/css" :
      name.endsWith(".html") ? "text/html; charset=utf-8" :
      "application/octet-stream";
    byte[] body = Files.readAllBytes(target);
    exchange.getResponseHeaders().set("Content-Type", type);
    exchange.sendResponseHeaders(200, body.length);
    try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
  }
}
JAVA
RUN javac /tmp/StaticServer.java
EXPOSE 80
CMD ["java", "-cp", "/tmp", "StaticServer"]
EOF
'''
      }
    }

    stage('Build and push images') {
      steps {
        withCredentials([
          usernamePassword(credentialsId: 'k8s-node-ssh', usernameVariable: 'K8S_SSH_USER', passwordVariable: 'K8S_SSH_PASSWORD'),
          usernamePassword(credentialsId: 'harbor-credentials', usernameVariable: 'REGISTRY_USERNAME', passwordVariable: 'REGISTRY_PASSWORD')
        ]) {
          sh '''#!/usr/bin/env bash
set -euo pipefail
sudo_refresh() { printf '%s\n' "$K8S_SSH_PASSWORD" | sudo -S -v >/dev/null; }
sudo_refresh
printf '%s\n' "$REGISTRY_PASSWORD" | sudo -n nerdctl -n k8s.io --insecure-registry login "$IMAGE_REGISTRY" -u "$REGISTRY_USERNAME" --password-stdin
images=(
  "config-service|config-service|8888"
  "eureka-service|eureka-service|8761"
  "gateway-service|gateway-service|8080"
  "student-service|student-service|8081"
  "course-service|course-service|8082"
  "teacher-service|teacher-service|8083"
  "enrollment-service|enrollment-service|8084"
  "frontend|frontend|80"
)
for item in "${images[@]}"; do
  module="${item%%|*}"
  rest="${item#*|}"
  container="${rest%%|*}"
  image="$IMAGE_REGISTRY/$IMAGE_PROJECT/$module:$IMAGE_TAG"
  latest_image="$IMAGE_REGISTRY/$IMAGE_PROJECT/$module:latest"
  dockerfile="$module/Dockerfile.ci"
  echo "Building ${image} from ${dockerfile}"
  sudo -n nerdctl -n k8s.io --insecure-registry build -t "$image" -f "$dockerfile" .
  sudo -n nerdctl -n k8s.io --insecure-registry tag "$image" "$latest_image"
  echo "Pushing ${image}"
  sudo -n nerdctl -n k8s.io --insecure-registry push "$image"
  echo "Pushing ${latest_image}"
  sudo -n nerdctl -n k8s.io --insecure-registry push "$latest_image"
done
'''
        }
      }
    }

    stage('Deploy to Kubernetes') {
      steps {
        withCredentials([
          usernamePassword(credentialsId: 'harbor-credentials', usernameVariable: 'REGISTRY_USERNAME', passwordVariable: 'REGISTRY_PASSWORD')
        ]
        ) {
          sh '''#!/usr/bin/env bash
set -euo pipefail
kubectl delete pod -n "$NAMESPACE" --field-selector=status.phase=Failed || true
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/metrics-server.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/storage.yaml
kubectl create configmap config-repo -n "$NAMESPACE" --from-file=config-repo --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap mysql-init-sql -n "$NAMESPACE" --from-file=scripts/sql --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap prometheus-conf -n "$NAMESPACE" --from-file=prometheus.yml=k8s/files/prometheus.yml --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap grafana-datasource -n "$NAMESPACE" --from-file=prometheus.yml=k8s/files/grafana-datasource.yml --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap grafana-dashboard-provider -n "$NAMESPACE" --from-file=provider.yml=k8s/files/grafana-dashboard-provider.yml --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap grafana-dashboard-scms -n "$NAMESPACE" --from-file=scms-dashboard.json=k8s/files/scms-dashboard.json --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap nginx-conf -n "$NAMESPACE" --from-file=nginx.conf=k8s/files/nginx.conf --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap logstash-pipeline -n "$NAMESPACE" --from-file=logstash.conf=k8s/files/logstash.conf --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f k8s/mysql.yaml
kubectl rollout status statefulset/mysql -n "$NAMESPACE" --timeout=420s
kubectl create secret docker-registry "$IMAGE_PULL_SECRET" -n "$NAMESPACE" \
  --docker-server="$IMAGE_REGISTRY" \
  --docker-username="$REGISTRY_USERNAME" \
  --docker-password="$REGISTRY_PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -
cat > /tmp/scms-serviceaccount-patch.json <<EOF
{"imagePullSecrets":[{"name":"$IMAGE_PULL_SECRET"}]}
EOF
kubectl patch serviceaccount default -n "$NAMESPACE" --type merge -p "$(cat /tmp/scms-serviceaccount-patch.json)"
kubectl apply -f k8s/apps.yaml
kubectl apply -f k8s/frontend-nginx.yaml
kubectl apply -f k8s/observability.yaml
kubectl apply -f k8s/elk.yaml
kubectl apply -f k8s/hpa.yaml
for d in eureka-service student-service course-service teacher-service enrollment-service gateway-service; do
  kubectl set env "deployment/$d" CONFIG_SERVER_URL="http://config-service.${NAMESPACE}.svc.cluster.local:8888" -n "$NAMESPACE"
done
if [ -f scripts/sql/06-upgrade-course-schedule-weeks.sql ]; then
  kubectl exec -i -n "$NAMESPACE" mysql-0 -- mysql -uroot -p123888 < scripts/sql/06-upgrade-course-schedule-weeks.sql
fi
if [ -f scripts/sql/05-demo-data.sql ]; then
  kubectl exec -i -n "$NAMESPACE" mysql-0 -- mysql -uroot -p123888 < scripts/sql/05-demo-data.sql
fi
set_image() {
  deployment="$1"
  container="$2"
  image="$IMAGE_REGISTRY/$IMAGE_PROJECT/$deployment:$IMAGE_TAG"
  echo "kubectl set image deployment/$deployment $container=$image"
  kubectl set image "deployment/$deployment" "$container=$image" -n "$NAMESPACE"
  kubectl rollout status "deployment/$deployment" -n "$NAMESPACE" --timeout=420s
}
set_image config-service config-service
set_image eureka-service eureka-service
set_image student-service student-service
set_image course-service course-service
set_image teacher-service teacher-service
set_image enrollment-service enrollment-service
set_image gateway-service gateway-service
set_image frontend frontend
kubectl get pods -n "$NAMESPACE" -o wide
kubectl get deployments -n "$NAMESPACE" -o custom-columns=NAME:.metadata.name,READY:.status.readyReplicas,IMAGES:.spec.template.spec.containers[*].image
'''
        }
      }
    }

    stage('Smoke test') {
      steps {
        sh '''#!/usr/bin/env bash
set -euo pipefail
urls=(
  "http://$MASTER_HOST:30080/"
  "http://$MASTER_HOST:30080/api/courses"
  "http://$MASTER_HOST:30081/actuator/health"
  "http://$MASTER_HOST:30061/"
  "http://$MASTER_HOST:30090/-/healthy"
  "http://$MASTER_HOST:30300/api/health"
  "http://$MASTER_HOST:30601/api/status"
)
for url in "${urls[@]}"; do
  echo "Checking $url"
  curl -fsS --max-time 20 "$url" >/dev/null
  echo "OK $url"
done
'''
      }
    }

    stage('Publish release branch') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'github-release-credentials', usernameVariable: 'GITHUB_USERNAME', passwordVariable: 'GITHUB_TOKEN')]) {
          sh '''#!/usr/bin/env bash
set -euo pipefail
commit="$(git rev-parse --short=8 HEAD)"
auth="$(printf '%s:%s' "$GITHUB_USERNAME" "$GITHUB_TOKEN" | base64 | tr -d '\\n')"
for resolve in $GITHUB_COM_RESOLVE_CANDIDATES; do
  echo "Publishing with $resolve"
  if git -c "http.extraHeader=Authorization: Basic ${auth}" \
    -c "http.curloptResolve=$resolve" \
    push "$REPO_URL" "+HEAD:refs/heads/$RELEASE_BRANCH"; then
    echo "Published deployed commit ${commit} to ${RELEASE_BRANCH}"
    exit 0
  fi
  sleep 8
done
exit 1
'''
        }
      }
    }
  }

  post {
    success { echo "CI/CD success. Deployed image tag ${env.IMAGE_TAG}" }
    failure { echo 'CI/CD failed. Check console output.' }
    always { echo 'CI/CD pipeline finished.' }
  }
}
