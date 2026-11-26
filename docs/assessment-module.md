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
management page. The `/app/results` page lists each session with the number
of results recorded for its assessment. Result entry and correction exist in
the application layer but have no page yet, so the page never shows a mark
that was not stored. The `/app/exam-sessions` page schedules sessions, finds
one by a case-insensitive substring of its id, replaces invigilators and
cancels a session after a confirmation dialog. It shows a session only
after the server stored it. Write actions require `academic:write`, use
the shared CSRF token, follow Post/Redirect/Get and keep entered values on
validation errors. Cancelling releases the active booking and removes the
session in the same transaction.

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
- The `/app/results` page lists sessions and recorded-result counts. Result
  entry, correction and candidate-level lists have no page yet.
- Integration tests against PostgreSQL through Testcontainers; the
  service tests use in-memory fakes, and the Flyway migration is applied
  separately during local setup.
- The assessment link should move from course id to course offering id
  when the academic module adds course offerings.
