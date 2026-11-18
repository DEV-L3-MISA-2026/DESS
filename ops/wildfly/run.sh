#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../lib/env.sh
source "$SCRIPT_DIR/../lib/env.sh"

usage()
{
    cat <<EOF
Usage: $(basename "$0") [options]

Builds the application, applies Flyway migrations, installs the PostgreSQL
JDBC module when needed, starts WildFly, configures java:/jdbc/DessDS and
deploys dess-app.war.

Options:
  --with-db       Start the local PostgreSQL Compose container when
                  DESS_DB_HOST points to localhost.
  --skip-build    Reuse the existing dess-app/target/dess-app.war.
  --skip-migrate  Do not run Flyway before deploying.
  -h, --help      Show this help.

Set WILDFLY_PORT_OFFSET in .env when the default WildFly ports are in use.
WildFly runs in the background. Use ops/wildfly/stop.sh to stop it.
EOF
}

start_database="false"
build_application="true"
run_migrations="true"

while (( $# > 0 ))
do
    case "$1" in
        --with-db)
            start_database="true"
            ;;
        --skip-build)
            build_application="false"
            ;;
        --skip-migrate)
            run_migrations="false"
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
dess_require_db_password

cli="$WILDFLY_HOME/bin/jboss-cli.sh"
war="$DESS_REPO_ROOT/dess-app/target/dess-app.war"
run_dir="$DESS_REPO_ROOT/.run"
pid_file="$run_dir/wildfly.pid"

if [[ ! -x "$WILDFLY_HOME/bin/standalone.sh" || ! -x "$cli" ]]
then
    echo "error: WILDFLY_HOME does not point to a WildFly installation: $WILDFLY_HOME" >&2
    exit 1
fi

if [[ ! -w "$WILDFLY_HOME/standalone" ]]
then
    echo "error: $WILDFLY_HOME/standalone is not writable." >&2
    echo "       Use a copy of WildFly owned by your user and point WILDFLY_HOME at it." >&2
    exit 1
fi

server_running()
{
    dess_cli --connect --command=":read-attribute(name=server-state)" 2>/dev/null | grep -q 'running'
}

compose_command()
{
    if [[ -f "$DESS_ENV_FILE" ]]
    then
        printf '%s\n' "docker" "compose" "--env-file" "$DESS_ENV_FILE" "-f" "$DESS_REPO_ROOT/ops/postgres/compose.yaml"
    else
        printf '%s\n' "docker" "compose" "-f" "$DESS_REPO_ROOT/ops/postgres/compose.yaml"
    fi
}

start_database_container()
{
    if [[ "$start_database" != "true" ]]
    then
        return
    fi

    if [[ "$DESS_DB_HOST" != "localhost" && "$DESS_DB_HOST" != "127.0.0.1" ]]
    then
        echo "DESS_DB_HOST=$DESS_DB_HOST is not local; not starting the Compose database."
        return
    fi

    if ! command -v docker >/dev/null 2>&1
    then
        echo "error: --with-db requires Docker." >&2
        exit 1
    fi

    if ! docker compose version >/dev/null 2>&1
    then
        echo "error: --with-db requires the Docker Compose plugin." >&2
        exit 1
    fi

    mapfile -t compose < <(compose_command)
    echo "Starting PostgreSQL..."
    "${compose[@]}" up -d

    for _ in $(seq 1 30)
    do
        status="$(docker inspect -f '{{.State.Health.Status}}' dess-postgres 2>/dev/null || true)"
        if [[ "$status" == "healthy" ]]
        then
            return
        fi
        sleep 2
    done

    echo "error: PostgreSQL did not become healthy in time." >&2
    exit 1
}

build_war()
{
    if [[ "$build_application" != "true" ]]
    then
        echo "Skipping build; using $war"
        return
    fi

    echo "Building dess-app..."
    (cd "$DESS_REPO_ROOT" && mvn -B -ntp -pl dess-app package)
}

migrate_database()
{
    if [[ "$run_migrations" != "true" ]]
    then
        echo "Skipping Flyway migrations."
        return
    fi

    "$SCRIPT_DIR/../db/migrate.sh"
}

wait_for_server()
{
    for _ in $(seq 1 90)
    do
        if server_running
        then
            return
        fi

        if [[ -f "$pid_file" ]] && ! kill -0 "$(cat "$pid_file")" 2>/dev/null
        then
            echo "error: WildFly exited during startup. Last log lines:" >&2
            tail -n 40 "$WILDFLY_HOME/standalone/log/server.log" >&2 || true
            exit 1
        fi

        sleep 2
    done

    echo "error: WildFly did not reach the running state; see $WILDFLY_HOME/standalone/log/server.log." >&2
    exit 1
}

start_server()
{
    if server_running
    then
        echo "WildFly is already running."
        return
    fi

    mkdir -p "$run_dir"
    echo "Starting WildFly..."
    if (( WILDFLY_PORT_OFFSET > 0 ))
    then
        nohup "$WILDFLY_HOME/bin/standalone.sh" \
            -Djboss.socket.binding.port-offset="$WILDFLY_PORT_OFFSET" \
            >"$run_dir/wildfly.out" 2>&1 &
    else
        nohup "$WILDFLY_HOME/bin/standalone.sh" >"$run_dir/wildfly.out" 2>&1 &
    fi
    echo $! > "$pid_file"
    wait_for_server
}

deploy_war()
{
    if [[ ! -f "$war" ]]
    then
        echo "error: $war not found; run without --skip-build first." >&2
        exit 1
    fi

    echo "Deploying $(basename "$war")..."
    dess_cli --connect --command="deploy --force \"$war\""
}

start_database_container
if ! dess_check_local_db_port
then
    exit 1
fi
build_war
migrate_database
"$SCRIPT_DIR/install-postgresql-driver.sh"
start_server
"$SCRIPT_DIR/configure-datasource.sh"
deploy_war

echo
echo "WildFly is running."
echo "Application: $(dess_app_url)"
echo "Stop WildFly: $SCRIPT_DIR/stop.sh"
