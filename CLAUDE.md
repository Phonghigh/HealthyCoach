# HealthyCoach

AI Marathon Coach: turns Garmin/check-in data into an actionable weekly training plan. First target race 22/01/2027. See [docs/index.md](./docs/index.md) for full doc map (Vietnamese).

## Status

**In implementation — Phase 02 scaffolding complete.** Backend (NestJS + TypeScript + PostgreSQL), Android (Kotlin + Jetpack Compose), and Prisma schema (12 tables) now exist. Tech stack is **decided** (see [MVP plan](./plans/260907-2130-mvp-implementation/plan.md)). Product direction and architecture in `docs/` remain proposal; implementation tracks the plan phases.

## Doc map

- [Overview & PDR](./docs/project-overview-pdr.md) — problem, goals, requirements, acceptance criteria
- [Product scope & interfaces](./docs/product-scope-and-interfaces.md) — Garmin/mobile/web role split
- [System architecture](./docs/system-architecture.md) — components, data, decision boundaries
- [Core workflows](./docs/core-workflows.md) — onboarding through next-week adjustment
- [Garmin & Connect IQ](./docs/garmin-integration-and-connect-iq.md) — confirmed capabilities, PoC gaps
- [PoC plan](./docs/proof-of-concept-plan.md) — five experiments on Forerunner 165
- [Roadmap](./docs/project-roadmap.md) — MVP to Healthy Lifestyle Assistant
- [Code standards](./docs/code-standards.md) — implementation principles once code starts
- [Training safety & nutrition](./docs/training-safety-and-nutrition.md) — safety rails, fueling, advice limits
- [Assumptions & open questions](./docs/assumptions-and-open-questions.md) — full list

## Open questions (condensed — see assumptions-and-open-questions.md for details)

| Topic | Question |
| --- | --- |
| Product | Personal tool or commercial product? |
| Target | Is race date/runner baseline fixed? |
| Garmin | Developer Program / Training / Courses API access — not yet obtained |
| Device | Which firmware/models beyond Forerunner 165 (PoC target)? |
| Clinical | Who reviews/approves safety rules and nutrition content? |
| Privacy | Retention, data residency, export/delete, coach sharing? |
| Maps | POI provider and who verifies water/WC/safety data? |
| Technical | Stack decided: NestJS/TypeScript + Kotlin/Compose + PostgreSQL — see [Phase 02 complete](./plans/260907-2130-mvp-implementation/phase-02-project-scaffolding.md) |
| Metrics | Success metrics/telemetry/north star not yet defined |

## Project rules

- New markdown files go only in `plans/` or `docs/` — never elsewhere — unless explicitly requested.
- Follow the global development rules in `~/.claude/rules/` (YAGNI/KISS/DRY, file naming, docs update protocol).
- Don't assume a tech stack, product type, or Garmin API access when planning — these are open decisions (see above). Ask before assuming.
