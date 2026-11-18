#!/usr/bin/env bash
#
# Shared environment loading for the local-development scripts under ops/.
# Source this file; sourcing only defines the paths and helper functions.

DESS_REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DESS_ENV_FILE="$DESS_REPO_ROOT/.env"
export DESS_REPO_ROOT DESS_ENV_FILE

#
# Loads .env when present and applies the local database defaults.
# Variables missing from .env keep the value already exported in the shell.
#
dess_load_env()
{
    if [[ -f "$DESS_ENV_FILE" ]]
    then
        set -a
        # shellcheck disable=SC1090
        source "$DESS_ENV_FILE"
        set +a
    fi

    export DESS_DB_HOST="${DESS_DB_HOST:-localhost}"
    export DESS_DB_PORT="${DESS_DB_PORT:-5432}"
    export DESS_DB_NAME="${DESS_DB_NAME:-dess}"
    export DESS_DB_USER="${DESS_DB_USER:-dess}"
    export DESS_DB_PASSWORD="${DESS_DB_PASSWORD:-}"
    export WILDFLY_PORT_OFFSET="${WILDFLY_PORT_OFFSET:-0}"
}

#
# Runs jboss-cli against the WildFly management interface, taking the
# configured port offset into account.
#
dess_cli()
{
    if (( WILDFLY_PORT_OFFSET > 0 ))
    then
        "$WILDFLY_HOME/bin/jboss-cli.sh" --controller="localhost:$((9990 + WILDFLY_PORT_OFFSET))" "$@"
    else
        "$WILDFLY_HOME/bin/jboss-cli.sh" "$@"
    fi
}

#
# Prints the application URL for the configured HTTP port offset.
#
dess_app_url()
{
    printf 'http://localhost:%s/dess-app/' "$((8080 + WILDFLY_PORT_OFFSET))"
}

#
# Fails with an actionable message when the database password is missing.
#
dess_require_db_password()
{
    if [[ -z "$DESS_DB_PASSWORD" ]]
    then
        echo "error: DESS_DB_PASSWORD is not set." >&2
        echo "       Copy .env.example to .env and set a local password." >&2
        exit 1
    fi
}

#
# Prints the JDBC URL built from the DESS_DB_* settings.
#
dess_jdbc_url()
{
    printf 'jdbc:postgresql://%s:%s/%s' "$DESS_DB_HOST" "$DESS_DB_PORT" "$DESS_DB_NAME"
}

#
# Fails with an actionable message when a local database port does not accept
# TCP connections. A healthy Compose container whose published port is
# unreachable has usually lost its Docker network, so the message names the
# commands that repair that state. Remote hosts are not checked. Set
# DESS_SKIP_DB_PORT_CHECK=1 to bypass the check.
#
dess_check_local_db_port()
{
    local host="$DESS_DB_HOST" container project network status probe=()

    if [[ "${DESS_SKIP_DB_PORT_CHECK:-0}" == "1" ]]
    then
        return 0
    fi

    case "$host" in
        localhost|127.0.0.1|::1)
            ;;
        *)
            return 0
            ;;
    esac

    if command -v timeout >/dev/null 2>&1
    then
        probe=(timeout 2)
    fi

    if "${probe[@]}" bash -c "</dev/tcp/$host/$DESS_DB_PORT" 2>/dev/null
    then
        return 0
    fi

    echo "error: nothing accepts TCP connections on $host:$DESS_DB_PORT." >&2

    if command -v docker >/dev/null 2>&1
    then
        container="dess-postgres"
        if ! docker inspect "$container" >/dev/null 2>&1
        then
            container="$(docker ps -a \
                --filter 'label=com.docker.compose.service=postgres' \
                --format '{{.Names}}' 2>/dev/null | head -n 1)"
        fi

        status=""
        if [[ -n "$container" ]]
        then
            status="$(docker inspect -f '{{.State.Status}}/{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' \
                "$container" 2>/dev/null || true)"
        fi

        if [[ "$status" == "running/healthy" ]]
        then
            if ! docker inspect -f '{{json .HostConfig.PortBindings}}' "$container" 2>/dev/null \
                | grep -q "\"$DESS_DB_PORT/tcp\""
            then
                echo "       Docker container '$container' is healthy but does not publish port $DESS_DB_PORT." >&2
                echo "       Check DESS_DB_PORT in .env or the mapping in ops/postgres/compose.yaml." >&2
                return 1
            fi

            project="$(docker inspect -f '{{index .Config.Labels "com.docker.compose.project"}}' \
                "$container" 2>/dev/null || true)"
            network="${project:-postgres}_default"

            echo "       Docker container '$container' is healthy, but its published port is not reachable." >&2
            echo "       This usually means the container lost its Docker network. Repair it with:" >&2
            echo "         docker network connect $network $container" >&2
            echo "       If Docker reports that it is already connected, something else blocks the port." >&2
            echo "       Recreating the container also repairs the network; the named volume keeps the data:" >&2
            echo "         docker compose --env-file .env -f ops/postgres/compose.yaml up -d --force-recreate" >&2
            return 1
        fi
    fi

    echo "       Start the local database with:" >&2
    echo "         docker compose --env-file .env -f ops/postgres/compose.yaml up -d" >&2
    echo "       Or set DESS_DB_HOST and DESS_DB_PORT for another server." >&2
    return 1
}
