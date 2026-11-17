#!/usr/bin/env bash
set -euo pipefail
: "${WILDFLY_HOME:?Set WILDFLY_HOME}"
V="${POSTGRES_JDBC_VERSION:-42.7.7}"
D="$WILDFLY_HOME/modules/org/postgresql/main"
mkdir -p "$D"
curl -fL "https://repo1.maven.org/maven2/org/postgresql/postgresql/$V/postgresql-$V.jar" -o "$D/postgresql-$V.jar"
cat > "$D/module.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="org.postgresql"><resources><resource-root path="postgresql-$V.jar"/></resources><dependencies><module name="java.sql"/><module name="jakarta.transaction.api"/></dependencies></module>
EOF
echo "PostgreSQL JDBC module installed. Restart WildFly before configuring the datasource."
