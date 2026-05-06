#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOG_DIR="$ROOT/logs"
KEEP_FRONTEND="${KEEP_FRONTEND:-0}"

services=(
  "gateway-service"
  "enrollment-service"
  "teacher-service"
  "course-service"
  "student-service"
  "eureka-service"
  "config-service"
)

if [[ "$KEEP_FRONTEND" != "1" ]]; then
  services=("frontend" "${services[@]}")
fi

for service in "${services[@]}"; do
  pid_file="$LOG_DIR/$service.pid"
  if [[ ! -f "$pid_file" ]]; then
    echo "$service pid file not found, skip."
    continue
  fi

  pid="$(head -n 1 "$pid_file")"
  if [[ -z "$pid" ]]; then
    echo "$service pid file is empty, skip."
    continue
  fi

  if kill -0 "$pid" >/dev/null 2>&1; then
    echo "Stopping $service pid=$pid ..."
    kill "$pid"
  else
    echo "$service pid=$pid is not running."
  fi
done

echo "Stop command completed."
