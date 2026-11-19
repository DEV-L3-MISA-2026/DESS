#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../lib/env.sh
source "$SCRIPT_DIR/../lib/env.sh"

dess_load_env
dess_require_db_password

: "${WILDFLY_HOME:?Set WILDFLY_HOME in .env or in the shell (see .env.example)}"

cli="$WILDFLY_HOME/bin/jboss-cli.sh"
jdbc_url="$(dess_jdbc_url)"

if [[ ! -x "$cli" ]]
then
    echo "error: WILDFLY_HOME does not point to a WildFly installation: $WILDFLY_HOME" >&2
    exit 1
fi

if ! dess_cli --connect --command=":read-attribute(name=server-state)" >/dev/null 2>&1
then
    echo "error: WildFly is not running; start it before configuring the datasource." >&2
    exit 1
fi

# CLI string values cannot safely carry double quotes or backslashes.
if [[ "$DESS_DB_PASSWORD" == *[\"\\]* ]]
then
    echo "error: DESS_DB_PASSWORD must not contain double quotes or backslashes." >&2
    exit 1
fi

commands_file="$(mktemp)"
trap 'rm -f "$commands_file"' EXIT

cat > "$commands_file" <<EOF
if (outcome != success) of /subsystem=datasources/jdbc-driver=postgresql:read-resource
    /subsystem=datasources/jdbc-driver=postgresql:add(driver-name=postgresql, driver-module-name=org.postgresql, driver-class-name=org.postgresql.Driver)
end-if

if (outcome != success) of /subsystem=datasources/data-source=DessDS:read-resource
    /subsystem=datasources/data-source=DessDS:add(jndi-name=java:/jdbc/DessDS, driver-name=postgresql, connection-url="$jdbc_url", user-name="$DESS_DB_USER", password="$DESS_DB_PASSWORD", enabled=true, min-pool-size=2, max-pool-size=20, check-valid-connection-sql="SELECT 1")
else
    /subsystem=datasources/data-source=DessDS:write-attribute(name=connection-url, value="$jdbc_url")
    /subsystem=datasources/data-source=DessDS:write-attribute(name=user-name, value="$DESS_DB_USER")
    /subsystem=datasources/data-source=DessDS:write-attribute(name=password, value="$DESS_DB_PASSWORD")
    /subsystem=datasources/data-source=DessDS:write-attribute(name=enabled, value=true)
end-if
EOF

dess_cli --connect --file="$commands_file"
dess_cli --connect --command="/subsystem=datasources/data-source=DessDS:test-connection-in-pool"

echo "Datasource java:/jdbc/DessDS is configured and reachable."
