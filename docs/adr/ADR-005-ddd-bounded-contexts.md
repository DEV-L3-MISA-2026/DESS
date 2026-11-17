# ADR-005: DDD bounded contexts with api/internal layering

- Status: Proposed (confirm on Day 1)

## Context
Ten developers work in parallel for ten days and must not break each other's modules.

## Decision
Each module is a bounded context with `api/` (published interfaces) and `internal/{domain,application,
infrastructure,rest}`. Aggregates reference each other by UUID. Domain classes carry JPA annotations
directly (no separate persistence model). `ArchitectureTest` enforces the dependency rules.

## Consequences
Clear ownership and testable domain rules without a database. Domain events, CQRS and typed ID wrappers
are out of the MVP.
