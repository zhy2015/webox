# Test Strategy

## Test levels

| Level | Scope | Primary tools |
| --- | --- | --- |
| Backend unit | cutoff policy, pricing, option validation, quantity cap, status transitions | JUnit 5 |
| Backend integration | controllers, security, Flyway, repositories, transactions, idempotency, concurrent inventory | Spring Boot Test + Testcontainers MySQL |
| Frontend unit/component | money display, cart identity, forms, allergen dialog, filters, protected navigation | Vitest + Testing Library |
| End to end | real browser through frontend/backend/MySQL for critical employee and Console journeys | Playwright |
| Operational smoke | health, migrations, seed data, login, menu, checkout, cancellation | script-driven HTTP/browser checks |

H2 or another embedded database must not be used as a substitute in integration tests because MySQL locking, indexes, generated behavior, and SQL semantics are part of correctness.

## Critical test cases

- Submit the same idempotency key concurrently and assert one order, one stock deduction, and identical successful replay.
- Submit different keys for the same user/date/meal and assert the active-slot constraint permits only one effective order.
- Race multiple users for the last items and assert no negative inventory or partial order.
- Cancel the same pending order concurrently and assert one transition and one restoration.
- Verify cutoff boundaries immediately before, at, and after 10:00/15:00 in `Asia/Shanghai` with an injected clock.
- Recalculate prices from catalog options and reject stale, unknown, duplicated, or incompatible selections.
- Confirm the sixth total item is blocked regardless of line count.
- Confirm allergen warning rejection and acceptance each produce exactly the intended cart mutation.
- Confirm employees cannot call Console endpoints even when client routes are manipulated.
- Confirm order history retains old names, options, and prices after catalog edits.
- Confirm every visible tested UI string and seeded dish value is English.

## Fixtures and time

Seed at least one employee, one administrator, all required enumerations, the nine PRD example dishes, configurable options, and an orderable menu for the current business date. Tests use a fixed clock and explicit dates; the demo seed script creates a fresh menu for the actual run date.

## Quality gates

- All baseline tests pass on a clean MySQL schema.
- Migrations apply from zero and do not depend on developer-local state.
- Backend formatting/static analysis, frontend type-check/lint, unit tests, and a critical Playwright smoke path pass.
- No test relies on execution order, an external paid LLM, or a developer’s local timezone.
- Optional AI tests use a deterministic fake provider for contract behavior plus a separate opt-in live probe.
