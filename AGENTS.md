# HealthyCoach

AI Marathon Coach: turns Garmin/check-in data into an actionable weekly training plan. First target race 22/01/2027. See [docs/index.md](./docs/index.md) for full doc map (Vietnamese).

## Status

**Docs-only.** No source code, stack, or schema exists yet. Everything in `docs/` is proposal/direction, not implementation. Tech stack is undecided — do not assume one when discussing this project.

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
| Technical | Stack not yet chosen |
| Metrics | Success metrics/telemetry/north star not yet defined |

## Project rules

- New markdown files go only in `plans/` or `docs/` — never elsewhere — unless explicitly requested.
- Follow the global development rules in `~/.Codex/rules/` (YAGNI/KISS/DRY, file naming, docs update protocol).
- Don't assume a tech stack, product type, or Garmin API access when planning — these are open decisions (see above). Ask before assuming.
