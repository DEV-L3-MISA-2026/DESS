#!/usr/bin/env bash
# Quick health check: PostgreSQL, then the application server's management endpoint.
set -uo pipefail
. "$(dirname "$0")/_env.sh"

status=0

if dc exec -T db pg_isready -U "$DESS_DB_USER" -d "$DESS_DB_NAME" > /dev/null 2>&1; then
  echo "PostgreSQL : OK"
else
  echo "PostgreSQL : DOWN"; status=1
fi

# WildFly management health endpoint (default port 9990). Adjust for another server.
if curl -fsS --max-time 3 http://localhost:9990/health > /dev/null 2>&1; then
  echo "WildFly    : OK"
else
  echo "WildFly    : DOWN or health endpoint not reachable"; status=1
fi

exit $status
