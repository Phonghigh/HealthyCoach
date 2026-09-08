# Phase 02 — Project scaffolding

## Context Links
- [plan.md](./plan.md) · [phase-01](./phase-01-verify-health-connect-data-path.md)
- `docs/system-architecture.md` §2 components, §3 data model
- `docs/code-standards.md` §1 structure, §4 testing

## Overview
- **Priority:** P0 — blocks 03–08.
- **Status:** done
- **Description:** Create the repo skeleton: NestJS backend with an empty-but-wired module layout, Postgres schema + migrations, a Compose Android app shell with navigation, and a CI job that compiles + tests both.

## Key Insights
- **Why a domain-module layout now:** the NFR "rules must be testable independently of LLM/UI" (`docs/project-overview-pdr.md` §5) is an *architecture* requirement, not a coding-style one. Getting `rules/` as a pure-function package with zero NestJS imports from day one is what makes it cheap later. Retrofitting purity is expensive.
- **Why no auth:** single-user personal tool. A single `X-Device-Key` shared-secret header is enough to stop random internet traffic — full auth is YAGNI. But the schema keeps a `users` row (id=1) so multi-user is a migration, not a rewrite.
- **Why migrations from the start:** health data accumulates and is not re-derivable. Losing it to a `synchronize: true` schema drop would be unrecoverable. Never enable TypeORM/Prisma auto-sync.
- **Why one repo:** solo dev, KISS. Android and backend in one repo, no shared build tooling between them.

## Requirements
Functional: `GET /health` returns 200 · migrations create all MVP tables · Android app launches to an empty Today screen with 3-tab bottom nav · both build in CI.
Non-functional: backend unit tests run without a DB (rules are pure) · secrets from env only · file size <200 lines per `docs/development-rules.md`.

## Architecture
```
healthycoach/
  backend/            NestJS
    src/
      rules/          PURE TS. no @Injectable, no DB, no HTTP. the heart of the product.
      activities/     ingestion + storage of completed runs
      plans/          training plan CRUD + generation orchestration
      checkins/       post-run + pain check-ins
      nutrition/      fueling card orchestration
      db/             prisma schema + migrations
      common/         config, device-key guard, error filter
  android/            Kotlin + Compose
    app/src/main/java/com/healthycoach/
      ui/today  ui/plan  ui/checkin  ui/theme
      data/     retrofit api + dtos + repositories
      health/   HealthConnect (or file import) source
  plans/  docs/
```
Dependency rule (enforce in review): `rules/` imports nothing from the other folders. Everything else may import `rules/`.

**Postgres tables (MVP subset of `docs/system-architecture.md` §3):**
`users` · `athlete_profiles` · `race_goals` · `training_plans` · `training_weeks` · `planned_workouts` · `completed_activities` · `subjective_checkins` · `fueling_plans` · `fueling_logs` · `injury_flags` · `plan_adjustments`.
Deferred: `activity_laps`, `daily_recovery_metrics`, `coach_recommendations`, `nutrition_profiles` (fold into `athlete_profiles` for MVP).

Every `completed_activities` row carries `source` (`health_connect` | `fit_upload` | `manual`) and `external_id` — provenance is a `docs/code-standards.md` §2 requirement and `external_id` gives us idempotent import for free.

## Related Code Files (all new)
Backend:
- `backend/package.json`, `tsconfig.json`, `.env.example`, `nest-cli.json`, `jest.config.ts`
- `backend/src/main.ts`, `src/app.module.ts`
- `backend/src/common/config.ts`, `common/device-key.guard.ts`, `common/http-exception.filter.ts`
- `backend/src/db/schema.prisma`, `db/prisma.service.ts`, `db/seed.ts`
- `backend/src/rules/index.ts` (barrel; implementations land in Phase 04)
- `backend/src/{activities,plans,checkins,nutrition}/*.module.ts` (empty modules)
- `backend/Dockerfile`

Android:
- `android/settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`
- `android/app/build.gradle.kts`, `src/main/AndroidManifest.xml`
- `android/app/src/main/java/com/healthycoach/MainActivity.kt`
- `.../ui/theme/Theme.kt`, `.../ui/AppNavHost.kt`
- `.../ui/today/TodayScreen.kt`, `.../ui/plan/PlanScreen.kt`, `.../ui/checkin/CheckinScreen.kt` (placeholders)
- `.../data/ApiClient.kt`, `.../data/dto/` (empty)

