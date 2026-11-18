# Collaborators

This page explains who owns what, how we work, and how code reaches `main`.
It is based on the two-week MVP plan (`docs/PMP.md`) and the architecture summary
(`docs/architecture_farany.md`).

## Team and ownership

| Person | Main area | What they own |
| --- | --- | --- |
| Sergio | Lead and architecture | Cross-module contracts, architecture decisions, integration and release. Reviews boundary changes. |
| Ronhy | Institution | Institution model, unit types, hierarchy rules, XML/XSD import and hierarchy tests. |
| Iavo | Platform | Jakarta EE skeleton, CDI and configuration, datasource, migrations, error format, auth and roles, OpenAPI. |
| Mikajy | DevOps | CI, local setup, WildFly and PostgreSQL scripts, logs, health checks, backup and restore. |
| Tanjona | Academic core | People, academic periods, programs, program versions and program enrollment. |
| Gilbert | Academic core | Student groups, group enrollment, courses, course offerings, teaching assignments and teaching sessions. |
| Fabien | Assessment | Assessment definitions, eligibility and the result model. |
| Christian | Assessment | Exam sessions, supervisors and booking integration. |
| Miranto | Scheduling | Rooms, the booking service, availability and database conflict constraints. |
| Vahatra | Scheduling and quality | Room capacity, booking API, integration and concurrency tests. Also implements features. |

One person owns each entity. Ask the owner before changing its rules or public API.

## GitHub access

Current repository collaborators:

| GitHub handle | Role |
| --- | --- |
| `sergiorandria` | admin (lead developer) |
| `RonhyRaktosMG` | write |
| `vahatraSylvio23` | write |
| `Heriandrisoa` | write |
| `IavoAmbinintsoa` | write |
| `NiainaGilbert` | write |
| `Tsifeherana` | write |
| `Fabien-Halaby` | write |
| `herimanantsoa51` | write |
| `Mokabe000` | write |
| `Adnan-f` | read |

Only Sergio can push or merge into `main` and `dev`. Everyone else works on a `feature/*`
branch and opens a pull request to `dev`. Every pull request needs Sergio's review; the
`.github/CODEOWNERS` file requests it automatically.

> `main` and `dev` are protected on GitHub: only Sergio can push or merge, every pull request
> needs his review, the CI `build` check must pass, and force pushes and deletions are blocked.

## Branches

| Branch | Purpose |
| --- | --- |
| `main` | Release branch. It must always build. Only Sergio can push or merge. |
| `dev` | Integration branch. Only Sergio can push or merge. |
| `feature/<area>-<topic>` | Short branch for one fix or feature. Open a pull request to `dev`. |

Only Sergio pushes or merges into `main` and `dev`. Run the tests before you open a pull
request.

## How we work

- Keep branches short. Merge at least once a day.
- Sergio reviews and merges every pull request. Module owners can comment on changes in their
  area, but Sergio gives the required approval.
- Keep `main` and `dev` buildable.
- Commit database migrations with the feature that needs them.
- Use PostgreSQL with Testcontainers for booking and recursive-query tests. Do not rely only
  on an in-memory database for those rules. The current unit tests use H2; this is a gap to close.
- Never access another module's repository or tables directly. Use its application interface.
- XML import calls the institution application service. It does not write tables itself.
- Keep secrets out of Git: `.env`, tokens and local configuration stay untracked.

## Definition of done

A feature is done when:

- it works through the application layer;
- it saves its data;
- it validates its input;
- it has success and failure tests;
- it builds on the latest `dev`;
- it runs in the local application;
- its docs and comments are updated.

## Communication

- 10 to 15 minutes at the start of the day: what was done, what is next, blockers.
- 10 to 15 minutes at the end of the day: make sure `dev` builds and the board is current.
- Raise blockers immediately. Do not wait for the next meeting.
- In a pull request, explain what changed, why, and how it was tested.

## Sources

- `docs/PMP.md` - two-week MVP delivery plan (15 November 2026).
- `docs/architecture_farany.md` - architecture summary.
