# DESS - Two-Week MVP Delivery Plan

**Revised working plan | 15 November 2026**  
**Team:** 10 people (Sergio, Ronhy, Iavo, Mikajy, Tanjona, Gilbert, Fabien, Christian, Miranto, Vahatra)  
**Delivery horizon:** 10 working days  
**Deployment model:** Local-first application; one institution per local installation/database for the MVP

---

## 1. Executive summary

DESS is one configurable academic management application. The institution describes its organizational structure in XML. The academic concepts and business rules remain explicit in Java.

The two-week MVP must deliver one usable path from setup to results. **Course-session scheduling is part of that path and comes before exam scheduling.** Both use one booking service, so rooms, teachers and student groups cannot be booked twice at overlapping times.

The MVP path is:

1. Import an institution structure from XML.
2. Register people, academic periods, student groups and program enrollments.
3. Define courses, course offerings and teaching assignments.
4. Schedule a teaching session for a course offering. Reserve its room, teacher and group; reject conflicts.
5. Create an assessment and schedule an exam session using the same booking service.
6. Record and retrieve assessment results.

The deliverable is a locally runnable **modular monolith with a REST API**. The backend, database and (when implemented) web frontend must run on a developer's machine or an institution's local server. A production web interface is not required for the two-week target; the frontend stack is selected now so it can be started if the core workflow finishes early.

## 2. Decisions and scope corrections

### Confirmed decisions

| Decision | Status | Result |
|---|---|---|
| ADR-001: plain organizational tree plus `HierarchyService` | **Approved** | No Composite classes in the MVP. |
| ADR-003: database-enforced booking conflicts | **Approved** | Application checks give a clear error; PostgreSQL exclusion constraints protect against concurrent requests. |
| ADR-004: assessment owns `ExamSession`; scheduling owns bookings | **Confirmed** | Scheduling stays generic and has no dependency on Assessment. The same service is also used by Academic for teaching sessions. |
| ADR-002: `StudentGroup` is explicit; programs are deferred | **Amended** | `StudentGroup`, `AcademicProgram` and `ProgramVersion` are all in the MVP. Program enrollment is supported. |
| One institution per database | **Confirmed for MVP** | No tenant-routing layer or cross-institution administration. |
| Local deployment | **Required** | Backend and database run locally. The application is accessed on the same machine first; access from the institution's local network can be added later. |

### Important scope distinction

The MVP includes **basic manual scheduling of individual teaching sessions** for course offerings. It does not include a complete recurring timetable engine, bulk timetable generation or automatic optimization. Those remain post-MVP work.

The MVP also includes `AcademicProgram`, `ProgramVersion` and student program enrollment. It does not include a full curriculum editor, credit model or prerequisite engine.

## 3. Domain vocabulary

| Concept | Meaning |
|---|---|
| `OrganizationalUnit` | A node in the institution tree, such as campus, faculty, department, school or academic level. |
| `StudentGroup` | A cohort taught and examined together. It has an owning organizational unit; it is not an organizational unit. |
| `AcademicProgram` | A named course of study, such as BSc Computer Science. |
| `ProgramVersion` | A dated or named version of a program's rules. A student enrolls against a version. |
| `ProgramEnrollment` | A student's enrollment in an `AcademicProgram` and `ProgramVersion`. |
| `Course` | A subject in the catalogue, such as Mathematics. |
| `CourseOffering` | A delivery of a course to one or more groups in an academic period, with assigned teachers. |
| `TeachingSession` | One dated/time-bounded class meeting for a course offering. Academic owns it. |
| `Assessment` | What is evaluated: exam or test, maximum score and basic settings. |
| `ExamSession` | When and where an assessment takes place, including candidates and supervisors. Assessment owns it. |
| `Booking` | A reservation of a bookable resource for a time range. Scheduling owns it. |

Do not add `CLASS_GROUP` as an organizational-unit type. Keep `StudentGroup` as the only representation of a teaching cohort. Do not add `Map<String, Object>` custom attributes to every unit. That creates a second, weakly typed data model and a validation burden. Custom unit attributes remain out of the MVP.

## 4. Architecture

### 4.1 Application shape

Use one deployable Java application with modules and clear boundaries. Do not split into microservices during this MVP.

