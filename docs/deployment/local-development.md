# Local Development and Deployment

## Status

This runbook was verified on 2026-08-17 with MySQL 8.4, JDK 17.0.16 and the locked frontend dependencies.

## Prerequisites

- JDK 17
- A project-pinned Maven wrapper
- Node.js 20 or newer and npm
- Docker with Compose support for standalone MySQL

## Local sequence

1. Start MySQL from the repository root:

   ```bash
   cd infra
   docker compose up -d --wait
   ```

2. Start the backend with JDK 17. Flyway applies the schema and the application creates idempotent demo data for today and tomorrow.

   ```bash
   cd ../backend
   export JAVA_HOME=$(/usr/libexec/java_home -v 17)
   export PATH="$JAVA_HOME/bin:$PATH"
   ./mvnw spring-boot:run
   ```

3. Start the frontend in another terminal:

   ```bash
   cd frontend
   npm ci
   npm run dev -- --host 127.0.0.1
   ```

4. Open `http://127.0.0.1:5173`. Verify backend health at `http://localhost:8080/actuator/health` and OpenAPI at `http://localhost:8080/swagger-ui.html`.

The seed must create a current-day orderable menu regardless of the calendar date. It must also print non-secret demo account identifiers without logging passwords in production environments.

## Configuration contract

| Setting group | Examples | Rule |
| --- | --- | --- |
| MySQL | host, port, database, username, password | Required; no committed secrets |
| Application | business timezone, frontend origin, session duration | Explicit defaults for local only |
| Uploads | provider, local path/bucket, size limit | Persistent path outside app artifact |
| AI | enabled flag, provider URL, model, API key | Optional; baseline starts without it |

Provide a checked-in `.env.example` containing safe placeholders and comments. Production configuration comes from the platform secret store.

## Production shape

- Build immutable frontend and backend artifacts in CI.
- Serve the SPA through a static host or edge proxy and forward `/api` to Spring Boot under one trusted origin.
- Run MySQL as a separately managed persistent service with backups and restricted network access.
- Store uploads in durable object storage; do not rely on container-local disk.
- Run migrations as a controlled release step before shifting traffic.
- Verify readiness, schema version, login, menu read, and a non-destructive API smoke check after deployment.

## Rollback and recovery

- Application releases must be backward compatible with the currently deployed schema for at least one rollout window.
- Prefer additive migrations; destructive schema cleanup occurs only after old code is retired and backups are verified.
- Roll back application artifacts independently from database recovery.
- Document restoration testing for MySQL and object storage before declaring production readiness.
