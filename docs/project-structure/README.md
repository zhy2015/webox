# Project Structure

```text
webox/
├── backend/                 Java 17 + Spring Boot application and tests
├── frontend/                React + TypeScript SPA and tests
├── docs/
│   ├── requirements/        Product baseline and acceptance criteria
│   ├── technical-design/    Architecture and detailed designs
│   ├── api/                 API conventions and contract pointers
│   ├── testing/             Test strategy and quality gates
│   ├── deployment/          Local and production runbooks
│   ├── project-structure/   Repository ownership and dependency rules
│   ├── adr/                 Architecture decision records
│   └── commit-records/      Human-readable delivery history
├── infra/                   Compose and deployment infrastructure
├── scripts/                 Repeatable setup, test, seed, and smoke commands
├── assets/                  Curated source assets and mapping manifests
├── ai-conversations/        Required raw AI conversation exports
├── 参考资料/                 Original immutable requirement package
└── README.md                Concise project entry point
```

## Ownership rules

- `backend/` and `frontend/` are independently buildable and do not import source from each other.
- Their shared contract is HTTP/OpenAPI, not copied Java/TypeScript domain models. Generated clients may be produced from OpenAPI into a clearly generated frontend path.
- `infra/` starts dependencies and deploys artifacts; it does not contain business logic.
- `scripts/` orchestrates documented commands and must not become a second implementation layer.
- `assets/` contains curated inputs. Runtime uploads and database files belong in ignored persistent volumes.
- `参考资料/` is evidence, not application runtime content. Changes require an explicit source-material correction record.
- `docs/adr/` records durable decisions; `docs/commit-records/` records what changed and how it was verified.

## Expected backend tree

```text
backend/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── src/
    ├── main/
    │   ├── java/.../webox/   bootstrap, shared kernel, and feature modules
    │   └── resources/        application config and Flyway migrations
    └── test/                 unit and MySQL integration tests
```

## Expected frontend tree

```text
frontend/
├── package.json
├── package-lock.json
├── e2e/
└── src/
    ├── app/                  router, providers, shells, and bootstrapping
    ├── features/             business-capability slices
    ├── shared/               UI primitives, API, utilities, and tokens
    └── test/                 shared test setup and fixtures
```

## Why the additional root directories exist

- `infra/` keeps independent MySQL and later deployment definitions out of application code.
- `scripts/` gives reviewers one repeatable path instead of scattered manual commands.
- `assets/` prevents numeric reference filenames from becoming an undocumented data contract.
- `ai-conversations/` is a mandatory PRD deliverable and must be visible from day one.
- `docs/api`, `docs/testing`, and `docs/adr` close gaps in the original four documentation categories: frontend/backend coordination, verifiable quality, and long-term decision history.

Avoid creating more top-level directories until a real ownership boundary appears. In particular, a separate shared library, microservice, Redis layer, or Kubernetes directory is premature for the current scale.
