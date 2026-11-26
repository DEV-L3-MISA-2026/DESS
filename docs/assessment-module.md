# Assessment module

Owner: Fabien. Christian owns the exam sessions, the supervisors and
the booking integration.

Sources: `docs/PMP.md` (lot C, MVP path steps 5 and 6, day 7),
`docs/architecture_farany.md` (§5, §10), `COLLABORATORS.md`.

## What the module owns

| Concept | Meaning |
| --- | --- |
| `Assessment` | What is evaluated: an exam, a test or a practical evaluation, with a maximum score. One assessment can be held in several exam sessions. |
| Eligibility | Who may sit an assessment, from the enrollment rules. |
| `Result` | One student's outcome for one assessment, with a score or an absence/excuse status, plus audit fields. |
| `ExamSession` | When and where an assessment is held: a room, a half-open UTC time range, the candidates and the invigilators. |

Grade averages, weights, retakes and ranking are out of the MVP
scope (PMP §11), so the module has no weight and no average.

## Model

- `Assessment`: id (UUID), course id (UUID), title, type
  (`EXAM`, `TEST`, `PRACTICAL`), maximum score (decimal, greater
  than zero, at most two decimal places), active flag, creation time
  in UTC. An assessment is never deleted; it is deactivated.
- `Result`: id (UUID), assessment id, exam session id (optional),
  student person id (numeric id of the people module), score
  (decimal or null), status (`SCORED`, `ABSENT`, `EXCUSED`),
  entered by, entered at, updated at. A `SCORED` result carries a
  score. An `ABSENT` or `EXCUSED` result carries no score.
- One student has at most one result per assessment. A retake is a
  new assessment, not a second result. The service checks it and the
  database enforces it with a unique constraint on
  (assessment, student).
- `ExamSession`: an assessment id, a room, a half-open UTC time range,
  candidate person ids and invigilator person ids. The domain record
  validates the range, the UTC conversion, duplicate person ids and the
  rule that a person cannot be both a candidate and an invigilator.

## Links to other modules

- The assessment is linked to a course id, checked through
  `CourseService`. The academic module has no course offering yet,
  so there is no offering link. When a course offering lands, the
  link should move to the offering id, and the eligibility check
  should require an enrollment for that offering.
- The student is a numeric person id, as in the exam session model.
  Eligibility uses `PeopleService` and `ProgramEnrollmentService`
  only. The module never reads the academic or people tables or
  repositories directly.
- The exam session id on a result is recorded as given. The result
  service does not check it yet; the follow-up should check that the
  session exists, belongs to the assessment and lists the student as a
  candidate.

## Eligibility rules

A student may sit an assessment when:

1. the assessment exists and is active;
2. the student exists as a student;
3. the student has at least one ACTIVE program enrollment.

The program enrollment is the enrollment rule that exists on this
branch. It is a proxy for the offering enrollment until course
offerings exist.

## Result rules

A result is recorded only for an existing assessment and an eligible
student, and only once per assessment and student. The score must be
at least zero and at most the assessment maximum score. A correction
changes the score, the status and the update time. It never changes
who entered the result or when it was entered.

## Exam sessions

`ExamSession` owns one exam. Sessions are persisted by Flyway-owned
tables (`exam_session`, `exam_session_candidate`,
`exam_session_invigilator`). The `assessment` module reaches them only
through its `ExamSessionRepository` application port and its JPA
adapter. Hibernate never creates or updates the schema.

`ExamSessionServiceImpl` reserves the room and every invigilator through
the frozen scheduling `BookingService` contract. It uses
`ResourceRef.room` and `ResourceRef.personId` so the same person always
maps to the same bookable resource. The service is transactional: a
booking conflict leaves neither a booking nor a session. A conflict
names the resource in an `ExamSessionConflictException` message, so the
web layer can explain it in text.

`withInvigilators` cancels the active booking and reserves a new one with
the replacement list. `withCandidates` only updates the session;
candidate eligibility is checked by the assessment definitions work, not
here.

