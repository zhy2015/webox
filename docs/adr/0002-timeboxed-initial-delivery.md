# ADR 0002: 90-Minute Initial Delivery Scope

- Status: Accepted
- Date: 2026-08-17

## Context

The implementation has a strict 90-minute timebox. The product should be demonstrably complete before optional optimization. The workspace currently has no application code, the default Java runtime is 8 rather than the required 17, and the installed Docker daemon is not running.

## Decision

Prioritize one real MySQL-backed employee ordering flow and minimum Console management. Implement server-authoritative price calculation, meal cutoffs, order idempotency, active-slot uniqueness, and transactional inventory in the baseline because later UI polish cannot repair incorrect commercial data.

Use a Spring Boot modular monolith, one React SPA, server-side HTTP sessions for the single-node initial release, MySQL JSON for low-query customization structures and order snapshots, and local/predefined image paths before multipart upload.

Pause AI recommendations, dashboards, SSE inventory updates, Redis, object storage, distributed sessions, microservices, and non-critical visual optimization until the P0 release gate passes.

## Consequences

- The first release remains small enough to finish and demonstrate under the timebox.
- Inventory is transactionally correct even though other browsers do not receive push updates yet.
- The initial session implementation is single-node and does not survive backend restart; Spring Session JDBC is a later hardening task.
- JSON configuration accelerates delivery but may be normalized later if querying and administration requirements expand.
- Image URL/predefined-path input temporarily falls short of full upload convenience but keeps dish management functional.

## Stop rules

- At 60 minutes, a browser must complete a real order or all new features stop.
- At 72 minutes, no new dependency or architecture change is allowed.
- At 82 minutes, only demo-blocking defects, clean startup, smoke verification, and documentation may change.
- A P1 item is removed immediately if it delays or regresses a P0 path.
