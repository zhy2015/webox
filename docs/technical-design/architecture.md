# Technical Architecture

> Active delivery profile: the 90-minute implementation uses the simplifications and stop rules in `initial-delivery-solution.md` and ADR 0002. This document remains the long-term architecture direction.

## 1. Design goal

Use a modular monolith to deliver a reliable full-stack system quickly without creating distributed-system overhead. Frontend and backend deploy independently, while MySQL is the durable system of record.

```text
Browser (Employee / Console)
        |
        | HTTPS, JSON, SSE for optional live/AI streams
        v
React SPA ---- same-origin /api proxy ---- Spring Boot API
                                             |
                                             | transactions, parameterized queries
                                             v
                                    Standalone MySQL 8
                                             |
                           +-----------------+----------------+
                           |                                  |
                  image storage adapter              optional LLM provider
```

Redis is intentionally absent from the baseline. Menu metadata may use a bounded in-process cache only after invalidation rules are implemented. Live inventory is always confirmed in MySQL transactions.

## 2. Frontend boundaries

- `app/`: routing, providers, authentication bootstrap, error boundaries, and role-aware shells.
- `features/auth`, `menu`, `cart`, `checkout`, `orders`, `preferences`, `console`, and optional `assistant`/`dashboard`.
- `shared/`: design tokens, accessible UI primitives, API client, validation helpers, decimal money formatting, and test fixtures.
- TanStack Query owns server state. A small dedicated store owns the local cart and versioned persistence.
- Routes and navigation improve usability but never replace backend authorization.
- The development server proxies `/api` to avoid divergent browser security behavior between local and deployed environments.

## 3. Backend boundaries

The backend is grouped by business capability, not only by technical layer:

| Module | Responsibility |
| --- | --- |
| `auth` | Registration, password hashing, session lifecycle, roles, and current user |
| `catalog` | Dish definitions, categories, allergens, customization groups/options, and publish state |
| `menu` | Date-specific dish availability and supply configuration |
| `preference` | Employee allergens, cuisines, taste, spice, and budget |
| `order` | Checkout policy, snapshots, totals, idempotency, order history, status, and cancellation |
| `inventory` | Transactional deduction/restoration and stock change events |
| `admin` | Console use cases and administrator-only policies |
| `recommendation` | Provider-neutral recommendation orchestration and output validation |
| `dashboard` | Read models and real-order aggregates |

Modules may call another module’s application interface. They must not reach into another module’s repository or mutate its entities directly.

## 4. Core data model

The initial schema should include:

- `users`, `user_sessions`, `user_addresses`, and `dietary_preferences` plus normalized preference join tables.
- `dishes`, `dish_allergens`, `dish_option_groups`, and `dish_options`.
- `daily_menus` and `daily_menu_items` with date, configured supply, and remaining supply.
- `orders`, `order_items`, and `order_item_options` with commercial snapshots.
- `order_idempotency` with a unique `(user_id, idempotency_key)` constraint and stored response/order reference.
- `active_order_slots` with a unique `(user_id, delivery_date, meal_period)` guard for `Pending`/`Confirmed` orders.

Use UUID/ULID-style opaque API identifiers while retaining efficient database keys where appropriate. Store money as `DECIMAL(12,2)` and map it to `BigDecimal`; the frontend exchanges fixed decimal strings.

## 5. Checkout transaction

1. Authenticate the employee and validate the idempotency key.
2. If the key already completed, return its original order response.
3. Validate input limits, configured options, date/meal cutoff, and the five-item cap.
4. Acquire the active meal-slot guard and lock the relevant daily-menu rows in deterministic order.
5. Recalculate unit prices, option charges, totals, publish/menu eligibility, and inventory.
6. Persist the order and immutable item/option snapshots.
7. Deduct inventory and complete the idempotency record in the same transaction.
8. Commit, then publish best-effort stock events. A later outbox enhancement can provide durable event delivery.

Unique constraints are the final duplicate guard. UI button disabling is only a user-experience measure.

## 6. Cancellation transaction

Lock the order, verify ownership and `Pending` state, transition to `Cancelled`, restore each inventory row exactly once, release the active slot, and commit. Repeated cancellation returns a stable conflict without restoring stock twice.

## 7. Authentication and security

- Prefer a server-managed session stored durably in MySQL and sent through an `HttpOnly`, `Secure` in production, `SameSite=Lax` cookie.
- Enable CSRF protection for state-changing requests and use the SPA-compatible token flow.
- Hash passwords with an adaptive algorithm supported by Spring Security.
- Enforce roles in service methods and HTTP security rules.
- Validate upload content type, size, generated filename, and storage path.
- Use bean validation, parameterized repositories, response DTOs, and centralized English error mapping.
- Apply request size limits and focused rate limits to authentication, checkout, and AI endpoints.

## 8. Caching and high-traffic behavior

- Start with indexed MySQL queries, HTTP caching for immutable images, pagination, and client query caching.
- If measured load requires it, cache menu catalog projections by date in Caffeine, excluding or separately versioning volatile remaining stock.
- Invalidate affected keys after dish publish/edit and daily-menu changes.
- Never trust cached stock during checkout; database locks and constraints decide availability.

## 9. Live inventory and AI streaming

Server-Sent Events are sufficient for one-way stock updates and streamed AI recommendation events. Clients reconnect with bounded backoff and always refetch authoritative state after reconnect. The AI provider receives a server-curated candidate set, not unrestricted user/database content; returned dish IDs are validated before emission.

## 10. Observability and failure behavior

- Structured logs include request/correlation ID, route, latency, outcome, and safe actor ID.
- Actuator exposes separate liveness and readiness; readiness depends on MySQL.
- Metrics cover request latency/error rate, checkout conflicts, idempotency replay, low-stock failures, and AI provider failures.
- Missing AI configuration disables only the optional assistant and returns a stable capability response.
- Image or live-stream failures retain usable dish/order flows with a visible fallback.
