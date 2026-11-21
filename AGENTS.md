# Agent guide

This file tells AI coding agents how to work in this repository. Read it before you change code.

## What this repository is

DESS (Department Exam and Schedule System) is a Jakarta EE 11 application for one academic
institution. It runs locally on WildFly 42 with PostgreSQL and Java 21. The architecture is a
modular monolith. The current frontend is a JSP graph editor for the institution module.

The back end uses Java 21, Jakarta EE 11, CDI, Jakarta Persistence (JPA) with the Hibernate
version supplied by WildFly, PostgreSQL, Flyway and Maven. The frontend is JSP with custom CSS
and vanilla JavaScript. The design baseline targets Jakarta JSTL and Bootstrap 5.

The team plan targets a REST API and a React frontend. The repository does not have them yet.
Do not assume they exist. Do not add React, Angular, Vue, Node or a separate frontend
application without an explicit architecture decision from Sergio.

## Read first

- `README.md` - setup and local commands.
- `COLLABORATORS.md` - team, owners and branch rules.
- `docs/security-module.md` - security module layout and current gaps.
- `docs/institution-dependency-graph.md` - module dependencies.
- [docs/PMP.md](docs/PMP.md) and [docs/architecture_farany.md](docs/architecture_farany.md) -
  the target plan and architecture.
- `docs/design/` - the UI/UX baseline. Read all five files before any frontend work:
  `UI_UX_DESIGN_SYSTEM.md`, `DESIGN_TOKENS.md`, `COMPONENT_CATALOG.md`,
  `PAGE_SPECIFICATIONS.md` and `UX_RULES.md`.

## Hard rules

1. Never push to `main`. Only Sergio merges into `main`.
2. Work on `dev` or a `feature/*` branch. Open a pull request to `dev`. Small changes may be
   pushed directly to `dev`, but only Sergio merges pull requests into `dev`.
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
11. Follow `docs/design/`. Do not invent a palette, typography scale, spacing scale, border
    radius or shadow convention.
12. JSP is presentation only. Do not put business logic, database access, authorization
    decisions or complex calculations in JSP. New pages use JSTL and Expression Language, not
    scriptlets. The existing institution editor predates this rule; do not copy its scriptlet
    style into new pages.
13. New JSP views go under `WEB-INF/views/` so they cannot be requested directly. Escape
    untrusted content and enforce authorization on the server for every protected action.
14. Do not claim that visual behavior was verified unless you ran the application and inspected
    the rendered page or screenshots.

## Current state

