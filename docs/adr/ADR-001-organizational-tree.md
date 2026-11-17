# ADR-001: Plain organizational tree plus HierarchyService

- Status: Approved

## Context
Institutions are described by a tree of organizational units (campus, faculty, department, school, level).

## Decision
Store a plain tree (parent id per unit) and use a `HierarchyService` (domain service) for rules and
recursive queries. No Composite pattern classes in the MVP.

## Consequences
Simple persistence and easy XML import. Recursive queries rely on PostgreSQL, so tests use a real database.
