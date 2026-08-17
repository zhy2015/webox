# P0 Initial Implementation

Date: 2026-08-17

## Delivered

- Spring Boot modular monolith with session authentication, role authorization, menu, preferences, order and Console APIs.
- MySQL 8.4 Compose service, Flyway schema, idempotent English demo data and 20 bundled food assets.
- Transactional checkout with server-side pricing, cut-off normalization, active-slot uniqueness, idempotency keys and row-locked inventory.
- Responsive React employee application and administrator Console.
- Employee login, menu filtering, required-option customization, persisted cart, checkout, order history, cancellation and preference forms.
- Administrator dish editing and daily-menu inventory controls.

## Verification evidence

- `cd backend && ./mvnw -q test`: passed with JDK 17.
- `frontend/npm run typecheck`: passed.
- `frontend/npm test`: 3 cart model tests passed.
- `frontend/npm run build`: passed.
- `/actuator/health`: returned `UP` against the Compose MySQL instance.
- Browser employee smoke: sign in, choose required Quinoa option, add to cart, place order, open order history and cancel a pending order.
- Browser Console smoke: administrator sign in, load nine seeded dishes with their daily stock and save the daily menu.
- Responsive check: employee menu at 390 x 844 rendered without horizontal overflow or overlapping controls.

## Open release work

- Add automated backend coverage for price validation, cut-off boundaries, idempotent replay, concurrent inventory and cancellation restoration.
- Add automated browser coverage for the manual smoke flow and administrator-to-employee menu propagation.
- Rehearse an empty-volume release from a clean MySQL data directory.
- Export the complete raw AI conversation into `ai-conversations/` before final submission.
