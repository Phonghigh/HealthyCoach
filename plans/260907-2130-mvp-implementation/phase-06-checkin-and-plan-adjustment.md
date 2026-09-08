# Phase 06 — Post-run check-in + plan adjustment

## Context Links
- [plan.md](./plan.md) · [phase-04](./phase-04-rules-engine-core.md) · [phase-05](./phase-05-mobile-today-and-weekly-plan-ui.md)
- `docs/project-overview-pdr.md` §4 P0.6–P0.7, §6 (missed session must not create two adjacent hard days)
- `docs/core-workflows.md` §3, §4 · `docs/training-safety-and-nutrition.md` §2 signal table
- `docs/system-architecture.md` §3 (audit requirements)

## Overview
- **Priority:** P0 — closes the product loop. Without this, it is a static plan generator.
- **Status:** pending
- **Description:** <30-second post-run check-in feeding a deterministic adjustment engine that changes the upcoming plan with a stated reason and a full audit trail. User must approve before anything changes.

## Key Insights
- **Why <30s:** a check-in nobody completes produces no adjustments, which makes the whole loop dead. Every extra question is a tax on adherence. Ask only what cannot be derived from the activity data (`docs/product-scope-and-interfaces.md` §5) — RPE and pain are subjective, distance and pace are not.
- **Why conditional questions:** fueling questions only for long runs, deep pain questions only if pain was reported. This is what keeps the median check-in at ~15s.
- **Why "propose then apply" instead of auto-apply:** silent plan changes destroy trust, and the docs mandate an explicit `Áp dụng` step. The user must see `data → conclusion → action` and consent.
- **Why never compensate:** the single most dangerous instinct after a missed run is to make it up. The engine must structurally refuse: no adding missed volume onto later sessions, no upgrading intensity, no two hard days adjacent. This is a P0 acceptance criterion.
- **Why versioned adjustments:** `plan_adjustments` stores input snapshot, rule version, reason, before/after and actor. Without it, "why did my Thursday change?" is unanswerable — and transparency is the product's differentiator.

## Requirements
Functional:
1. Check-in entry points: Today card after an activity is detected, and workout detail. Also prompt for a **missed** planned workout (no activity by end of day).
2. Check-in fields — step 1: completion status (`completed | partial | missed`), RPE 1–10, overall feeling. Step 2 (conditional): pain yes/no → hands off to Phase 08. Step 3 (conditional, long run only): fueling actuals + GI issues → Phase 07.
3. Missed-workout flow: reason (`time | fatigue | illness | pain | weather | other`) + options (reschedule within the week / drop it).
4. `POST /checkins` persists the check-in and returns a **proposal**: conclusion text + list of proposed changes + reasons.
5. `POST /adjustments/:id/apply` applies the proposal, writes a new plan version, and records the audit row. Rejection is also recorded.
6. Adjustment rules (deterministic):
   - High RPE (≥8) on an easy run → next session downgraded to recovery; reason stated.
   - Missed due to fatigue/illness → reduce the following session; never compensate.
   - Missed due to time → allow reschedule only if it does not violate safety guardrails.
   - Two+ misses in a week → hold weekly volume next week (no progression).
   - Any active pain flag → drop quality work; defer to Phase 08 safety engine.
   - Consistent completion + normal RPE → normal progression continues.
7. History view: list of applied adjustments with reasons.

Non-functional: check-in POST responds <1s · proposal generation is pure/deterministic · every applied change is reproducible from its stored snapshot.

## Architecture
```
CheckinDto -> checkins.service (persist)
           -> rules/adjustment-engine.ts  (PURE)  -> AdjustmentProposal
           -> rules/safety-guardrails.ts  (PURE)  -> validated proposal   <-- reused from Phase 04
           -> returned to app for approval
apply      -> plan-versioning.ts -> new planned_workouts + plan_adjustments audit row
```
The adjustment engine reuses Phase 04's `safety-guardrails.ts` rather than re-implementing checks. Any proposal that would violate a guardrail is discarded before the user ever sees it — the user cannot approve an unsafe change, because unsafe changes are never offered.

Plan versioning: rather than mutating `planned_workouts`, mark superseded rows and insert new ones with an incremented `version`. Append-only history is what makes the audit trail truthful.

## Related Code Files (all new)
Pure rules:
- `backend/src/rules/adjustment-engine.ts` — `propose(checkin, week, history) -> AdjustmentProposal`
- `backend/src/rules/adjustment-rules-table.ts` — the signal→action table from `docs/training-safety-and-nutrition.md` §2, as data not code
- `backend/src/rules/adjustment-engine.spec.ts`, `adjustment-rules-table.spec.ts`
- `backend/src/rules/types.ts` (extend: `Checkin`, `AdjustmentProposal`, `ProposedChange`)
Backend:
- `backend/src/checkins/checkins.module.ts`, `checkins.controller.ts`, `checkins.service.ts`
- `backend/src/checkins/dto/create-checkin.dto.ts`, `dto/adjustment-proposal.dto.ts`
- `backend/src/plans/plan-versioning.ts`, `plan-versioning.spec.ts`
- `backend/src/checkins/missed-workout.detector.ts` (nightly job or on-request check)
Android:
- `.../ui/checkin/CheckinFlowScreen.kt`, `CheckinViewModel.kt`
- `.../ui/checkin/StepEffort.kt`, `StepPainGate.kt`, `StepMissedReason.kt`
- `.../ui/checkin/ProposalReviewScreen.kt` — data → conclusion → action, with Apply / Dismiss
- `.../ui/history/AdjustmentHistoryScreen.kt`
- `.../data/dto/CheckinDto.kt`, `AdjustmentDto.kt`, `.../data/CheckinRepository.kt`
- `.../ui/checkin/CheckinViewModelTest.kt`

