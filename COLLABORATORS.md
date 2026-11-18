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
| `sergiorandria` | admin |
| `RonhyRaktosMG` | admin |
| `vahatraSylvio23` | admin |
| `Heriandrisoa` | admin |
| `IavoAmbinintsoa` | admin |
| `NiainaGilbert` | admin |
| `Tsifeherana` | admin |
| `Fabien-Halaby` | admin |
| `herimanantsoa51` | admin |
| `Mokabe000` | admin |
| `Adnan-f` | read |

Sergio is the only person who merges into `main`. Other admins can review and merge into
`dev`, but not into `main`.

> GitHub branch protection is not available for this private repository on the free plan.
> Until the plan changes, the `main` rule is a team rule: do not merge to `main` unless you
> are Sergio.

## Branches

| Branch | Purpose |
| --- | --- |
| `main` | Release branch. It must always build. Only Sergio merges. |
| `dev` | Integration branch. Day-to-day work lands here. |
| `feature/<area>-<topic>` | Short branch for one fix or feature. Open a pull request to `dev`. |

Do not push directly to `main`. Run the tests before pushing to `dev`.

## How we work

- Keep branches short. Merge at least once a day.
- One review per merge. The module owner reviews routine work. Sergio reviews cross-module
  contracts and architecture changes.
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
