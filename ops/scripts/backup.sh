#!/usr/bin/env bash
# Creates a custom-format PostgreSQL dump in backups/.
set -euo pipefail
. "$(dirname "$0")/_env.sh"

mkdir -p "$ROOT/backups"
FILE="$ROOT/backups/dess-$(date +%Y%m%d-%H%M%S).dump"
dc exec -T db pg_dump -U "$DESS_DB_USER" -Fc "$DESS_DB_NAME" > "$FILE"
echo "Backup written to $FILE"
