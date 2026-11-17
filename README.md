# DESS — local-first backend scaffold

Maven multi-module Jakarta EE application. No frontend is included.

```text
DESS/
├── pom.xml
├── dess-app/                  # WAR deployed to WildFly
│   └── src/main/java/com/dess/
│       ├── institution/{domain,application,infrastructure}
│       ├── people/{domain,application,infrastructure}
│       ├── academic/{domain,application,infrastructure}
│       ├── assessment/{domain,application,infrastructure}
│       ├── scheduling/{domain,application,infrastructure}
│       ├── security/
│       └── shared/
├── db-migration/              # Separate Flyway Maven runner
├── ops/postgres/              # Optional local PostgreSQL Compose file
└── ops/wildfly/               # JDBC driver and datasource setup
```

## Stack
- Java 21; Jakarta EE 11 APIs; WildFly 42 runtime.
- Jakarta Persistence; Hibernate is supplied by WildFly (do not bundle another Hibernate).
- PostgreSQL; WildFly JTA datasource `java:/jdbc/DessDS`.
- Flyway Maven plugin in `db-migration`; schema history is stored in `flyway_schema_history`.
- Maven multi-module build; JUnit 5.

## Local setup
1. Copy `.env.example` to `.env`, set a local password, then export it: `set -a; source .env; set +a`.
2. Start PostgreSQL (optional Docker): `docker compose -f ops/postgres/compose.yaml up -d`.
3. Set `WILDFLY_HOME` to your WildFly installation. Install the JDBC module: `./ops/wildfly/install-postgresql-driver.sh`. Restart WildFly.
4. Configure datasource while WildFly is running: `./ops/wildfly/configure-datasource.sh`.
5. Apply migrations: `mvn -pl db-migration flyway:info` then `mvn -pl db-migration flyway:migrate`.
6. Build: `mvn clean verify`. Deploy `dess-app/target/dess-app.war` to WildFly.

## Migration policy
Put SQL files in `db-migration/src/main/resources/db/migration/` using names such as `V1__initial_schema.sql`. Never edit a migration already applied to a shared database; add a new version. Run Flyway before deploying the corresponding application version. Flyway owns schema evolution; Hibernate must not create or update the schema.

## Boundaries
- Modules call other modules through their `application` layer, not by reaching into their repositories.
- `academic` owns regular teaching sessions; `assessment` owns exam sessions; `scheduling` owns rooms, bookings and conflict detection. Assessment and academic call scheduling to reserve resources.
- `shared` stays small and technical.
- One local WildFly instance and one local PostgreSQL database for this development setup. No cloud deployment, tenant routing, orchestration or frontend.

For production, do not store datasource passwords as plain text in CLI scripts; use a WildFly credential store or deployment secret management.
