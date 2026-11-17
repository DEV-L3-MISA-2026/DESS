#!/usr/bin/env bash
set -euo pipefail
: "${WILDFLY_HOME:?Set WILDFLY_HOME}"
: "${DB_PASSWORD:?Source .env first}"
DB_HOST="${DB_HOST:-127.0.0.1}"; DB_PORT="${DB_PORT:-5432}"; DB_NAME="${DB_NAME:-dess}"; DB_USER="${DB_USER:-dess}"
"$WILDFLY_HOME/bin/jboss-cli.sh" --connect <<EOF
/subsystem=datasources/jdbc-driver=postgresql:add(driver-name=postgresql,driver-module-name=org.postgresql,driver-class-name=org.postgresql.Driver)
/subsystem=datasources/data-source=DessDS:add(jndi-name=java:/jdbc/DessDS,driver-name=postgresql,connection-url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME},user-name=${DB_USER},password=${DB_PASSWORD},enabled=true,min-pool-size=2,max-pool-size=20,check-valid-connection-sql="SELECT 1")
/subsystem=datasources/data-source=DessDS:test-connection-in-pool
EOF
