# ADR 0001: Modular Monolith and Standalone MySQL

- Status: Accepted
- Date: 2026-08-17

## Context

WeBox serves approximately 200 employees, has a concentrated ordering window, requires a complete Java 17/Spring Boot backend and independent MySQL, and must be delivered and operated as one coherent product. Correct checkout, authorization, idempotency, and inventory transactions matter more than independent service scaling.

## Decision

Build one Spring Boot deployable organized as business-capability modules, one React SPA with Employee and Console shells, and one standalone MySQL database. Deploy frontend and backend independently but keep the API under a common trusted origin in production.

Do not introduce Redis or microservices in the baseline. Use transactional MySQL constraints and row locks for commercial correctness. Add bounded in-process caching only for measured read pressure and keep live stock authoritative in MySQL.

## Consequences

- Checkout and cancellation can use local database transactions with clear failure semantics.
- Development, testing, and deployment remain understandable within the challenge and small-team context.
- Module boundaries require code review discipline because they are not network-enforced.
- A single backend is a scaling unit, which is acceptable for the stated load and can be replicated later if session and event designs support it.
- Optional LLM and image storage integrations remain adapters rather than core infrastructure dependencies.

## Alternatives considered

- Microservices: rejected because they add distributed transactions, deployment burden, and failure modes without a stated scaling need.
- Redis as a mandatory stock/idempotency authority: rejected because the PRD makes it optional and MySQL already provides durable constraints and transactions.
- Embedded database for local/test use: rejected because the PRD explicitly requires independent MySQL and concurrency semantics must be tested against the production database family.