| Area | State |
| --- | --- |
| `institution` | Working: XML config, validation, graph editor and export. |
| `security` | Working for local use: PBKDF2 passwords, login, roles, CSRF and request filters. Accounts are in memory. |
| `people`, `academic`, `scheduling` | Working through JSP pages and JPA. |
| `assessment` | Working through the application layer and JPA: assessment definitions, eligibility and results. Exam sessions and the Examinations/Results pages are on the unmerged branch `feature/assessment` (PR #12). |
| Platform and REST | Health and login endpoints only; domain endpoints are not implemented. |
| JPA entities | People, academic and scheduling. Flyway owns the schema. |
| Frontend | JSP application pages plus the institution editor. React is a target, not current code. |
| Design system | Baseline in `docs/design/`; implemented on the application pages and the institution structure page. |

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

## Frontend and design system

The UI/UX baseline is in `docs/design/`. Read all five files before implementing or changing a
page. Treat them as the source of truth. If an implementation conflicts with them, follow the
documents and report the conflict.

### Design-system rules

- Use the named design tokens. Do not invent colors, type sizes, spacing, radii or shadows.
- Reuse shared components before creating new ones.
- Keep page headers, breadcrumbs, forms, tables, alerts, badges, dialogs and pagination
  consistent.
- Keep the interface calm, professional, academic, information-dense and predictable.
- Avoid generic AI-dashboard styling, heavy gradients, glassmorphism, oversized cards,
  decorative icons and unnecessary animation.
- Use icons only when they improve recognition; pair ambiguous icons with text.
- Do not communicate status through color alone.

### Frontend implementation rules

- Use semantic HTML and a correct heading hierarchy.
- Every form control needs a visible label or accessible name.
- Provide useful validation messages and preserve entered values after a validation error.
- Include loading, empty, error and success states for asynchronous or data-driven views.
- Make layouts responsive. Wide tables may scroll horizontally or become a readable stacked
  list on narrow screens.
- Keep JavaScript modular and scoped to the page or component. Avoid global variables.
- Do not add a dependency for a small interaction that vanilla JavaScript can handle.
- Ensure keyboard access, visible focus states and adequate contrast. Aim for touch targets of
  at least 44px where practical.
- Escape untrusted user content. Do not render it as raw HTML.

### Scheduling views

- Scheduling is a core workflow, not a decorative calendar.
- Make the selected group, teacher, room, date and time context explicit.
- Explain room, teacher and group conflicts in text; do not only show a red badge.
- Provide group, teacher and room perspectives where they are relevant.
- Provide a readable list alternative for timetable views on small screens.
- Never imply that a booking is confirmed before the server has validated and persisted it.

### Design workflow

1. Inspect the existing page and the components it already uses.
2. Read the five design documents.
3. Identify reusable components and existing conventions.
4. Implement the smallest coherent change.
5. Check links, form actions, validation, empty and error states, responsive behavior and
   accessibility.
6. Report the files changed, decisions made, tests run and anything not verified.

Two OpenCode subagents support this work locally. They are ignored by Git and do not ship with
the repository: `.opencode/agents/frontend-developer.md` implements pages and
`.opencode/agents/ui-reviewer.md` reviews JSP, CSS and JavaScript changes.

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

3. Read the code, tests and docs for the module. For frontend work, also read `docs/design/`.
4. Make the smallest change that solves the problem.
5. Add success and failure tests.
6. Run `mvn -B -ntp test`. For frontend changes, also check the page in the running application.
7. Update the docs and comments. Update the design documents when a token or component changes.
8. Commit with a clear message.
9. Push the branch and open a pull request to `dev`.

Small documentation changes may go directly to `dev`. Code changes should use a branch.

## Testing

- Tests use JUnit 5.
- Add at least one success test and one failure test for new behavior.
- Unit tests use in-memory fakes. Booking rules, database constraints and recursive queries
  need PostgreSQL with Testcontainers when those features are implemented.
- Frontend changes need a manual check in the running application: keyboard navigation, visible
  focus, contrast, responsive layout and the empty, error and success states.
- Do not weaken `ArchitectureTest` to make code compile. Fix the code or discuss the rule first.

## Security module

- The module implements login, logout, sessions, roles, CSRF checks and request filtering.
- Passwords use PBKDF2-HMAC-SHA256 with a per-account salt. No password ships in the code.
  Accounts are seeded from `DESS_ADMIN_PASSWORD` and `DESS_PROFESSOR_PASSWORD` only. For local
  testing, `ops/dev/seed-accounts.sh` writes those variables to `.env`.
- Accounts live in memory and reset on restart. Persisting them needs a Flyway migration, a JPA
  entity and an explicit decision from Sergio.
- `CsrfFilter` runs before `SecurityFilter`. Every POST form must carry `csrfToken`.
- Pages require a session except `/login`, `/api/auth/login`, `/api/health`, `/assets/` and
  `/favicon.ico`. `/app/security` also requires the `ADMIN` role.
- The application is for localhost development. It has no TLS, no credential store and no
  production hardening.

## When to ask

Ask before you:

- change a module's public API or cross-module contract;
- change the database schema;
- add a dependency;
- introduce a frontend framework or build tool;
- change or extend the design system (tokens, components or page patterns);
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
- for frontend changes, it follows `docs/design/` and was checked in the running application;
- its docs and comments are updated.

## Sources

- `docs/PMP.md` - two-week MVP delivery plan.
- `docs/architecture_farany.md` - architecture summary.
- `docs/design/UI_UX_DESIGN_SYSTEM.md` - product experience and visual direction.
- `docs/design/DESIGN_TOKENS.md` - colors, typography, spacing, radii and shadows.
- `docs/design/COMPONENT_CATALOG.md` - shared components and their states.
- `docs/design/PAGE_SPECIFICATIONS.md` - page layouts and required content.
- `docs/design/UX_RULES.md` - UX rules and acceptance checklist.
