# Phase 07 — Fueling / nutrition card

## Context Links
- [plan.md](./plan.md) · [phase-05](./phase-05-mobile-today-and-weekly-plan-ui.md) · [phase-06](./phase-06-checkin-and-plan-adjustment.md)
- `docs/training-safety-and-nutrition.md` §3 fueling design, §4 hydration
- `docs/project-overview-pdr.md` §4 P0.8, §6 ("không khẳng định 'đã dùng' nếu chưa xác nhận")
- `docs/core-workflows.md` §6

## Overview
- **Priority:** P1 within MVP — required by acceptance criteria, but only meaningful once long runs exceed 90 min.
- **Status:** pending
- **Description:** Deterministic carbohydrate/hydration planning for long runs, plus post-run intake confirmation feeding conservative adjustments to future fueling plans.

## Key Insights
- **Why this is arithmetic, not AI:** the core is `carbTargetPerHour ÷ carbPerGel = gelsPerHour`, straight from `docs/training-safety-and-nutrition.md` §3. It must use the **actual label value** of the user's product, not an assumed 25 g. This is the clearest case in the product where a deterministic engine beats a language model.
- **Start conservative, progress tolerance:** 30–60 g/h is the cited endurance starting range; higher intakes require trained gut tolerance. Start the user at the low end and only raise it after confirmed, GI-symptom-free long runs. Never jump to 90 g/h because a study says elites do it.
- **Never claim consumption we did not observe:** the app plans gels; it does not know they were eaten. Store `planned` and `actual` separately and label anything unconfirmed as `reminder_shown` only — an explicit doc requirement and the honest-data principle in miniature.
- **Never introduce something new on race day:** encode as a hard rule, not advice text. The race-week fueling plan may only contain products with prior confirmed tolerance.
- **Hydration is not prescribable:** individual sweat rates vary hugely and over-drinking is genuinely dangerous (hyponatremia). Give a range plus context guidance, never a single number presented as a requirement.

