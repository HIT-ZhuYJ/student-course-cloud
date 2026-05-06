#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SQL_DIR="$ROOT/scripts/sql"

MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-123888}"

command -v mysql >/dev/null 2>&1 || {
  echo "mysql was not found. Please install it or add it to PATH." >&2
  exit 1
}

files=(
  "00-create-databases.sql"
  "01-student-service.sql"
  "02-course-service.sql"
  "03-teacher-service.sql"
  "04-enrollment-service.sql"
  "05-demo-data.sql"
)

for file in "${files[@]}"; do
  path="$SQL_DIR/$file"
  if [[ ! -f "$path" ]]; then
    echo "SQL file not found: $path" >&2
    exit 1
  fi

  echo "Executing $file ..."
  mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" "-p$MYSQL_PASSWORD" < "$path"
done

echo "Database initialization completed."
