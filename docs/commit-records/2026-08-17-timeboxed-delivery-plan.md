# Timeboxed Delivery Plan

## Scope

Converted the general architecture into a strict 90-minute implementation plan with prioritized TODO items, validation criteria, stop rules, and explicit deferred work.

## Decisions

- Pull transactional inventory into P0 while pausing SSE inventory presentation.
- Use a single-node server HTTP session for the initial release.
- Use MySQL JSON for customization structures and immutable option snapshots.
- Permit predefined/local image paths or image URLs in P0; multipart upload is P1.
- Pause AI, dashboard, Redis, microservices, object storage, and non-critical polish.

## Environment evidence

- Default Java and `javac`: Corretto 8. OpenJDK 17.0.16 is already installed and only needs to be activated for the project shell.
- Maven: 3.9.10.
- Node.js/npm: 24.3.0 / 11.14.0.
- Docker/Compose: installed, but the Docker daemon is not running.

## Verification

- Environment commands were executed read-only.
- Application tests are not yet available because implementation has not started.
- Documentation link validation must be rerun after this change.
