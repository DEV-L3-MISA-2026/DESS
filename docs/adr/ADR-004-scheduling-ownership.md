# ADR-004: Assessment owns ExamSession; scheduling owns bookings

- Status: Confirmed

## Context
Both teaching sessions and exam sessions need rooms, teachers and groups to be free.

## Decision
`academic` owns `TeachingSession`, `assessment` owns `ExamSession`, `scheduling` owns `Room`, `Booking`
and the reusable reservation service. `scheduling` knows nothing about courses or exams.

## Consequences
An exam cannot overlap a class session. `scheduling` has no dependency on `assessment`.