| Module | Owns | Depends on |
|---|---|---|
| `institution` | Institution setup, organizational units, unit types, hierarchy rules and XML import | `shared` |
| `people` | Person, student profile, professor profile | `shared` |
| `academic` | Periods, groups, enrollments, programs, program versions, program enrollments, courses, offerings, teaching assignments and teaching sessions | `institution`, `people`, `scheduling`, `shared` |
| `scheduling` | Rooms, bookable-resource references, bookings and conflict checks | `shared` |
| `assessment` | Assessments, exam sessions, candidates, eligibility and results | `academic`, `people`, `scheduling`, `shared` |
| `security` | Accounts, roles and authorization rules | `people`, `shared` |
| `platform` | REST setup, API error mapping, OpenAPI and application configuration | module application APIs |
| `shared` | IDs, shared error types and small time/value types only | none |

Modules communicate through their **application interfaces**, not through another module's repositories or database tables. `platform` is the delivery/API boundary; it must not become a second domain model.

Dependency direction:

```text
academic   -> institution, people, scheduling
assessment -> academic, people, scheduling
security   -> people
scheduling -> shared
institution, people -> shared

No module depends on assessment from scheduling.
No module accesses another module's repository directly.
```

### 4.2 Ownership of scheduling

The ownership rule is important:

- `academic` owns `TeachingSession` because it is a class meeting of a `CourseOffering`.
- `assessment` owns `ExamSession` because it is an assessment event.
- `scheduling` owns `Room`, `Booking` and the reusable reservation/conflict service.

The `scheduling` module does not know what a course or an exam is. It only receives resource references and a time range. Both Academic and Assessment call it.

### 4.3 Course-session booking flow

```text
CourseOffering
     |
     v
TeachingSession (owned by Academic)
     |
     | asks to reserve resources
     v
BookingService (owned by Scheduling)
     |
     +---- Room
     +---- Teacher
     +---- StudentGroup
     |
     v
PostgreSQL conflict constraint
```

A session is accepted only if its room, assigned teacher(s) and group are available. Check room capacity as part of the booking use case. The same group/teacher/room reservations are visible to the exam workflow, so an exam cannot overlap with a class session.

### 4.4 Booking contract sketch

Agree this interface between Academic, Assessment and Scheduling on Day 1. The exact package names can change, but the contract must not.

```java
public enum BookableResourceType { ROOM, PERSON, STUDENT_GROUP }
public enum BookingOwnerType { TEACHING_SESSION, EXAM_SESSION }

public record ResourceRef(BookableResourceType type, UUID id) {}

public record BookingRequest(
    UUID ownerId,
    BookingOwnerType ownerType,
    List<ResourceRef> resources,
    Instant startsAt,
    Instant endsAt
) {}

public interface BookingService {
    BookingReceipt reserve(BookingRequest request);
    void cancel(UUID bookingId);
}
```

`startsAt` must be before `endsAt`. Treat ranges as half-open: `[start, end)`. A session ending at 10:00 does not conflict with a session starting at 10:00. Store instants in UTC; display them using the institution's configured time zone.

### 4.5 Preventing simultaneous double bookings

Do not rely only on `SELECT` followed by `INSERT`: two simultaneous requests can both pass the check. Perform a friendly availability check in Java, then let PostgreSQL enforce the rule with an exclusion constraint on the resource and time range. Use `btree_gist` for the GiST equality/range operators. Catch the constraint violation and return the same readable conflict response.

Conceptual SQL sketch (adapt names and column types to the final schema):

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE booking
ADD CONSTRAINT booking_no_overlap
EXCLUDE USING gist (
    resource_type WITH =,
    resource_id   WITH =,
    during        WITH &&
);
```

One transaction writes all resource bookings for a session. If any resource conflicts, the whole reservation fails.

## 5. Programs and university enrollment

`AcademicProgram` and `ProgramVersion` are included in the MVP. This is the agreed amendment to ADR-002.

```text
AcademicProgram: BSc Computer Science
  |
  +-- ProgramVersion: 2026 catalogue
  |
  +-- ProgramVersion: 2027 catalogue

ProgramEnrollment
  student -> program -> program version -> status / dates
