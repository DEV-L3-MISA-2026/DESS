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
- The exam session id on a result is recorded as given. Exam
  sessions have no persistence yet on the dev branch, so the result
  service cannot check the session. When session persistence lands,
  it should check that the session exists, belongs to the assessment
  and lists the student as a candidate.

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

## Persistence

- Tables `assessment` and `assessment_result`, created by
  `V20261121_1200__assessment_create_assessment_and_result.sql`.
  Flyway owns the schema.
- The database enforces the one-result rule, the score/status
  coherence and the score lower bound. The score upper bound is
  enforced by the application, which knows the assessment maximum.
- Entities are registered in `dessPU` and in the `assessmentTestPU`
  test unit.

## Open questions

- Christian's `ExamSession` model and the read-only Examinations and
  Results pages live on the unmerged branch `feature/assessment`
  (PR #12). This branch does not touch them. Once PR #12 is merged,
  the Results page can read through `ResultService` instead of
  showing "Not entered yet", and the result service can check the
  session against the stored exam sessions.
- The assessment link should move from course id to course offering
  id when the academic module adds course offerings.
