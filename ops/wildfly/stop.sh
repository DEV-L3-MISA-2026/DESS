#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../lib/env.sh
source "$SCRIPT_DIR/../lib/env.sh"

usage()
{
    cat <<EOF
Usage: $(basename "$0") [--with-db]

Shuts down WildFly and, with --with-db, stops the local PostgreSQL Compose
container. The database volume is preserved.
EOF
}

stop_database="false"

while (( $# > 0 ))
do
    case "$1" in
        --with-db)
            stop_database="true"
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
    shift
done

dess_load_env

: "${WILDFLY_HOME:?Set WILDFLY_HOME in .env or in the shell (see .env.example)}"

cli="$WILDFLY_HOME/bin/jboss-cli.sh"

if [[ -x "$cli" ]] && dess_cli --connect --command=":read-attribute(name=server-state)" >/dev/null 2>&1
then
    echo "Shutting down WildFly..."
    dess_cli --connect --command=":shutdown"

    for _ in $(seq 1 30)
    do
        if ! dess_cli --connect --command=":read-attribute(name=server-state)" >/dev/null 2>&1
        then
            break
        fi
        sleep 1
    done
else
    echo "WildFly is not running."
fi

rm -f "$DESS_REPO_ROOT/.run/wildfly.pid"

if [[ "$stop_database" == "true" ]]
then
    if command -v docker >/dev/null 2>&1
    then
        if [[ -f "$DESS_ENV_FILE" ]]
        then
            compose=(docker compose --env-file "$DESS_ENV_FILE" -f "$DESS_REPO_ROOT/ops/postgres/compose.yaml")
        else
            compose=(docker compose -f "$DESS_REPO_ROOT/ops/postgres/compose.yaml")
        fi

        echo "Stopping PostgreSQL..."
        "${compose[@]}" stop
    else
        echo "Docker is not available; skipping the database shutdown." >&2
    fi
fi
