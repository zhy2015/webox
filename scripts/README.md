# Scripts

This directory contains the repeatable developer and CI entry points for test orchestration and smoke verification.

Scripts are non-interactive, fail on errors, print actionable diagnostics, and avoid embedding secrets.

## Commands

- `./scripts/smoke-api.sh`: against a running backend, creates a unique employee, verifies Console denial, reads the real menu, places an idempotent order and cancels it. Override the API URL with `WEBOX_API_BASE`.
- `./scripts/smoke-concurrency.sh`: constrains one item to a single remaining unit, submits two employee orders concurrently, requires one success and one `INSUFFICIENT_STOCK`, then cancels and restores the original menu.
- `./scripts/verify-clean-db.sh`: starts a disposable MySQL container on a random port, runs Flyway and demo seeding through a temporary backend, verifies expected row counts, then removes the isolated container.
- `./scripts/verify.sh`: starts MySQL, runs backend tests, installs locked frontend dependencies, runs frontend checks/build, starts the backend when needed, checks health/OpenAPI and executes the API smoke.

The API smoke leaves an acceptance user and a cancelled order in the local database for traceability. It does not delete or reset existing data.