## Implementation Steps
1. Extend `rules/types.ts` with `Checkin`, `ProposedChange { workoutId, field, before, after, reason }`, `AdjustmentProposal { conclusion, changes[], rulesVersion }`.
2. Write `adjustment-rules-table.ts` as a declarative array of `{ id, when(signals): boolean, action, reasonTemplate, priority }`. Declarative because a coach should be able to review the rule set without reading control flow, and because adding a rule then becomes a data change with a test, not a refactor.
3. `adjustment-engine.ts`: evaluate the table against the check-in + recent history, take the highest-priority matching action per affected workout, build the proposal. Pure — no DB.
4. Run every proposal through Phase 04's `applySafetyRules`; discard violating changes and log which rule blocked them.
5. `adjustment-engine.spec.ts`: table-driven — one case per rule, plus **the P0 acceptance case**: a missed hard session must never result in two adjacent hard days, and total volume must not be redistributed upward.
6. Prisma: add `version` and `supersededAt` to `planned_workouts`; create `plan_adjustments` (checkinId, inputSnapshot JSON, rulesVersion, proposal JSON, status `proposed|applied|rejected`, actor, timestamps). Migration.
7. `plan-versioning.ts`: apply a proposal transactionally — supersede old rows, insert new versions, write the audit row. Test that a failed apply leaves zero partial state.
8. `checkins.service.ts` + controller: `POST /checkins` → persist + return proposal; `POST /adjustments/:id/apply`; `POST /adjustments/:id/reject`; `GET /adjustments?limit=`.
9. `missed-workout.detector.ts`: on `GET /today`, if a planned workout from a past day has no matching completed activity and no check-in, surface a "missed?" prompt. Simplest possible implementation — no scheduler needed for MVP.
10. Android `CheckinFlowScreen`: 3 steps max, large tap targets, RPE as a slider with word labels ("easy" … "max"), no free-text required. Steps 2 and 3 conditional.
11. `ProposalReviewScreen`: render conclusion → the change list with before/after → Apply / Dismiss. Never auto-apply.
12. `AdjustmentHistoryScreen`: reverse-chronological list of applied adjustments with reason text.
13. Wire the check-in prompt into Today (post-activity, and for the missed case).
14. Manual E2E: run → sync → check-in with RPE 9 → see proposal downgrading the next session → apply → Today reflects it → history shows it.

## Todo List
- [ ] Types extended (`Checkin`, `ProposedChange`, `AdjustmentProposal`)
- [ ] Declarative `adjustment-rules-table.ts`
- [ ] Pure `adjustment-engine.ts` + per-rule tests
- [ ] Proposals validated through Phase 04 safety guardrails
- [ ] **Test: missed hard session never yields two adjacent hard days**
- [ ] **Test: missed volume never redistributed upward**
- [ ] Migration: workout versioning + `plan_adjustments`
- [ ] Transactional `plan-versioning.ts` + rollback test
- [ ] Check-in / apply / reject / history endpoints
- [ ] Missed-workout detection on `GET /today`
- [ ] 3-step conditional check-in UI (<30s)
- [ ] Proposal review screen w/ explicit Apply
- [ ] Adjustment history screen
- [ ] Manual E2E loop verified

## Success Criteria
A real check-in completes in under 30 seconds (time it). RPE 9 on an easy run produces a stated conclusion and a concrete downgrade to the next session. A missed long run produces a proposal that adds no volume elsewhere and creates no adjacent hard days. Every applied adjustment is reconstructible from `plan_adjustments.inputSnapshot` + `rulesVersion`. Nothing changes without an explicit tap.

## Risk Assessment
| Risk | Mitigation |
| --- | --- |
| Check-in too long → not used | Hard cap of 3 steps; conditional branches; time the real flow, cut questions until under 30s. |
| Rule conflicts producing contradictory changes | `priority` field; highest priority wins per workout; test conflicting-signal cases explicitly. |
| Adjustment cascade rewrites the whole roadmap | Scope adjustments to the current + next week only. Roadmap regeneration is a separate explicit action. |
| Audit rows drift from actual plan state | Write both in one transaction (step 7). |
| Users over-report to game the plan | Out of scope — single-user honest-actor tool. Note only. |

## Security Considerations
Check-ins contain subjective health data (pain, illness) — same handling as activities: no logs, HTTPS, device-key guarded. `inputSnapshot` JSON stores health data; it must be included in any future export/delete workflow (`docs/system-architecture.md` §6). Reject endpoints must be idempotent so a double-tap cannot apply a proposal twice — enforce via a status check inside the transaction.

## Next Steps
Phase 07 attaches to check-in step 3. Phase 08 attaches to step 2 and can veto proposals produced here.
