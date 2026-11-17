#!/usr/bin/env bash
# Runs Flyway (official Docker image) against every module's migration folder.
set -euo pipefail
. "$(dirname "$0")/_env.sh"

MOUNTS=()
LOCATIONS=()
for dir in "$ROOT"/backend/*/src/main/resources/db/migration/*/; do
  [ -d "$dir" ] || continue
  compgen -G "${dir}*.sql" > /dev/null || continue
  name="$(basename "$dir")"
  MOUNTS+=(-v "${dir%/}:/flyway/sql/${name}:ro")
  LOCATIONS+=("filesystem:/flyway/sql/${name}")
done

if [ "${#LOCATIONS[@]}" -eq 0 ]; then
  echo "No migrations found."
  exit 0
fi

LOCS="$(IFS=,; echo "${LOCATIONS[*]}")"

# outOfOrder=true is for local development only (timestamped migrations from many branches).
docker run --rm --network dess_default "${MOUNTS[@]}" \
  -e FLYWAY_URL="jdbc:postgresql://db:5432/${DESS_DB_NAME}" \
  -e FLYWAY_USER="${DESS_DB_USER}" \
  -e FLYWAY_PASSWORD="${DESS_DB_PASSWORD}" \
  -e FLYWAY_LOCATIONS="${LOCS}" \
  -e FLYWAY_OUT_OF_ORDER=true \
  flyway/flyway:11 migrate
