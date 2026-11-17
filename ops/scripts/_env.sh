# Sourced by the other scripts: loads .env and defines helpers.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
if [ -f "$ROOT/.env" ]; then
  set -a; . "$ROOT/.env"; set +a
fi
: "${DESS_DB_PASSWORD:?DESS_DB_PASSWORD is not set (copy .env.example to .env)}"
DESS_DB_NAME="${DESS_DB_NAME:-dess}"
DESS_DB_USER="${DESS_DB_USER:-dess}"

# docker compose bound to this project
dc() { docker compose -f "$ROOT/ops/docker-compose.yml" --env-file "$ROOT/.env" "$@"; }
