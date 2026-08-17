# WeBox

WeBox is an internal employee meal ordering platform for approximately 200 users. Employees browse the daily menu, customize dishes, manage a cart, place and cancel orders, and maintain dietary preferences. Administrators use a separate Console experience to manage dishes and daily menus.

The source requirement is `参考资料/AI Vibe Coding V3.0 - PRD.md`. All end-user UI, seed data, validation messages, and AI-generated recommendations must be in English. Engineering documentation may be written in Chinese.

## Five-minute reviewer path

```bash
git clone git@github.com:zhy2015/webox.git
cd webox
./scripts/verify.sh
```

Prerequisites are Docker Desktop, Node.js 20+ and JDK 17. The verification command starts MySQL, runs all backend and frontend checks, exercises the real API, proves concurrent inventory safety, and rehearses migrations and seed data against an isolated empty database. It does not delete the normal demonstration database.

After verification, follow [Run locally](#run-locally) and open `http://127.0.0.1:5173` to inspect the employee and administrator workflows.

## Delivery scope

- Tier 1: authentication, daily menu, search and filtering, cart, checkout, order history, order details, and pending-order cancellation.
- Tier 2: dietary preferences, meal-ordering rules, role-based access, and Console dish/daily-menu management.
- Tier 3: concurrency-safe inventory with live updates, streaming AI recommendations, and a real-order operations dashboard.

Tier 1 and Tier 2 are the delivery baseline. Tier 3 is additive and must not destabilize the baseline.

The active implementation is constrained to a strict 90-minute initial delivery. Transactional inventory is pulled into the baseline because order correctness is more important than live stock presentation. AI recommendations, dashboards, SSE, Redis, and non-critical optimization are paused until the P0 release gate passes.

## Technology stack

- Frontend: React 19, TypeScript, Vite, React Router, TanStack Query, Lucide, and Vitest.
- Backend: Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA, Flyway, springdoc-openapi, and JUnit 5.
- Data: a standalone MySQL 8 service. Money is stored and calculated with decimal-safe types, never floating point.
- Runtime: Docker Compose for local infrastructure; frontend and backend remain independently buildable.

Exact dependency versions are pinned in `frontend/package-lock.json` and resolved by `backend/mvnw` from `backend/pom.xml`.

## Repository map

| Path | Purpose |
| --- | --- |
| `backend/` | Spring Boot service, migrations, backend tests, and service configuration |
| `frontend/` | Employee and Console SPA, frontend tests, and build configuration |
| `docs/` | Requirements, technical design, API, tests, deployment, decisions, and change records |
| `infra/` | Local and deployment infrastructure definitions |
| `scripts/` | Repeatable setup, verification, seed, and developer workflow scripts |
| `assets/` | Curated source assets and explicit asset-to-data mappings |
| `ai-conversations/` | Raw, unedited AI coding conversation exports required by the PRD |
| `参考资料/` | Original task inputs; keep unchanged for traceability |

See [the project structure guide](docs/project-structure/README.md) for ownership and dependency rules.

## Current status

The P0 initial application is executable. Employee authentication, menus, customization, cart, checkout, orders, cancellation and preferences are connected to MySQL. The administrator Console supports dish and daily-menu management. Transactional inventory, server-side price calculation, cut-off handling and idempotency are implemented.

The browser smoke flow and responsive menu have been verified. Eleven backend rule tests, four frontend state tests, the frontend production build, real-MySQL API smoke, concurrent inventory race and isolated clean-database rehearsal pass. The remaining submission gaps are automated browser coverage and the required raw AI conversation export; see [TODO.md](TODO.md) for the evidence-based status.

## Maintainer and AI handoff

Read these files in order before changing code:

1. `README.md` for executable commands and current release state.
2. [`TODO.md`](TODO.md) for verified completion status, P1 candidates and intentionally paused work.
3. [`docs/requirements/product-requirements.md`](docs/requirements/product-requirements.md) and the untouched source PRD under `参考资料/` for product behavior.
4. [`docs/technical-design/architecture.md`](docs/technical-design/architecture.md) and [`docs/api/README.md`](docs/api/README.md) for module and API boundaries.
5. [`docs/commit-records/2026-08-17-rule-hardening-and-verification.md`](docs/commit-records/2026-08-17-rule-hardening-and-verification.md) for the latest fixes and verification evidence.

Architecture invariants:

- Keep the backend a Java 17 Spring Boot modular monolith and the frontend a separate React SPA.
- MySQL is authoritative and mandatory. Do not substitute H2, SQLite or in-memory persistence.
- The backend owns permissions, business time, prices, customization validation, active meal slots, idempotency and inventory.
- Preserve row locking and the database uniqueness constraints around inventory, idempotency and active meal slots.
- Use fixed-decimal money on the backend and integer cents in frontend calculations.
- Keep all user-facing copy and seed content in English. Preserve `参考资料/` unchanged.
- Do not start P1 or paused Tier 3 work by weakening a completed P0 acceptance rule.

Before handing off any change, run `./scripts/verify.sh`. If only documentation changed, at minimum validate local Markdown links and keep `TODO.md` evidence-based. The complete raw AI conversation export is intentionally still marked incomplete because it must come from the coding tool itself and must not be reconstructed from summaries.

## Run locally

On macOS, the commands below explicitly select JDK 17 because another system Java may be the default. On Linux or Windows, ensure `JAVA_HOME` already points to JDK 17.

```bash
cd infra
docker compose up -d --wait

cd ../backend
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw spring-boot:run
```

In a second terminal:

```bash
cd frontend
npm ci
npm run dev -- --host 127.0.0.1
```

Open `http://127.0.0.1:5173`. The Vite server proxies the API and dish images to Spring Boot.

| Role | Email | Password |
| --- | --- | --- |
| Employee | `employee@webox.local` | `Lunch123` |
| Administrator | `admin@webox.local` | `Admin123` |

Health: `http://localhost:8080/actuator/health`

OpenAPI UI: `http://localhost:8080/swagger-ui.html`

Run the complete local verification from the repository root:

```bash
./scripts/verify.sh
```

## Documentation entry points

- [Requirements baseline](docs/requirements/product-requirements.md)
- [Acceptance matrix](docs/requirements/acceptance-criteria.md)
- [Technical architecture](docs/technical-design/architecture.md)
- [90-minute initial technical solution](docs/technical-design/initial-delivery-solution.md)
- [Execution TODO and completion checks](TODO.md)
- [API conventions](docs/api/README.md)
- [Test strategy](docs/testing/test-strategy.md)
- [Local deployment plan](docs/deployment/local-development.md)
- [Decision records](docs/adr/)
- [Change records](docs/commit-records/)
