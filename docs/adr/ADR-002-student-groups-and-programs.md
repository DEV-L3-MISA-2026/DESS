# ADR-002: StudentGroup is explicit; programs are in the MVP

- Status: Amended

## Context
Teaching cohorts and academic programs are different concepts.

## Decision
`StudentGroup` is its own concept (never an organizational unit). `AcademicProgram`, `ProgramVersion`
and `ProgramEnrollment` are part of the MVP; a program enrollment does not replace a group enrollment.

## Consequences
Full curriculum management, credits and prerequisites stay post-MVP.
