# Architecture

One deployable Java application (modular monolith), organised as DDD bounded contexts.
No microservices in the MVP.

## Bounded contexts and dependencies

| Context (module) | Type | Owns | Depends on |
|---|---|---|---|
| `institution` | Supporting | Institution, organizational units, unit types, hierarchy rules, XML import | `shared` |
| `people` | Supporting | Person, student profile, professor profile | `shared` |
| `scheduling` | Core | Rooms, bookings, conflict checks | `shared` |
| `academic` | Core | Periods, groups, programs, enrollments, courses, offerings, teaching sessions | `institution`, `people`, `scheduling`, `shared` |
| `assessment` | Supporting | Assessments, exam sessions, candidates, eligibility, results | `academic`, `people`, `scheduling`, `shared` |
| `security` | Generic | Accounts and roles (admin, professor) | `people`, `shared` |
| `platform` | Delivery | WAR, REST application, error mapping, OpenAPI, configuration | all modules (through `api` packages) |
| `shared` | Shared kernel | IDs, `TimeRange`, base exceptions | none |

```text
academic   -> institution, people, scheduling
assessment -> academic, people, scheduling
security   -> people
scheduling -> shared
institution, people -> shared

scheduling never depends on assessment (or on any other business context).
```

## Context map

- `scheduling` is upstream: an Open Host Service whose published language is
  `BookingService`, `BookingRequest` and `ResourceRef`. `academic` and `assessment` are customers.
- `assessment` consumes `academic` through an anti-corruption layer: it defines its own
  `CandidateSource` port; an adapter in `infrastructure/acl` translates `academic.api` data.
- `institution`, `people`, `security` are conformist: others reference their IDs and accept their model.
- The shared kernel stays tiny. Changes to it need a review by the lead.

## Layers inside each context

```text
<context>/
  api/                 published interfaces and DTOs - the ONLY package other contexts may import
  internal/
    domain/            aggregates, entities, value objects, domain services, repository interfaces
    application/       use cases (transaction boundary), ports to other contexts
    infrastructure/    JPA repositories, adapters, anti-corruption layers, XML parsing
    rest/              Jakarta REST resources and request/response DTOs
```

Rules (checked by `ArchitectureTest` in `platform`):

1. Other contexts import only `..<context>.api..`.
2. `internal.domain` does not depend on `application`, `infrastructure` or `rest`.
3. Aggregates reference each other, and other contexts, by UUID only.
4. One repository per aggregate root; interface in `domain`, implementation in `infrastructure`.
5. No public setters: state changes go through methods named in the glossary.

## Aggregates and key invariants

| Context | Aggregate root (contents) | Key invariant |
|---|---|---|
| institution | `Institution` | Code unique |
| institution | `OrganizationalUnit` (parent by id) | Allowed parent-child pair; no cycle on move; inactive parent cannot hold an active child |
| people | `Person` (`StudentProfile`, `ProfessorProfile`) | - |
| academic | `AcademicPeriod` | start < end |
| academic | `AcademicProgram` (`ProgramVersion`) | Version code unique within the program |
| academic | `ProgramEnrollment` | Dates coherent; enrolls against one version |
| academic | `StudentGroup`, `GroupEnrollment` | A student is enrolled once per group |
| academic | `Course` | Code unique |
| academic | `CourseOffering` (`TeachingAssignment`) | At least one group and one teacher before sessions are planned |
| academic | `TeachingSession` | Has a booking; start < end |
| scheduling | `Room` | Capacity > 0 |
| scheduling | `Booking` (`BookingSlot`) | At least one resource; no overlap per resource (also enforced by PostgreSQL) |
| assessment | `Assessment` | maxScore > 0 |
| assessment | `ExamSession` (candidates, supervisors) | Has a booking |
| assessment | `AssessmentResult` | `SCORED`: 0 <= score <= max; `ABSENT`/`EXCUSED`: no score |

## Transactions

One aggregate per transaction, with one deliberate exception (ADR-006): a `TeachingSession`
(or `ExamSession`) and its `Booking` are saved in the same JTA transaction, because booking
conflicts must be rejected immediately.
