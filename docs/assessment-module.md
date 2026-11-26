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
  than zero, at most two decimal places), duration in minutes
  (positive integer, see below), active flag, creation time
  in UTC. An assessment is never deleted; it is deactivated.
- `Result`: id (UUID), assessment id, exam session id (optional),
  student person id (numeric id of the people module), score
  (decimal or null), status (`SCORED`, `ABSENT`, `EXCUSED`),
  entered by, entered at, updated at. A `SCORED` result carries a
  score. An `ABSENT` or `EXCUSED` result carries no score.
- `ResultCorrection`: one entry of the correction history of a
  result: the result id, the previous score and status, the new
  score and status, who corrected it (`correctedBy`) and when
  (`correctedAt`). Entries are ordered oldest first. The original
  entered value stays readable as the previous value of the first
  entry of the trail.

## Duration

The duration of an assessment is `durationMinutes`, a positive
integer number of **minutes**. There is one unit only, minutes;
no second unit exists. A missing duration is rejected by the
create command, a zero or negative duration is rejected by the
`Assessment` domain record, both with an `IllegalArgumentException`
that the API error mapper turns into HTTP 400 (`INVALID_INPUT`),
never 500. The column `assessment.duration_minutes` is guarded by
the database too (`ck_assessment_duration`: greater than zero).
Rows created before the column existed were backfilled with 60
minutes by migration `V20261126_1200`; the column default was
removed in the same migration, so every new assessment must
supply its duration.
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

Each failure is reported as a stable code, never as a sentence:
`EligibilityReason` (`ASSESSMENT_INACTIVE`, `STUDENT_NOT_FOUND`,
`NO_ACTIVE_PROGRAM_ENROLLMENT`, `NO_OFFERING_ENROLLMENT`). Tests
assert the code, not the sentence. The English sentence shown to
a user lives in one place only, `EligibilityPresentation`, which
both the page (issue #33) and the API error path (the message of
`IneligibleCandidateException`, built by the result service from
the same presenter) use, so they can never drift apart. The
presenter lives in the assessment module, not in the web layer, on
purpose: the result service needs the same wording for its
exception message, and the application layer must not depend on
the web layer. Pages must use this presenter and never keep their
own copy of a sentence. All sentences are English; no French
reason is rendered anywhere in the assessment module.

## Result rules

A result is recorded only for an existing assessment and an eligible
student, and only once per assessment and student. The score must be
at least zero and at most the assessment maximum score. A correction
changes the score, the status and the update time. It never changes
who entered the result or when it was entered. Every correction also
appends one entry to the correction trail (table
`assessment_result_correction`, migration `V20261126_1200`) with the
previous score and status, the new score and status, who corrected
it and when. The corrector is required on `CorrectResultCommand`
(`correctedBy`); a correction without a named corrector is rejected.
The trail is read with `ResultService.correctionsFor(resultId)`, and
`ResultService.detail(id)` returns the result with its trail for the
result detail view. Two corrections leave an ordered trail, and the
original value stays readable as the previous value of the first
entry. Correction entries are history: they are never updated.

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
- Migration `V20261126_1200__assessment_duration_and_result_correction.sql`
  adds `assessment.duration_minutes` (positive minutes, see above)
  and the `assessment_result_correction` table (result id, previous
  and new score/status, corrected by, corrected at, with the same
  score/status coherence checks as `assessment_result` and
  `ON DELETE CASCADE` from the result).
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

## Blocked: issues #106 and #111 (course offerings, issue #18)

These two issues are NOT implemented in this batch, and no code was
written for them. The blocker is issue #18, "CourseOffering: course,
period and groups", which is OPEN and assigned to NiainaGilbert.

What is missing, verified on `dev`:

- There is no `CourseOffering` class anywhere in the codebase, and
  the academic pages state that offerings are not implemented yet.
- `AcademicPeriod` has no start or end date (only `academicYear` and
  `term`), so there are no period boundaries to check a date against.
- `Course` has no link to a period.
- `Assessment` itself has no date; the date of an assessment lives on
  `ExamSession.startsAt`/`endsAt`, which Christian owns.

What this blocks:

- #106 (assessment date inside its academic period): the issue asks to
  resolve the period from the offering and reject a date outside it,
  naming the period boundaries. Without an offering, there is no period
  to resolve and no boundaries to name.
- #111 (eligibility must check the offering enrollment): the issue asks
  to require an enrollment for the offering once #18 and #19 land (#19
  is also OPEN, assigned to NiainaGilbert). The program-enrollment check
  stays as the second rule, and the distinct reason code for the
  offering failure, `NO_OFFERING_ENROLLMENT`, already exists in
  `EligibilityReason` as a documented placeholder. It is never returned
  until #18 lands.

What will unblock them: #18 landing on `dev` (a `CourseOffering` with a
course, a period with start/end dates, and groups), plus #19 for #111.
When that happens, the assessment link should move from course id to
offering id, the period check of #106 and the offering-enrollment check
of #111 can be written against the real offering, and
`NO_OFFERING_ENROLLMENT` starts being returned. No offering model was
invented inside the assessment module, and the offering-enrollment check
was NOT faked against `GroupEnrollment`: group enrollment and offering
enrollment are different concepts.