```

Minimum MVP model:

- `AcademicProgram`: stable program identity and code.
- `ProgramVersion`: program, version label/code, effective period and status.
- `ProgramEnrollment`: student, program version, start/end dates and status.
- `StudentGroup`: separate teaching cohort. A program enrollment does not automatically replace group enrollment.

This supports a university that admits students by program. Full curriculum contents, course credits, prerequisites, progression rules and curriculum editing are post-MVP.

## 6. XML configuration and validation

XML is for initial institution setup and import. It is not the operational database and it must not contain executable business logic.

There are three validation layers:

1. **XML parser:** rejects malformed XML, including crossed tags such as `<a><b></a></b>`.
2. **XSD:** checks allowed element structure, required fields, attribute types and nesting shape.
3. **Java semantic validator:** checks that the declared parent-child type pair is allowed, codes are unique, references exist, and an inactive parent cannot contain an active child. The same validator runs when a unit is moved through the API.

A configuration can declare allowed parent-child pairs:

```xml
<unitTypes>
    <type code="CAMPUS"/>
    <type code="FACULTY"/>
    <type code="DEPARTMENT"/>
</unitTypes>

<allowedParentChildPairs>
    <allow parent="CAMPUS" child="FACULTY"/>
    <allow parent="FACULTY" child="DEPARTMENT"/>
