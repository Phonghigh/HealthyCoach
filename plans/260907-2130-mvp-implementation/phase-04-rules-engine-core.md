# Phase 04 — Rules engine core

## Context Links
- [plan.md](./plan.md) · [phase-02](./phase-02-project-scaffolding.md) · [phase-03](./phase-03-data-ingestion.md)
- `docs/project-overview-pdr.md` §4 P0.3–P0.5, §5 (testability NFR)
- `docs/training-safety-and-nutrition.md` §1 engine roles, §5 workload types
- `docs/system-architecture.md` §1, §7

## Overview
- **Priority:** P0 — this is the product. Everything else is I/O around it.
- **Status:** pending
- **Description:** Deterministic TypeScript modules that turn activity history + profile into a marathon roadmap and a concrete weekly plan, subject to hard safety guardrails. No LLM, no randomness, no dates read from `Date.now()` inside the rules.

## Key Insights
- **Why pure functions:** determinism is the entire safety argument. `plan(input) -> output` with no I/O means every rule is a table-driven test, and any produced plan can be reproduced exactly from its stored input snapshot when auditing "why did it tell me to run 30km?".
- **Why clock injection:** `new Date()` inside a rule makes tests time-dependent and flaky. Pass `today: LocalDate` in the input. Same for anything random — there must be nothing random.
- **Why safety is a separate *final* stage, not conditionals sprinkled in generation:** the training engine proposes, the safety engine disposes. A single chokepoint means "can an unsafe plan be emitted?" is answerable by reading one file. Sprinkled checks are unauditable.
- **Why 10%/week is a guideline, not gospel:** the classic "10% rule" is a convention, not a validated law. Encode it as a **configurable constant with a named source comment**, plus an absolute cap on long-run length and a mandatory down-week — the down-week (recovery week every 4th) does more for injury avoidance than precise weekly percentages.
- **Rule versioning:** every generated plan stores `rulesVersion`. When rules change, old plans remain explainable. Cheap now, impossible to retrofit.

## Requirements
Functional:
1. **Baseline analysis** — from last 4–8 weeks of activities: avg weekly distance, run frequency, longest run, avg easy pace, data-completeness flag.
2. **Roadmap** — weeks from today to 2027-01-22 split into Base → Build → Peak → Taper → Race with week counts.
3. **Weekly plan generation** — per week: target mileage, session count, long-run distance, and a day-by-day list of typed workouts (`easy | recovery | long | quality | strength | rest`) placed on the athlete's available days.
4. **Load progression** — weekly volume increase capped; every 4th week is a down week (~-20%); long run capped in absolute km and as a share of weekly volume.
5. **Safety guardrails** (hard, non-overridable): no two consecutive hard days; ≥1 full rest day/week; long run not adjacent to quality; taper strictly monotonic decreasing; absolute weekly and long-run ceilings; never increase load when a red flag is active.
6. **Targets per workout** — pace range, HR zone (if HR available) and RPE; when heat/humidity or HR data is unreliable, output an explicit fallback instruction to run by RPE.
7. Every plan and every constraint application emits a human-readable `reason` string.

Non-functional: `rules/` has zero framework/DB imports · ≥90% branch coverage on rules · all tests table-driven · pure and synchronous.

## Architecture
Pipeline (each stage a pure function):
```
activities + profile + goal + today
   -> analyzeBaseline()      -> Baseline
   -> buildRoadmap()         -> RoadmapWeek[]  (phase per week)
   -> generateWeek()         -> DraftWeek      (volume, sessions, placement)
   -> applyProgression()     -> DraftWeek      (vs previous week's ACTUAL, not planned)
   -> applySafetyRules()     -> SafeWeek + AppliedConstraint[]   <-- final authority
```
`applyProgression` compares against **completed** volume, not planned volume. Otherwise a user who skips two weeks gets progressed off a fictional base — a classic and dangerous bug.

All numeric constants live in one `rules/constants.ts` with a comment per value stating its rationale/source, so a coach or clinician can review them without reading logic.

Types are the contract: `rules/types.ts` defines `Baseline`, `TrainingPhase`, `WorkoutType`, `PlannedWorkout`, `WeekPlan`, `AppliedConstraint`, `RuleInput`, `RuleOutput`.

## Related Code Files (all new)
Pure rules:
- `backend/src/rules/types.ts`
- `backend/src/rules/constants.ts`
- `backend/src/rules/baseline-analysis.ts`
- `backend/src/rules/roadmap-builder.ts`
- `backend/src/rules/week-generator.ts`
- `backend/src/rules/load-progression.ts`
- `backend/src/rules/safety-guardrails.ts`
- `backend/src/rules/workout-targets.ts`
- `backend/src/rules/index.ts` (compose the pipeline)
Tests (one per module, table-driven):
- `backend/src/rules/*.spec.ts`
- `backend/src/rules/__fixtures__/athlete-scenarios.ts` (consistent runner, sporadic runner, injured runner, no-HR runner, zero-history runner)
Orchestration (impure, thin):
- `backend/src/plans/plans.module.ts`, `plans.controller.ts`, `plans.service.ts`
- `backend/src/plans/plan-persistence.ts` (DraftWeek → `training_weeks` + `planned_workouts` rows)

