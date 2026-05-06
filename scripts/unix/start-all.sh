#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOG_DIR="$ROOT/logs"
mkdir -p "$LOG_DIR"

MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-123888}"
JWT_SECRET="${JWT_SECRET:-local-demo-secret-change-me}"
SKIP_BUILD="${SKIP_BUILD:-0}"
SKIP_FRONTEND="${SKIP_FRONTEND:-0}"

export MYSQL_USER MYSQL_PASSWORD JWT_SECRET

command -v java >/dev/null 2>&1 || {
  echo "java was not found. Please install Java 17 and add it to PATH." >&2
  exit 1
}

command -v mvn >/dev/null 2>&1 || {
  echo "mvn was not found. Please install Maven 3.8+ and add it to PATH." >&2
  exit 1
}

if [[ "$SKIP_BUILD" != "1" ]]; then
  echo "Building backend modules ..."
  (cd "$ROOT" && mvn clean package -DskipTests)
fi

find_jar() {
  local module="$1"
  find "$ROOT/$module/target" -maxdepth 1 -type f -name "$module-*.jar" ! -name "*.original" | head -n 1
}

start_service() {
  local module="$1"
  local delay="$2"
  local jar
  jar="$(find_jar "$module")"

  if [[ -z "$jar" ]]; then
    echo "Jar file for $module was not found. Run mvn clean package -DskipTests first." >&2
    exit 1
  fi

  echo "Starting $module ..."
  nohup java -jar "$jar" > "$LOG_DIR/$module.log" 2>&1 &
  echo "$!" > "$LOG_DIR/$module.pid"
  echo "  pid: $(cat "$LOG_DIR/$module.pid"), log: $LOG_DIR/$module.log"
  sleep "$delay"
}

start_service "eureka-service" 12
start_service "student-service" 4
start_service "course-service" 4
start_service "teacher-service" 4
start_service "enrollment-service" 4
start_service "gateway-service" 6

if [[ "$SKIP_FRONTEND" != "1" ]]; then
  command -v npm >/dev/null 2>&1 || {
    echo "npm was not found. Please install Node.js and npm, or set SKIP_FRONTEND=1." >&2
    exit 1
  }

  if [[ ! -d "$ROOT/frontend/node_modules" ]]; then
    echo "Installing frontend dependencies ..."
    (cd "$ROOT/frontend" && npm install)
  fi

  echo "Starting frontend ..."
  pushd "$ROOT/frontend" >/dev/null
  nohup npm run dev > "$LOG_DIR/frontend.log" 2>&1 &
  echo "$!" > "$LOG_DIR/frontend.pid"
  popd >/dev/null
  echo "  pid: $(cat "$LOG_DIR/frontend.pid"), log: $LOG_DIR/frontend.log"
fi

echo ""
echo "Startup commands submitted."
echo "Eureka:   http://localhost:8761"
echo "Gateway:  http://localhost:8080"
echo "Frontend: http://localhost:5173"
echo ""
echo "PID files are in $LOG_DIR."