Repo: `.github/workflows/ci.yml`, `.gitignore`, `README.md`

## Implementation Steps
1. `npx @nestjs/cli new backend` (npm, strict TS). Delete the generated sample controller/service.
2. Add Prisma: `npm i -D prisma && npm i @prisma/client && npx prisma init`.
3. Write `schema.prisma` with the 12 tables above. Use `DateTime` in UTC everywhere; store distances in **metres** and durations in **seconds** (canonical units per `docs/code-standards.md` §2 — format only at the UI).
4. `npx prisma migrate dev --name init`. Commit the generated SQL migration.
5. `db/seed.ts`: insert user id 1, an `athlete_profile` (baseline 20–35 km/wk, run days, long-run day), and a `race_goal` (marathon, 2027-01-22, goal=completion).
6. `common/config.ts`: read `DATABASE_URL`, `DEVICE_KEY`, `PORT` via `@nestjs/config` with a validation schema — fail fast at boot on missing vars.
7. `device-key.guard.ts`: compare `X-Device-Key` header to `DEVICE_KEY` with a constant-time compare; register globally; exempt `/health`.
8. Global `ValidationPipe` (whitelist + forbidNonWhitelisted) and an exception filter emitting `{ code, message }` stable error shapes.
9. Add `GET /health` returning `{ status, dbConnected }`.
10. Create empty `rules/`, `activities/`, `plans/`, `checkins/`, `nutrition/` modules; register in `app.module.ts`.
11. Configure Jest: one project for `rules/**` (no setup, fast) and one for the rest.
12. Write `backend/Dockerfile` (node:20-alpine, multi-stage) and deploy a first empty instance to Railway/Render with a managed Postgres — deploying early makes deployment a small ongoing task rather than one big scary one at the end.
13. Android: new Compose project `com.healthycoach`, minSdk 28. Add Retrofit + kotlinx-serialization + Hilt + Navigation Compose to `libs.versions.toml`.
14. `AppNavHost.kt`: bottom nav with Today / Plan / Check-in routes and placeholder screens.
15. `ApiClient.kt`: Retrofit instance, base URL from `BuildConfig`, OkHttp interceptor injecting `X-Device-Key`.
16. `ci.yml`: job 1 = backend `npm ci && npm run build && npm test`; job 2 = `./gradlew :app:assembleDebug`.

## Todo List
- [x] NestJS project created, sample code removed
- [x] Prisma schema w/ 12 MVP tables + init migration committed
- [x] Seed script: user 1, athlete profile, race goal 2027-01-22
- [x] Config validation, device-key guard, validation pipe, error filter
- [x] `GET /health` green
- [x] Empty domain modules + `rules/` purity boundary documented in README
- [x] Jest configured, `npm test` passes on empty suite
- [x] Dockerfile + local docker-compose verified (PaaS deploy deferred for Phase 03)
- [x] Android Compose project + 3-tab nav shell, static review passed
- [x] Retrofit client w/ device-key interceptor
- [x] CI builds both sides, YAML valid

## Success Criteria
`npm test` and `npm run build` pass. `npx prisma migrate deploy` on a clean DB produces the full schema. Deployed `/health` returns `dbConnected: true`. Android debug APK installs and navigates between 3 empty screens. CI green on push.

## Risk Assessment
| Risk | Mitigation |
| --- | --- |
| Kotlin/Compose learning curve (new language for user) | Phase 02 Android work is deliberately trivial (nav shell only); real UI is Phase 05, after backend confidence is built. |
| Schema churn once rules engine is real | Expected. Migrations make it cheap — never edit an applied migration, always add a new one. |
| `rules/` purity erodes under deadline pressure | Add an ESLint `no-restricted-imports` rule banning `@nestjs/*` and `@prisma/client` inside `src/rules/`. Machine-enforced beats discipline. |
| Free PaaS tier sleeps / drops DB | Use a paid $5–10 Postgres tier; free tiers delete data. |

## Security Considerations
`.env` gitignored, `.env.example` committed with dummy values. `DEVICE_KEY` random 32+ bytes, stored in the PaaS secret store and Android `local.properties` (gitignored) — never in source. HTTPS enforced by the PaaS. Health data at rest relies on managed-Postgres encryption; note this as accepted for a single-user personal tool. No health values in logs.

## Next Steps
Phase 03 (ingestion) once Phase 01's gate reports GO/NO-GO. Phase 04 can start against seeded fixtures without waiting for real ingestion.