## Requirements
Functional:
1. **Nutrition profile** (part of `athlete_profiles`): list of products `{ name, carbGrams, caffeineMg?, sodiumMg?, tolerated: boolean }`, preferred carb target g/h, known GI sensitivities.
2. **Fueling plan generation** for any workout with estimated duration ≥90 min (60–90 min = optional/contextual, <60 min = none, per the doc's table).
3. Plan contents: pre-run meal timing guidance, target carb g/h, total carbs, gel count + timing schedule (e.g. at 40, 75, 110 min), fluid range per hour with a heat note, and electrolyte guidance.
4. Duration estimate = planned distance ÷ baseline easy pace. Show it as an estimate, not a promise.
5. Display: fueling summary on the Today card for long-run days; full card on workout detail.
6. **Post-run confirmation** (check-in step 3): gels actually taken, fluid estimate, GI symptoms 0–3. Unconfirmed = `reminder_shown`, never `consumed`.
7. **Tolerance progression:** after N (=3) consecutive long runs at the current target with GI ≤1, propose +10 g/h, capped. Any GI ≥2 holds or reduces the target. Requires a trend, never a single session (`docs/core-workflows.md` §6).
8. Race-week plan restricted to products with `tolerated: true`.
9. Optional sweat-rate logger: `(weightBefore − weightAfter + fluidIntake) ÷ hours`, stored as informational only.

Non-functional: all math pure and unit-tested incl. unit conversions · missing product data yields a clear "add your gel product" prompt, never a guessed default.

## Architecture
```
workout + nutritionProfile + fuelingHistory + weather?
   -> rules/fueling-calculator.ts   (PURE)  -> FuelingPlan
   -> rules/tolerance-progression.ts (PURE) -> carb target for next long run
post-run -> fueling_logs -> feeds tolerance-progression on the next generation
```
Fueling generation is triggered as part of Phase 04's week generation and stored in `fueling_plans` keyed to a `planned_workout`, so the Today endpoint returns it in the same round-trip rather than a second call.

Weather input is optional in MVP — if no weather source is wired, output the heat guidance as a conditional note ("if hot/humid, increase fluid toward the top of the range") rather than integrating a weather API. YAGNI: a weather API is a whole dependency for one sentence.

## Related Code Files (all new)
Pure rules:
- `backend/src/rules/fueling-calculator.ts`
- `backend/src/rules/tolerance-progression.ts`
- `backend/src/rules/nutrition-constants.ts` — carb ranges, gel defaults, fluid ranges, each with a source comment
- `backend/src/rules/fueling-calculator.spec.ts`, `tolerance-progression.spec.ts`
Backend:
- `backend/src/nutrition/nutrition.module.ts`, `nutrition.controller.ts`, `nutrition.service.ts`
- `backend/src/nutrition/dto/nutrition-profile.dto.ts`, `dto/fueling-log.dto.ts`
- Prisma additions: `fueling_plans`, `fueling_logs`, products JSON on `athlete_profiles`
Android:
- `.../ui/nutrition/FuelingCard.kt` (compact, for Today)
- `.../ui/nutrition/FuelingDetailScreen.kt` (timeline of gels + fluids)
- `.../ui/nutrition/ProductsScreen.kt` (CRUD the user's gel products)
- `.../ui/checkin/StepFueling.kt` (check-in step 3)
- `.../ui/nutrition/SweatRateDialog.kt` (optional)
- `.../data/dto/FuelingDto.kt`, `.../data/NutritionRepository.kt`

## Implementation Steps
1. `nutrition-constants.ts`: `CARB_START_G_PER_HOUR = 30`, `CARB_MAX_G_PER_HOUR = 60` (MVP cap — higher needs trained tolerance), `FUELING_THRESHOLD_MIN = 90`, `OPTIONAL_FUELING_MIN = 60`, `FLUID_ML_PER_HOUR_RANGE = [400, 800]`, `TOLERANCE_STREAK_REQUIRED = 3`, `CARB_STEP_G = 10`. Comment each with its rationale and doc reference.
2. `fueling-calculator.ts`: `plan(durationMin, carbTargetPerHour, products) -> FuelingPlan | NoFuelingNeeded | MissingProductData`. Compute total carbs, gels needed using each product's real label value, and evenly spaced timing offsets starting ~35–45 min in.
3. `fueling-calculator.spec.ts`: 45 min (none) · 75 min (optional, flagged not required) · 120 min · 180 min · a 40 g product vs a 22 g product producing different counts · zero products (returns `MissingProductData`, does not guess) · fractional gels rounded sensibly and never rounded up past the carb cap.
4. `tolerance-progression.ts`: `nextTarget(history, current) -> { target, reason }`. +`CARB_STEP_G` only after `TOLERANCE_STREAK_REQUIRED` clean sessions; hold on GI 2; reduce on GI 3; never exceed the cap; never progress in race week.
5. `tolerance-progression.spec.ts`: streak of 3 clean → increase · 2 clean then GI → hold · GI 3 → reduce · at cap → no change · race week → frozen.
6. Prisma migration: `fueling_plans` (workoutId, carbTargetPerHour, totalCarbG, items JSON, fluidRangeMl, rulesVersion), `fueling_logs` (workoutId, gelsActual, fluidMl?, giScore, confirmed boolean), products JSON on `athlete_profiles`.
7. `nutrition.service.ts`: generate on plan generation; `GET /nutrition/fueling/:workoutId`; `POST /nutrition/logs`; `GET/PUT /nutrition/profile`.
8. Include the fueling summary in the existing `GET /today` DTO (slot reserved in Phase 05).
9. Android `ProductsScreen`: add/edit gel products with name + carb grams (required) — required because the whole calculation depends on it; block fueling plan display with a prompt if the list is empty.
10. `FuelingCard`: target g/h, gel count, fluid range — three lines max on Today.
11. `FuelingDetailScreen`: timeline (minute → action), pre-run meal note, heat note, and a "not tested yet" warning badge on any untolerated product.
12. `StepFueling.kt`: only shown for long runs — gels taken (stepper), fluid (rough slider), GI (0–3 chips). All optional; skipping records `reminder_shown` not `consumed`.
13. Race-week filter: when generating for a race-phase workout, exclude products with `tolerated: false` and add an explicit "nothing new on race day" line.
14. Optional: `SweatRateDialog` computing and storing the formula result as informational.
15. Manual check: a 2h long run yields a plan matching a hand calculation; post-run confirmation appears in history and influences the next long run's target only after the streak.

## Todo List
- [ ] `nutrition-constants.ts` w/ sourced rationale comments
- [ ] Pure `fueling-calculator.ts` + full boundary tests (incl. no-products case)
- [ ] Pure `tolerance-progression.ts` + streak/GI/cap/race-week tests
- [ ] Migration: `fueling_plans`, `fueling_logs`, products on profile
- [ ] Fueling generated during week generation, returned in `GET /today`
- [ ] Nutrition profile + logs endpoints
- [ ] Products management screen (blocks planning when empty)
- [ ] Compact fueling card on Today
- [ ] Fueling detail timeline + untested-product warnings
- [ ] Check-in fueling step (long runs only, all optional)
- [ ] Race-week "nothing new" restriction implemented + tested
- [ ] Optional sweat-rate logger
- [ ] Hand-calculation cross-check on a real long run

## Success Criteria
A 2-hour long run with a 25 g gel and a 30 g/h target yields 60 g total → 2–3 gels with concrete timings, matching a manual calculation. Changing the product to 40 g changes the gel count. With no products configured, the app asks for one instead of assuming. Skipping the fueling check-in never records gels as consumed. Carb target only rises after 3 clean long runs and never above the cap.

## Risk Assessment
| Risk | Mitigation |
| --- | --- |
| Nutrition guidance presented as medical advice | Persistent disclaimer; ranges not prescriptions; `docs/training-safety-and-nutrition.md` disclaimer surfaced in-app. |
| Over-hydration advice causing real harm | Output a range with an explicit "do not force fluids beyond thirst in cool conditions" note; never a single mandated volume. |
| Assuming default gel carb content | `MissingProductData` result type makes guessing structurally impossible. |
| Single-session over-correction | `TOLERANCE_STREAK_REQUIRED = 3`; tested. |
| Constants not clinician-reviewed | Same as Phase 04 — flag in README as personal-use-only pending dietitian review. |

## Security Considerations
Nutrition and GI data are health data — same handling (HTTPS, device key, no logging, included in export/delete). Caffeine content is stored but the MVP must not compute caffeine dosing recommendations; display the label value only. No claim of consumption without confirmation is as much an integrity control as a UX rule.

## Next Steps
Phase 08 is the last MVP phase. Post-MVP: weather API integration for the heat adjustment, food photo logging, race-day fueling planner.