The `/app/examinations` page stays a read-only list with a link to the
management page. The `/app/exam-sessions` page schedules sessions, finds
one by a case-insensitive substring of its id, replaces invigilators and
cancels a session after a confirmation dialog. It shows a session only
after the server stored it. Write actions require `academic:write`, use
the shared CSRF token, follow Post/Redirect/Get and keep entered values on
validation errors. Cancelling releases the active booking and removes the
session in the same transaction.

## Pages (Batch B, issues #32, #33, #34, #117)

- `/app/assessments` (issue #32): the assessment list with type and status,
  and the create form (title, type, course, maximum score, duration in
  minutes). A created assessment is visible after reload. Validation errors
  return 400 with a summary and preserved values. Professors are read-only:
  the form is hidden and every write is refused on the server with 403,
  through the same `academic:write` check as the exam-sessions page
  (the professor role has read permissions only).
- `/app/eligibility?assessment=<id>` (issue #33): eligible and ineligible
  candidates for one assessment, with the reason for each ineligible
  candidate. The reason sentence comes from `EligibilityPresentation`, the
  same wording as the API error, and the page never decides eligibility
  itself: every row asks `EligibilityService`. Candidates come from the
  exam sessions of the assessment (the union of every session candidate
  list, in session order, without duplicates). When no session exists for
  the assessment, the page checks an explicit student list given as
  comma-separated person ids in the `students` query parameter; the list can
  also add candidates alongside the session lists. The explicit list was
  chosen over "all students" because the page must show the candidates of
  the assessment, not the whole student body, and no other candidate source
  exists in the servlets today.
- `/app/results?session=<id>` (issue #34): result entry for one exam
  session. Each candidate row shows the stored mark and status badge only;
  a candidate with no stored result shows no mark. A mark is entered per
  candidate and saved through `ResultService.recordResult`; an existing
  result is changed through `ResultService.correctResult` with the current
  user as `correctedBy`. The correction trail from
  `ResultService.detail(id)` is shown under a corrected result, so the
  original value stays readable. An invalid mark returns 400 and keeps the
  entered value. Save follows Post/Redirect/Get, so a reload shows the
  stored mark. The entry section identifies the assessment, the course, the
  session (date, time in UTC, room) and the candidate; no academic period
  is shown, because no course offering exists yet. No averages, weighting,
  ranking or retake UI is introduced here.
- Filters and empty state (issue #117): the assessments list filters by
  type and by course. Both filters live in the URL
  (`/app/assessments?type=EXAM&course=<id>`), so reloading the URL restores
  them. An unknown type or course value matches nothing instead of silently
  ignoring the filter. The empty state names the next action: it offers to
  clear the filters when filters are active, and otherwise tells a writer
  to create the first assessment below.

### Period filter: pending issue #18

The period filter of issue #117 is NOT implemented, and no period data is
faked. An assessment has no period: there is no `CourseOffering`, a course
has no period link, and `AcademicPeriod` has no start or end dates. When
#18 lands (a `CourseOffering` with a course, a period with start/end dates,
and groups), the assessment link should move from course id to offering id
and the period filter can be written against the real period. Until then
the list filters by type and course only.

## Persistence

- Tables `assessment` and `assessment_result`, created by
  `V20261121_1300__assessment_create_assessment_and_result.sql`.
- Tables `exam_session`, `exam_session_candidate` and
  `exam_session_invigilator`, created by
  `V20261121_1200__assessment_exam_session.sql`.
- Flyway owns the schema. The database enforces the one-result rule,
  the score/status coherence and the score lower bound. The score upper
  bound is enforced by the application, which knows the assessment
  maximum.
- Entities are registered in `dessPU` and in the test persistence units.

## Known gaps

- Booking a student group: the `ExamSession` model has no group field
  yet, so only the room and the invigilators are reserved.
- Integration tests against PostgreSQL through Testcontainers; the
  service tests use in-memory fakes, and the Flyway migration is applied
  separately during local setup.
- The assessment link should move from course id to course offering id
  when the academic module adds course offerings.