</allowedParentChildPairs>
```

The actual institution tree uses a recursive `unit` element. The XSD validates its shape; Java validates the allowed pair for each parent/child. This is a small declarative rule list, not a generic rule engine. Disable DTD/external entity processing (XXE protection). Import all-or-nothing in one transaction. For the MVP, import creates a new institution and fails if its code already exists.

## 7. Technology stack

The backend choice is **Jakarta EE (the modern name for Java EE/JEE)**, not Spring Boot. Use the platform APIs and one compatible application server.

### Backend

| Layer | Choice | Use |
|---|---|---|
| Language | Java 21 | Main backend language |
| Enterprise platform | Jakarta EE 11 | Standard enterprise APIs |
| Runtime | WildFly 42, or another Jakarta EE 11-compatible server | Deploy the backend application |
| REST | Jakarta REST | HTTP API |
| Dependency injection | CDI | Application services and wiring |
| Persistence API | Jakarta Persistence (JPA) | Entity mapping and transactions |
| ORM implementation | Hibernate ORM supplied by the server | Object-relational mapping |
| Database | PostgreSQL | Operational data and booking constraints |
| Migrations | Flyway | Versioned database changes, run during local setup or as a controlled release step |
| Validation | Jakarta Bean Validation | Request and domain input validation |
| Security | Jakarta Security / server-supported identity integration | Authentication and role checks |
| API contract | OpenAPI | Document and test the REST API |
| Tests | JUnit 5 + Testcontainers | Unit and real-PostgreSQL integration tests |
| Build | Maven | Build and dependency management |

Do not add Spring Data JPA to this Jakarta EE stack. Use `EntityManager` and Jakarta Persistence repositories/DAOs where needed. Hibernate is the JPA provider; do not bundle a second Hibernate version that conflicts with the application server.

### Frontend recommendation

For a locally hosted web application with a separate REST backend, use:

| Layer | Choice |
|---|---|
| UI | React + TypeScript |
| Build tool | Vite |
| Components | Material UI (MUI) Community components |
| API state | TanStack Query |
| Routing | React Router |
| Forms | React Hook Form + schema validation |
| Calendar | A simple custom weekly timetable grid first; FullCalendar Standard is an alternative |

Use the free MUI Community components and Data Grid for the first interface. Some advanced MUI X features require a commercial license. FullCalendar Standard is MIT-licensed; its resource timeline and vertical resource views are Premium. Do not make a paid calendar dependency part of the MVP. The two-week release may expose REST/OpenAPI only; the selected frontend can be implemented when the core workflow is stable.

### Local deployment shape

```text
Browser on the same computer
(or a browser on the institution's local network)
  | http://localhost (or the local server address)
  v
React frontend (local static server / development server)
  | /api
  v
Jakarta EE application container (WildFly)
  | JDBC / JPA
  v
PostgreSQL running locally
(one database per institution)
```

The first target is a single-machine setup: WildFly and PostgreSQL run on the same computer, and the user opens the application in a local browser. Keep the frontend and backend as separate build artifacts, but make the local setup straightforward to start and stop.

Mikajy owns the reproducible local environment and run instructions. A local Docker Compose setup may be used to start PostgreSQL and WildFly consistently, provided it does not slow the team down; a documented native installation is also acceptable. Keep credentials out of Git (for example, use local environment variables or an uncommitted configuration file). Include a database backup and restore procedure, health checks and useful local logs. Run Flyway migrations as part of the documented setup or release procedure.

The application may later be hosted on a computer/server inside the institution's local network so multiple users can access it. Public internet access, externally hosted services and managed databases are not part of this MVP.

**MVP tenancy boundary:** one local installation is configured for one institution and connects to that institution's database. Use the same application build for another institution, but do not build tenant switching or database routing in this release.

## 8. Team and module ownership

The team has ten people. Workstream allocations are provisional until the Day 1 skills review.

| Workstream | People | Main ownership |
|---|---|---|
| Lead / architecture | **Sergio** | Cross-module contracts, architecture decisions, integration and release. Pair with Iavo on the project skeleton at the start. Review boundary/contract changes, not every routine commit. |
| A. Institution | **Ronhy** | Institution model, unit types, hierarchy service, allowed parent-child rules, XML/XSD import and hierarchy tests. |
| A. Platform | **Iavo** | Jakarta EE project skeleton, CDI/configuration, datasource and migration wiring, error response model, authentication/roles and OpenAPI. Sergio helps design and establish the skeleton; local runtime setup and release instructions are owned by Mikajy. |
| A. DevOps | **Mikajy** | CI pipeline, repeatable local setup (Docker Compose if useful), WildFly/PostgreSQL startup, local configuration, run scripts or instructions, logs, health checks and database backup/restore procedure. |
| B. Academic core | **Tanjona, Gilbert** | Tanjona: people, periods, AcademicProgram, ProgramVersion and ProgramEnrollment. Gilbert: groups, group enrollments, courses, offerings, teaching assignments and TeachingSession. |
| C. Assessment and results | **Fabien, Christian** | Fabien: assessment definitions, eligibility and result model. Christian: ExamSession, supervisors and booking-service integration. |
| D. Scheduling and quality | **Miranto, Vahatra** | Miranto: Room, BookingService, availability and database conflict constraints. Vahatra: room capacity, booking API support and integration/concurrency tests; also implements features. |

### Practical workload adjustment

Iavo no longer owns environment setup: Mikajy owns the local runtime and release instructions. Sergio pairs with Iavo for the initial architecture and project skeleton, then steps back so Iavo can implement the platform foundations. Keep authentication to the minimum MVP roles (admin and professor); do not build a complex permission system.

Tanjona and Gilbert have an explicit starting split: Tanjona takes people, periods and program enrollment; Gilbert takes groups, course delivery and TeachingSession. They can rebalance after the Day 1 skills review, but every entity must have one named owner. Ronhy can support Academic after the XML/hierarchy path is stable, around Day 4. Christian, Fabien and Miranto must agree the booking interface before implementation diverges.

## 9. Ten-working-day delivery plan

| Day | Main work | End-of-day evidence |
|---|---|---|
| 1 | Confirm decisions; review skills; define module contracts; Sergio + Iavo design the project skeleton; agree booking contract; define XML rule format; Mikajy defines the simplest repeatable local run setup. | Written contracts and decisions; repository skeleton builds. |
| 2 | WildFly application starts locally; PostgreSQL connection; first migrations; shared error format; Testcontainers; seed data; initial CI and repeatable local startup (containerized only if the team chooses it). | A developer can start the application locally and persist one record. |
| 3-4 | Parallel module implementation. Institution XML/XSD; people and academic entities including programs; rooms and bookings; assessment model and result structure; auth and API. | Each workstream demonstrates its minimum use cases against agreed interfaces. |
| 5 | **First integrated path: schedule a course teaching session.** Check room, teacher and group conflicts. | Course session is saved; overlapping booking is rejected clearly. |
| 6 | Finish course scheduling API; connect exam session to the same BookingService; candidate eligibility and supervisor booking. | An exam can be scheduled without bypassing the common booking service. |
| 7 | Complete assessment result entry/retrieval; program enrollment; minimum roles; API flow end-to-end. | Full high-school path runs through the API. |
| 8 | University configuration and program enrollment; invalid XML/hierarchy; room/teacher/group conflicts; concurrent booking tests. | Both institution scenarios and failure tests pass. |
| 9 | Feature freeze; fix critical bugs; verify local backup/restore and setup from a clean checkout; documentation; security and regression pass. | Release candidate starts locally from a clean checkout. |
| 10 | Run the local setup, demonstrate, tag release and list known limitations. | Local MVP demonstrated and release tag created. |

### Critical milestones

- **Day 2:** application builds, starts and persists data.
- **Day 5:** course-session scheduling works and detects conflicts.
- **Day 7:** the main academic + exam + results path works through the API.
- **Day 8:** high-school and university scenarios pass.
- **Day 10:** the local release is reproducible and demonstrable.

## 10. MVP acceptance scenarios

### High-school scenario

1. Import a high-school XML configuration.
2. Create an academic period and a group such as `Terminale A`.
3. Register students and a professor; enroll students in the group.
4. Create Mathematics and a course offering assigned to the professor and group.
5. Create a dated teaching session. Reserve a room, teacher and group.
6. Try a second session that overlaps the room or teacher or group. It must be rejected with a readable error.
7. Create an assessment and schedule an exam session using the same booking service.
8. Enter a score and an absence status; retrieve the results.

### University scenario

1. Import a university structure with campus, faculty and department.
2. Create an `AcademicProgram` and a `ProgramVersion`.
3. Enroll a student in that program version.
4. Create a student group, course offering and teaching assignment.
5. Schedule a teaching session and verify resource conflicts.
6. Schedule an assessment session and retrieve results.

The same application code must support both configurations. Only the institution configuration and data differ.

## 11. MVP scope and cut line

### Included

- Institution creation and XML/XSD import with semantic parent-child validation.
- People, student and professor profiles.
- Academic periods, student groups and group enrollments.
- `AcademicProgram`, `ProgramVersion` and `ProgramEnrollment`.
- Courses, course offerings and teaching assignments.
- Basic manual scheduling of dated teaching sessions with room, teacher and group conflict checks.
- Assessments, exam sessions and shared booking/conflict checks.
- Basic results with status (`SCORED`, `ABSENT`, `EXCUSED`) and audit fields.
- Authentication with minimum admin and professor roles.
- PostgreSQL persistence, migrations, API documentation and core automated tests.
- Local deployment for one institution per installation/database.

### Excluded from the MVP

- Timetable optimization and automatic scheduling.
- Full recurring/term-wide teaching timetables, bulk changes and substitutions.
- Full program curriculum management, credit structures and prerequisite rules.
- Grade averages, weights, retakes and ranking.
- XML export, configuration versioning and custom unit attributes.
- File storage and notifications.
- Thesis defense system.
- Finance, payroll and procurement.
- Student or parent portals and mobile apps.
- Microservices, event sourcing, analytics and dashboards.
- Generic rule engine or executable rules in XML.
- Production web UI, unless the exam workflow is finished early. The frontend stack is selected, but the API is the release acceptance surface.

### If the team falls behind

Cut in this order: web UI; advanced candidate seat assignment; supervisor conflict checks (keep room and group checks at minimum); fine-grained permissions (keep admin/professor); depth of the university structure. Never cut persistence, course-room conflict checks, exam-room conflict checks, readable errors, result retrieval or tests for the main path and a conflict path.

## 12. After the MVP: roadmap sketches

These are follow-up work packages, not hidden MVP requirements.

| Work package | First useful increment |
|---|---|
| Full teaching timetables | Weekly templates, recurring sessions, substitutions, cancellation and term-wide views. The MVP already stores individual booked teaching sessions. |
| Timetable optimization | Generate candidate timetables from teacher, room, group and course constraints; show conflicts and let an administrator approve the result. |
| Academic programs and curricula | Extend the MVP program/version records with curriculum modules, courses, credits, prerequisites and progression rules. |
| Grading | Grading policies, averages, weighted components, retakes and rankings. |
| Configuration lifecycle | XML export, configuration versioning, migration/preview and validated custom unit attributes. |
| Communication and files | Secure file storage, email/SMS/in-app notifications and delivery history. |
| Thesis defense | Thesis records, supervisors, jury, defense sessions, room booking and outcomes. |
| Institution operations | Finance, payroll and procurement as separate business modules. |
| User-facing products | Student and parent portals, then mobile apps if there is a clear need. |
| Platform evolution | Analytics and dashboards; consider microservices or event sourcing only when scale or team boundaries justify them. |
| Rule extension | Consider a constrained, typed rule model only after concrete use cases; do not execute arbitrary XML code. |
| Frontend | Build the React application when the core API workflow is stable; expand it beyond the minimum admin screens. |

## 13. Working rules and definition of done

- One shared repository; `main` must build.
- Short feature branches; merge at least daily.
- At least one review per merge. Module owners approve routine work; Sergio reviews cross-module contracts and architecture changes.
- Database migrations are committed with the feature that needs them.
- Tests use PostgreSQL through Testcontainers; do not rely only on an in-memory database for exclusion constraints and recursive queries.
- Never access another module's repository directly.
- XML import calls the `institution` application service; it does not write tables itself.
- Each feature is done when it works through the application layer, persists, validates its inputs, has success and failure tests, builds on latest `main`, and is integrated into the locally running application.

Daily rhythm: 10-15 minutes at the start for yesterday/today/blockers; 10-15 minutes at the end to ensure `main` builds and the board is current. Raise cross-team blockers immediately.

Suggested task-board fields: `ID`, title, owner, workstream, dependencies, acceptance criteria, status, target day and reviewer.

## 14. Main delivery risks

| Risk | Response |
|---|---|
| Academic work blocks everyone | Split Tanjona/Gilbert's work on Day 1; seed data by Day 2; Ronhy joins Academic when institution work stabilizes. |
| Booking interface changes late | Christian, Fabien and Miranto agree one contract by end of Day 1. |
| Double booking under concurrency | PostgreSQL exclusion constraint plus simultaneous-request integration test. |
| Iavo is overloaded | Sergio helps design the skeleton; Mikajy owns the repeatable local setup and release process. |
| XML becomes a programming language | XSD + small declarative parent-child list + Java semantic validation only. |
| Cross-module coupling grows | Application interfaces only; architecture test blocks forbidden dependencies. |
| Local setup surprises the team | Mikajy proves the setup from a clean checkout by Day 2 and rehearses the local release on Day 9. |
| UI consumes the schedule | API is the acceptance surface; UI is a stretch goal. |

## 15. Day 1 decisions to record

1. Confirm Jakarta EE 11 / Java 21 / WildFly 42 or agree on a compatible server based on team experience.
2. Confirm PostgreSQL and the `btree_gist` extension for booking constraints.
3. Confirm one institution per database and one institution per local MVP installation.
4. Confirm the resource booking contract and time model (`Instant`, UTC, half-open intervals).
5. Confirm the XML shape and allowed parent-child pair format.
6. Confirm minimum roles and the exact API surface.
7. Split academic entities and use cases between Tanjona and Gilbert.
8. Confirm the local runtime setup, local configuration, backup/restore procedure and startup instructions with Mikajy.

## 16. Technical references checked for this revision

- [Jakarta EE 11 platform](https://jakarta.ee/specifications/platform/11/) - platform release and minimum Java version.
- [WildFly 42 Getting Started Guide](https://docs.wildfly.org/42/Getting_Started_Guide.html) - current EE 11 runtime, Java requirements and supported APIs.
- [React: Creating a React App](https://react.dev/learn/creating-a-react-app) and [Vite guide](https://vite.dev/guide/) - frontend starting point and build tooling.
- [TanStack Query documentation](https://tanstack.com/query/latest/docs/framework/react/guides/queries) - server-state fetching and caching.
- [MUI licensing](https://mui.com/pricing/) - Community and paid component boundaries.
- [FullCalendar pricing](https://fullcalendar.io/pricing) and [Premium plugins](https://fullcalendar.io/docs/premium) - Standard versus resource/timeline views.

---

**Prepared for the Day 1 planning session.** This is a delivery plan, not a claim that all implementation details have already been agreed. The booking contract, local setup approach and team skill split must be confirmed by the people named above.
