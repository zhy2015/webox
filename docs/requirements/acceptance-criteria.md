# Acceptance Matrix

The baseline is complete only when the browser flow, API behavior, and durable MySQL state agree. “Page exists” is not sufficient.

| ID | Priority | Acceptance outcome |
| --- | --- | --- |
| AUTH-01 | Tier 1 | A valid employee registers, signs in, refreshes, and remains authenticated. |
| AUTH-02 | Tier 1 | Duplicate email, malformed email, weak password, overlong input, and bad credentials return safe English feedback. |
| AUTH-03 | Tier 2 | An employee receives `403` from Console APIs and cannot render protected Console routes. |
| MENU-01 | Tier 1 | The daily menu loads from MySQL and shows image, English name, exact price, and category on mobile and desktop. |
| MENU-02 | Tier 1 | Search matches name/description; multiple category filters combine correctly and can be cleared. |
| MENU-03 | Tier 1 | Required options, optional add-ons, and option-dependent price are represented consistently in details and cart. |
| CART-01 | Tier 1 | Same dish/configuration merges; different configurations remain separate; totals use decimal-safe arithmetic. |
| CART-02 | Tier 2 | The sixth total item is blocked in menu and cart with an English explanation. |
| CART-03 | Tier 2 | A flagged allergen triggers confirmation; rejecting it makes no cart change and accepting adds exactly once. |
| ORDER-01 | Tier 1 | Checkout stores one order with correct snapshots, quantities, options, subtotals, total, address, date, and meal period. |
| ORDER-02 | Tier 1 | Parallel or repeated requests sharing an idempotency key produce one order and one commercial effect. |
| ORDER-03 | Tier 2 | Cutoff logic uses server business time and selects same-day dinner or next-day lunch as specified. |
| ORDER-04 | Tier 2 | A second `Pending` or `Confirmed` order in the same user/date/meal slot is blocked and links to the existing order. |
| ORDER-05 | Tier 1 | An employee sees only their orders; cancelling `Pending` succeeds once and all other cancellation attempts are rejected. |
| PREF-01 | Tier 2 | Preferences persist, `For You` changes deterministic ranking/highlighting, and disabling it restores default order. |
| PREF-02 | Tier 2 | Exceeding budget maximum warns but still permits a valid order. |
| ADMIN-01 | Tier 2 | An administrator can search, create, edit, publish, and unpublish dishes; employee visibility follows publish state. |
| ADMIN-02 | Tier 2 | An administrator can configure today or tomorrow’s menu and non-negative supply quantity. |
| STOCK-01 | P0 timebox | Concurrent orders never reduce daily inventory below zero; failure names insufficient dishes. |
| STOCK-02 | P0 timebox | Cancelling a pending order restores inventory exactly once. |
| STOCK-03 | Tier 3 paused | Another open menu receives updated inventory without a manual refresh. |
| AI-01 | Tier 3 | Streaming recommendations contain only in-stock, allergen-safe dish IDs and English personalized reasons. |
| DASH-01 | Tier 3 | Every displayed metric reconciles with real order and inventory records for the selected period. |
| OPS-01 | Baseline | A new developer can start MySQL, migrate/seed data, run both apps, and complete an order using documented steps. |
| DOC-01 | Baseline | README, OpenAPI, architecture, tests, deployment notes, and raw AI conversation exports are present. |

## Release gates

- Tier 1 and Tier 2 tests pass before Tier 3 work is accepted.
- No committed credential or plaintext password exists.
- A clean database can migrate and seed the current day’s orderable menu.
- The smoke flow covers register/sign in, browse, customize, cart, checkout, order detail, and pending cancellation.
- All visible UI text in the tested paths is English.
