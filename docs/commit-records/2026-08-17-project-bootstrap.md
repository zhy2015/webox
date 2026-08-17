# Project Bootstrap

## Scope

Established the WeBox repository structure and converted the V3.0 PRD into an implementation-oriented baseline.

## Decisions

- Preserve `参考资料/` unchanged as the source package.
- Use separate `backend/` and `frontend/` builds in one repository.
- Use a modular Spring Boot monolith with standalone MySQL.
- Keep Redis outside the baseline and isolate optional AI functionality.
- Add explicit infrastructure, scripts, assets, AI export, API, testing, and ADR ownership boundaries.

## Documents added

- Root overview and repository ownership map.
- Normalized requirements and acceptance matrix.
- Technical architecture, API conventions, test strategy, and deployment plan.
- ADR and change-record conventions.

## Verification

- Cross-checked the Markdown PRD, its HTML reading copy, the package README, and all 20 referenced image files.
- Confirmed the workspace did not contain an existing Git repository or application implementation at bootstrap time.
- Application build and runtime verification are not applicable yet because code has not been generated.

## Follow-up

Generate the frontend/backend project skeletons, pin toolchain versions, add MySQL Compose and Flyway migrations, seed a current-day menu, and verify the first end-to-end authentication/menu/order slice.
