#!/usr/bin/env bash
# Restores a dump created by backup.sh. This REPLACES the current database content.
set -euo pipefail
. "$(dirname "$0")/_env.sh"

FILE="${1:-}"
[ -n "$FILE" ] && [ -f "$FILE" ] || { echo "Usage: restore.sh <backup-file>" >&2; exit 2; }

read -r -p "This replaces the content of database '$DESS_DB_NAME'. Type 'yes' to continue: " ANSWER
[ "$ANSWER" = "yes" ] || { echo "Cancelled."; exit 1; }

dc exec -T db pg_restore -U "$DESS_DB_USER" -d "$DESS_DB_NAME" --clean --if-exists --no-owner < "$FILE"
echo "Restore finished."
