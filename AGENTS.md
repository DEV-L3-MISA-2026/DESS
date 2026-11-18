# Agent guide

This file tells AI coding agents how to work in this repository. Read it before you change code.

## What this repository is

DESS is a Jakarta EE 11 application for one academic institution. It runs locally on WildFly 42
with PostgreSQL and Java 21. The current frontend is a JSP graph editor for the institution
module.

The team plan targets a REST API and a React frontend. The repository does not have them yet.
Do not assume they exist.

## Read first

- `README.md` - setup and local commands.
- `COLLABORATORS.md` - team, owners and branch rules.
- `docs/security-module.md` - security module layout and current gaps.
- `docs/institution-dependency-graph.md` - module dependencies.
- [docs/PMP.md](docs/PMP.md) and [docs/architecture_farany.md](docs/architecture_farany.md) -
  the target plan and architecture.

## Hard rules

1. Never push to `main` or `dev`. Only Sergio pushes or merges into them.
2. Work on a `feature/*` branch and open a pull request to `dev`. Only Sergio reviews and
   merges pull requests.
3. Do not change repository settings, collaborators, visibility or branch protection unless
   Sergio asks you to.
4. Do not commit `.env`, `config/institution.xml`, tokens or other secrets.
5. Keep module boundaries. Call another module through its application interface.
6. Institution code must not import SQL, JPA, REST, servlet or another business module.
   `ArchitectureTest` enforces this.
7. Flyway owns the database schema. Hibernate must not create or update it.
8. The current frontend is JSP-only. Do not add servlets or JPA imports to `src/main/java`
   for the institution module without an explicit architecture decision.
9. XML is configuration. It is not operational data and not executable business logic.
10. Do not invent requirements. Check the plan and ask when something is unclear.

## Current state

| Area | State |
| --- | --- |
| `institution` | Working: XML config, validation, graph editor and export. |
| `security` | Scaffold only: records and interfaces, no behavior. |
| `people`, `academic`, `scheduling`, `assessment` | Package placeholders. |
| Platform and REST | Not implemented. |
| JPA entities | None yet. |
| Frontend | JSP editor only. React is a target, not current code. |

## Module map

| Module | Owns |
| --- | --- |
| `institution` | Institution, organizational units, unit types, hierarchy rules and XML. |
| `people` | Person, student profile and professor profile. |
| `academic` | Periods, groups, programs, enrollment, courses, offerings and teaching sessions. |
| `scheduling` | Rooms, bookings and conflict checks. |
| `assessment` | Assessments, exam sessions, candidates and results. |
| `security` | Accounts, roles, permissions, login and access checks. |
| `shared` | Small technical types used by several modules. |

Dependencies point from the outside in:

```text
academic   -> institution, people, scheduling
assessment -> academic, people, scheduling
security   -> people
scheduling -> shared
institution, people -> shared
```

No module reads another module's tables or repositories directly.

## Commands

Run these from the repository root:

```bash
mvn -B -ntp test                 # compile and run all tests
mvn -B -ntp clean package        # build the WAR
./ops/wildfly/run.sh --with-db   # build, migrate, start WildFly and deploy
./ops/wildfly/stop.sh --with-db  # stop WildFly and the local database
./ops/db/migrate.sh info         # show Flyway status
```

You need Java 21, Maven 3.9+, WildFly 42 and PostgreSQL 17.

## Code and text style

- Use simple English. Short sentences are better than long ones.
- Explain an acronym the first time you use it.
- Comments should explain why, not repeat what the code says.
- Use Javadoc with `@brief`, `@param` and `@return` where they help.
- Java uses 4 spaces and Allman braces.
- Keep one public type per file.
- Do not add a dependency without a clear reason.

## How to make a change

1. Start from the latest `dev`:

   ```bash
   git switch dev
   git pull origin dev
   ```

2. Create a short branch:

   ```bash
   git switch -c feature/<area>-<topic>
   ```

3. Read the code, tests and docs for the module.
4. Make the smallest change that solves the problem.
5. Add success and failure tests.
6. Run `mvn -B -ntp test`.
7. Update the docs and comments.
8. Commit with a clear message.
9. Push the branch and open a pull request to `dev`.

All changes, including documentation, go through a feature branch and a pull request. Only
Sergio merges.

## Testing

- Tests use JUnit 5.
- Add at least one success test and one failure test for new behavior.
- The current unit tests use H2. Booking rules, database constraints and recursive queries
  need PostgreSQL with Testcontainers when those features are implemented.
- Do not weaken `ArchitectureTest` to make code compile. Fix the code or discuss the rule first.

## Security module

- The module has three domain records and three service interfaces. There is no behavior yet.
- `ArchitectureTest` blocks JPA and servlet imports in `src/main/java`. Decide how security
  storage and filters will work before implementing them.
- `dess-app/pom.xml` currently declares only the CDI API.
- The JSP page has no authentication or CSRF protection. It is for localhost development only.

## When to ask

Ask before you:

- change a module's public API or cross-module contract;
- change the database schema;
- add a dependency;
- change the architecture guard;
- touch `main`, repository settings or collaborators;
- work on a requirement that is not in the plan.

## Definition of done

A change is done when:

- it works through the application layer;
- it saves its data;
- it validates its input;
- it has success and failure tests;
- it builds on the latest `dev`;
- it runs in the local application;
- its docs and comments are updated.

## Sources

- `docs/PMP.md` - two-week MVP delivery plan.
- `docs/architecture_farany.md` - architecture summary.
