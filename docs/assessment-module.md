# Assessment module — exam sessions

Status: exam session scheduling is implemented; assessment definitions, candidate
eligibility and results are still planned work.

## What is implemented

`ExamSession` owns one exam: an assessment id, a room, a half-open UTC time range,
candidate person ids and invigilator person ids. The domain record validates the range,
the UTC conversion, duplicate person ids and the rule that a person cannot be both a
candidate and an invigilator.

Sessions are persisted by Flyway-owned tables (`exam_session`, `exam_session_candidate`,
`exam_session_invigilator`). The `assessment` module reaches them only through its
`ExamSessionRepository` application port and its JPA adapter. Hibernate never creates or
updates the schema.

`ExamSessionServiceImpl` reserves the room and every invigilator through the frozen
scheduling `BookingService` contract. It uses `ResourceRef.room` and
`ResourceRef.personId` so the same person always maps to the same bookable resource.
The service is transactional: a booking conflict leaves neither a booking nor a session.
A conflict names the resource in an `ExamSessionConflictException` message, so the web
layer can explain it in text.

`withInvigilators` cancels the active booking and reserves a new one with the replacement
list. `withCandidates` only updates the session; candidate eligibility is not implemented.

The `/app/examinations` page stays a read-only list with a link to the management page.
The `/app/exam-sessions` page schedules sessions, finds one by a case-insensitive substring
of its id, replaces invigilators and cancels a session after a confirmation dialog. It shows
a session only after the server stored it. Write actions require `academic:write`, use the
shared CSRF token, follow Post/Redirect/Get and keep entered values on validation errors.
Cancelling releases the active booking and removes the session in the same transaction.

## What is not implemented

- `Assessment`, eligibility and the result model (Fabien's work in the delivery plan).
- Booking a student group: the `ExamSession` model has no group field yet, so only the room
  and the invigilators are reserved.
- Integration tests against PostgreSQL through Testcontainers; the service tests use
  in-memory fakes, and the Flyway migration is applied separately during local setup.