## Implementation Steps
1. Write `types.ts` first. Design the data shapes before any logic — the types are the spec.
2. `constants.ts`: `MAX_WEEKLY_INCREASE_PCT = 0.10`, `DOWN_WEEK_EVERY = 4`, `DOWN_WEEK_FACTOR = 0.8`, `MAX_LONG_RUN_SHARE_OF_WEEK = 0.35`, `ABSOLUTE_MAX_LONG_RUN_KM = 32`, `MIN_REST_DAYS_PER_WEEK = 1`, `TAPER_WEEKS = 3`, `EASY_SHARE_MIN = 0.8`. One comment per constant explaining why. `ABSOLUTE_MAX_LONG_RUN_KM = 32` because for a completion goal, running the full 42 km in training adds injury risk without proportional benefit.
3. `baseline-analysis.ts`: bucket activities into local-tz ISO weeks, compute avg weekly km / frequency / longest run / avg easy pace. Return `dataQuality: 'sufficient' | 'sparse' | 'none'` — with `none`, everything downstream must start conservatively at the low end of the declared baseline, not guess.
4. `baseline-analysis.spec.ts`: table cases incl. zero activities, one activity, a week-boundary crossing, an outlier ultra-long run (must not skew the baseline — use median for typical week).
5. `roadmap-builder.ts`: weeks between `today` and race date → last 3 = Taper, race week = Race, first ~40% = Base, next ~40% = Build, then Peak. Handle short runway (<12 weeks) by compressing Base, never by compressing Taper — taper is safety-critical.
6. `roadmap-builder.spec.ts`: 60-week runway, 12-week, 4-week, race-in-the-past (must return an error result, not throw).
7. `week-generator.ts`: given phase + target volume + available days → place long run on the athlete's long-run day, distribute remaining volume across easy runs, insert quality only in Build/Peak, one rest day minimum. Deterministic placement order (no shuffling).
8. `load-progression.ts`: `nextWeekVolume(prevActualKm, phase, weekIndex)` → apply increase cap, down-week rule, taper reductions. If prev actual < 60% of prev planned, do **not** progress — hold or reduce.
9. `load-progression.spec.ts`: consistent build · missed week · down week lands on week 4/8/12 · taper monotonic decrease · progression never exceeds cap.
10. `safety-guardrails.ts`: `applySafetyRules(week, context) -> { week, constraints: AppliedConstraint[] }`. Each constraint = `{ rule, reason, before, after }`. Reorder/downgrade sessions to satisfy: no back-to-back hard, rest day present, long run not adjacent to quality, caps respected, no increase when `injury_flags` active.
11. `safety-guardrails.spec.ts`: **the most important test file in the repo.** Adversarial cases — hand-craft dangerous weeks and assert they come out safe. Add a property-style test: for N generated weeks across random-but-seeded inputs, assert the invariants always hold.
12. `workout-targets.ts`: from baseline pace + workout type → pace range, HR zone, RPE. If HR data absent → omit zone and set `useRpeFallback: true` with reason text.
13. `plans.service.ts`: load profile/goal/activities → call `rules/index.ts` → persist → return. Thin. No logic here beyond fetch/save.
14. Endpoints: `POST /plans/generate` (regenerate from today), `GET /plans/current`, `GET /plans/week?date=`.
15. Persist `rulesVersion`, the input snapshot, and `AppliedConstraint[]` alongside the week — required by `docs/system-architecture.md` §3.

## Todo List
- [ ] `types.ts` contract defined
- [ ] `constants.ts` w/ per-constant rationale comments
- [ ] `baseline-analysis.ts` + tests (incl. sparse/none data)
- [ ] `roadmap-builder.ts` + tests (incl. short runway, past race date)
- [ ] `week-generator.ts` + tests
- [ ] `load-progression.ts` + tests (incl. missed-week hold)
- [ ] `safety-guardrails.ts` + adversarial + invariant tests
- [ ] `workout-targets.ts` + RPE-fallback tests
- [ ] Pipeline composed in `rules/index.ts`
- [ ] Athlete scenario fixtures (5 personas)
- [ ] `plans.service` persistence incl. rulesVersion + input snapshot + constraints
- [ ] `POST /plans/generate`, `GET /plans/current`, `GET /plans/week`
- [ ] ESLint import-boundary rule verified failing on a deliberate violation
- [ ] Coverage ≥90% branches in `rules/`

## Success Criteria
Generating a plan from the seeded 25 km/week baseline produces a sane ~60-week roadmap to 2027-01-22 with a peak long run ≤32 km and a 3-week taper. Running generation twice with identical input produces byte-identical output. No test in `rules/` touches a DB, network, or the system clock. The invariant test proves no emitted week contains consecutive hard days.

## Risk Assessment
| Risk | Mitigation |
| --- | --- |
| Rule constants are guesses, not clinically reviewed | Isolate in `constants.ts` with sources; add a README note that values need coach/clinician review before any non-personal use (`docs/training-safety-and-nutrition.md` §5 says exactly this). |
| Over-engineering into a full periodization framework | YAGNI: one goal (completion), one distance, one athlete. Do not build a generic engine. |
| Progression against planned instead of actual volume | Step 8 + an explicit test for it. |
| Long-run outlier skews baseline | Use median weekly volume; test case in step 4. |
| Timezone week-bucketing bugs | Reuse the Phase 03 tz helper; shared week-boundary test. |

## Security Considerations
No new external surface. The safety-relevant control is **integrity**: the safety stage must be non-bypassable. Enforce by making `applySafetyRules` the only export path used by `plans.service` — do not export the unsafe `DraftWeek` builders from `rules/index.ts`. A generated plan that skipped safety must be structurally impossible, not merely discouraged.

## Next Steps
Phase 05 renders these plans. Phase 06 re-runs this pipeline after check-ins. Phase 08 feeds `injury_flags` into the safety context defined here — design that input field now even though it stays empty until Phase 08.
