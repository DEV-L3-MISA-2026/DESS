#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../lib/env.sh
source "$SCRIPT_DIR/../lib/env.sh"

dess_load_env
dess_require_db_password

export DB_URL="$(dess_jdbc_url)"
export DB_USER="$DESS_DB_USER"
export DB_PASSWORD="$DESS_DB_PASSWORD"

echo "Database: $(dess_jdbc_url)"

usage()
{
    cat <<EOF
Usage: $(basename "$0") [migrate|info|validate|repair|clean] [maven-arguments...]

Applies the migrations from db-migration/src/main/resources/db/migration to
the database described by the DESS_DB_* variables. Without an argument, the
script prints the migration status and then applies the pending migrations.

The repository .env file is loaded automatically. Any extra arguments are
passed to Maven, for example -Dflyway.target=2.

clean drops every object in the configured schema and clears
flyway_schema_history. It is destructive; use it only for local development,
then run ./ops/db/migrate.sh to rebuild the schema.
EOF
}

command="migrate"
if (( $# > 0 ))
then
    command="$1"
    shift
fi

case "$command" in
    migrate|info|validate|repair|clean)
        ;;
    -h|--help)
        usage
        exit 0
        ;;
    *)
        usage >&2
        exit 64
        ;;
esac

if ! dess_check_local_db_port
then
    exit 1
fi

cd "$DESS_REPO_ROOT"

if [[ "$command" == "migrate" ]]
then
    exec mvn -B -ntp -pl db-migration flyway:info flyway:migrate "$@"
fi

exec mvn -B -ntp -pl db-migration "flyway:$command" "$@"
