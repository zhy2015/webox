# API Contract

The backend generates an OpenAPI 3 contract from the implemented controllers and DTOs. The checked behavior at `/swagger-ui.html` is authoritative; this file defines conventions and the current resource surface.

## Conventions

- Base path: `/api/v1`.
- JSON fields use `camelCase`; timestamps use ISO 8601 with an explicit offset.
- Money uses fixed decimal strings plus `currency: "CNY"` where ambiguity exists.
- Dates use `YYYY-MM-DD`; meal periods and status values use the PRD’s English enums.
- Collection endpoints accept bounded pagination and deterministic sorting.
- State-changing requests require CSRF protection under cookie-based authentication.
- Checkout requires an `Idempotency-Key` header.
- Validation failures return field errors; business conflicts return stable machine codes and English messages.

Example error envelope:

```json
{
  "code": "ORDER_SLOT_ALREADY_EXISTS",
  "message": "You already have an active lunch order for this date.",
  "correlationId": "01K...",
  "fieldErrors": []
}
```

## Implemented resources

| Area | Representative endpoints |
| --- | --- |
| Auth | `GET /auth/csrf`, `POST /auth/register`, `POST /auth/login`, `POST /auth/logout`, `GET /auth/me` |
| Menu | `GET /menus/{date}`, `GET /dishes/{dishId}` |
| Preferences | `GET /me/preferences`, `PUT /me/preferences` |
| Orders | `POST /orders`, `GET /orders`, `GET /orders/active`, `GET /orders/{orderId}`, `POST /orders/{orderId}/cancel` |
| Console dishes | `GET/POST /console/dishes`, `PUT /console/dishes/{dishId}`, `PATCH /console/dishes/{dishId}/status` |
| Console menus | `GET/PUT /console/menus/{date}` |

Inventory streaming, AI recommendations, the operations dashboard and a dedicated address resource are deferred. Generated examples use English seed data and redact authentication material.
