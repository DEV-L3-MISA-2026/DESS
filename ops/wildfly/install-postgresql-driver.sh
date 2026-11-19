#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../lib/env.sh
source "$SCRIPT_DIR/../lib/env.sh"

dess_load_env

: "${WILDFLY_HOME:?Set WILDFLY_HOME in .env or in the shell (see .env.example)}"

version="${POSTGRES_JDBC_VERSION:-42.7.7}"
module_dir="$WILDFLY_HOME/modules/org/postgresql/main"
driver_jar="$module_dir/postgresql-$version.jar"

if [[ ! -x "$WILDFLY_HOME/bin/jboss-cli.sh" ]]
then
    echo "error: WILDFLY_HOME does not point to a WildFly installation: $WILDFLY_HOME" >&2
    exit 1
fi

mkdir -p "$module_dir"

if [[ -f "$driver_jar" ]]
then
    echo "PostgreSQL JDBC driver $version is already installed."
else
    echo "Downloading PostgreSQL JDBC driver $version..."
    temporary_jar="$(mktemp "$module_dir/postgresql-$version.jar.XXXXXX")"
    trap 'rm -f "$temporary_jar"' EXIT
    curl -fsSL --retry 3 \
        "https://repo1.maven.org/maven2/org/postgresql/postgresql/$version/postgresql-$version.jar" \
        -o "$temporary_jar"
    mv "$temporary_jar" "$driver_jar"
    trap - EXIT
fi

cat > "$module_dir/module.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="org.postgresql">
    <resources>
        <resource-root path="postgresql-$version.jar"/>
    </resources>
    <dependencies>
        <module name="java.sql"/>
        <module name="java.management"/>
        <module name="jakarta.transaction.api"/>
    </dependencies>
</module>
EOF

echo "PostgreSQL JDBC module ready at $module_dir."
echo "Restart WildFly if it was running before this module was installed."
