# ADR-006: Session and booking share one transaction

- Status: Proposed (confirm on Day 1)

## Context
DDD recommends one aggregate per transaction, but a session must never exist without its booking,
and conflicts must be rejected immediately.

## Decision
`TeachingSession` and `ExamSession` are saved in the same JTA transaction as their `Booking`.
`BookingService.reserve` joins the caller's transaction and flushes so conflicts surface inside the call.

## Consequences
A conflict rolls back the whole use case. Contexts are coupled through a synchronous call, which is
acceptable inside one deployable application.
