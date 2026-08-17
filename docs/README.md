# Documentation

| Directory | Contents |
| --- | --- |
| `requirements/` | Normalized product scope, business rules, acceptance criteria, and open questions |
| `technical-design/` | Architecture, module boundaries, data model, concurrency, security, and runtime flows |
| `api/` | API conventions and the generated OpenAPI contract entry point |
| `testing/` | Test levels, critical scenarios, fixtures, and quality gates |
| `deployment/` | Local setup, configuration, deployment, rollback, and operations |
| `project-structure/` | Repository ownership rules and directory map |
| `adr/` | Immutable architecture decision records and supersession history |
| `commit-records/` | Human-readable delivery records linked to commits or work sessions |

The original PRD remains under `参考资料/`. Documents here normalize it for implementation but do not silently override it. Any intentional product change must be recorded in requirements and in an ADR or change record as appropriate.

The current implementation driver is the root `TODO.md`; its completion checks override optimistic status reporting. The 90-minute simplifications and stop rules are defined in `technical-design/initial-delivery-solution.md` and ADR 0002.
