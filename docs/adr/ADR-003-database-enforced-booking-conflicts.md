# ADR-003: Database-enforced booking conflicts

- Status: Approved

## Context
A SELECT followed by an INSERT lets two simultaneous requests both pass the check.

## Decision
Check availability in Java for a clear error message, and enforce the rule with a PostgreSQL exclusion
constraint (`btree_gist`) on resource and time range.

## Consequences
Concurrency is safe. PostgreSQL is required, and tests must run on a real PostgreSQL (Testcontainers).
