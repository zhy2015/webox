# Rule Hardening and Repeatable Verification

Date: 2026-08-17

## Corrections

- Fixed checkout to accept the same dish in multiple lines when each line has a different customization.
- Aggregate stock is now checked across every customization of the same dish before any inventory is deducted.
- Made the business clock injectable so the 10:00 lunch and 15:00 dinner boundaries are deterministic in tests.
- Standardized unknown-option validation to return `UNKNOWN_OPTION` before checking required groups.
- Extracted allergen approval into a pure frontend rule while preserving the existing confirmation experience.
- Rejects administrator supply reductions below the quantity already allocated to active orders, preventing cancellation from corrupting inventory accounting.
- Added an active-order lookup that applies the server-side cutoff slot, allowing checkout to show `View existing order` before submission.

## Automated coverage

- Eleven backend tests cover customization pricing, aggregate stock, stock-adjustment accounting, the five-item limit, invalid options, idempotent replay, active meal-slot uniqueness and lookup, cancellation restoration and cutoff boundaries.
- Four frontend tests cover cart configuration identity, five-item enforcement, integer-cent totals and allergen approval.
- `scripts/smoke-api.sh` verifies real-MySQL registration validation, employee authorization, preference persistence, invalid budgets, administrator inventory propagation and restoration, multi-configuration checkout, idempotent replay and cancellation.
- `scripts/verify.sh` runs infrastructure startup, backend tests, frontend checks/build, health/OpenAPI checks and the API smoke as one command.
- `scripts/smoke-concurrency.sh` proves that two real employee sessions competing for the final item produce one success and one `INSUFFICIENT_STOCK` without overselling.
- `scripts/verify-clean-db.sh` proves Flyway and seed startup against a disposable empty MySQL instance without touching the demonstration database.
- Final browser verification confirmed the server-resolved `View existing order` action and cleared the temporary cart afterward.

## Remaining risks

- Browser smoke is currently manual rather than committed as an automated browser suite.
