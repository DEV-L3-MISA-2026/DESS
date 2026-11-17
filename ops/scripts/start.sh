#!/usr/bin/env bash
# Starts PostgreSQL (Docker) and applies the migrations of every module.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"

if [ ! -f "$ROOT/.env" ]; then
  cp "$ROOT/.env.example" "$ROOT/.env"
  echo "Created .env from .env.example. Edit DESS_DB_PASSWORD, then run this script again."
  exit 1
fi

. "$HERE/_env.sh"
dc up -d --wait db
"$HERE/migrate.sh"

cat <<MSG

PostgreSQL is up and migrated.
Next: build with 'mvn -B -ntp package', then deploy backend/platform/target/dess.war
to your application server (see docs/local-setup.md).
MSG
