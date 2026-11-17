# Local setup

Owner: DevOps workstream. Everything below is a starting point to be proven from a clean
checkout (plan: Day 2) and rehearsed again on Day 9.

## Prerequisites

- Java 21 and Maven 3.9+
- Docker with the Compose plugin (for PostgreSQL; optional if PostgreSQL is installed natively)
- A Jakarta EE 11 compatible application server (the plan targets WildFly 42)
- Node.js 22+ (frontend only, stretch goal)

## Steps

1. `cp .env.example .env` and set `DESS_DB_PASSWORD`.
2. `./ops/scripts/start.sh` - starts PostgreSQL and runs the Flyway migrations of every module.
3. `mvn -B -ntp package` - builds all modules and the WAR `backend/platform/target/dess.war`.
4. Configure the datasource once (template, adapt to your server layout):
   `$WILDFLY_HOME/bin/jboss-cli.sh --file=ops/wildfly/configure-datasource.cli`.
   This requires the PostgreSQL JDBC driver to be installed as a WildFly module (`org.postgresql`)
   or deployed as a driver jar.
5. Export the `DESS_DB_*` variables in the shell that starts WildFly, then copy
   `dess.war` to `$WILDFLY_HOME/standalone/deployments/`.
6. API base URL: `http://localhost:8080/dess/api`.
7. `./ops/scripts/health-check.sh`.

Stop PostgreSQL with `./ops/scripts/stop.sh` (data is kept in a Docker volume).

## Database migrations

- Each module keeps its migrations in `backend/<module>/src/main/resources/db/migration/<module>/`.
- Name them with a timestamp to avoid version collisions between developers:
  `V20261117_1030__academic_create_student_group.sql`.
- Commit a migration together with the feature that needs it.
- `migrate.sh` runs Flyway with `outOfOrder=true` for local development only.
  Do not use that flag for a release.
