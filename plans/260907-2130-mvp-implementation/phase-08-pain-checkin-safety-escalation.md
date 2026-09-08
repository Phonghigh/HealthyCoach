# Phase 08 — Pain check-in + safety escalation

## Context Links
- [plan.md](./plan.md) · [phase-04](./phase-04-rules-engine-core.md) · [phase-06](./phase-06-checkin-and-plan-adjustment.md)
- `docs/training-safety-and-nutrition.md` §2 signal table (authoritative for this phase)
- `docs/core-workflows.md` §5 pain escalation
- `docs/project-overview-pdr.md` §4 P0.9, §6 (red flag → stop/reduce + professional referral, **no diagnosis**)

## Overview
- **Priority:** P0 — the highest-consequence phase. Getting this wrong can hurt the user.
- **Status:** pending
- **Description:** Body-map pain capture, deterministic risk classification, and escalation actions that override all other planning logic. The app never names a condition, never diagnoses, never suggests treatment.

## Key Insights
- **The single hard boundary: classify risk, never name a condition.** "Localized shin pain worsening during runs" → risk level + action. Not "this could be a stress fracture". Naming conditions is medical diagnosis; it is out of scope, out of competence, and explicitly forbidden by the docs. Enforce it in code by making the output type an enum of risk levels and pre-written action texts — there is no free-text field for a condition, so one cannot be emitted.
- **Why safety overrides everything:** the safety engine is the last stage in every pipeline (Phase 04 generation, Phase 06 adjustment). An active red flag must be able to veto any proposed increase regardless of how good the other signals look. Priority ordering is a correctness property, not a preference.
- **Why the emergency tier is separate:** chest pain, fainting, unusual breathlessness are not training signals — they are "stop and seek urgent medical help now". They must bypass all training logic and show an unmissable, non-dismissible message. Keep this branch trivially simple and impossible to accidentally reorder below other rules.
- **Clinician override:** if the user records that a professional gave them instructions, the app must not contradict or override them (`docs/core-workflows.md` §5). A `clinicianGuidanceActive` flag suppresses the app's own escalation actions in favour of "follow your clinician's advice".
- **Conservative under uncertainty:** with ambiguous or partial input, always classify upward, not downward. A false "take it easy" costs one session; a false "you're fine" can cost the race.

## Requirements
Functional:
1. Pain entry from check-in step 2 and from a persistent "report pain" entry point (pain does not only occur after runs).
2. Capture: location (body map), intensity 0–10, timing (`before | during | after | constant`), trend (`improving | stable | worsening`), swelling y/n, changes running gait y/n, weight-bearing possible y/n, duration in days.
3. Emergency screen: chest pain / fainting or dizziness / unusual shortness of breath — asked as an explicit separate question.
4. Risk classification into 4 levels, mapping directly to `docs/training-safety-and-nutrition.md` §2:
   - `low` — mild, non-progressive, no gait change → monitor, keep easy, reduce intensity.
   - `elevated` — localized pain increasing during runs → drop quality work, cross-train, re-check in 2–3 days.
   - `high` — gait-altering pain, swelling, sharp pain, or cannot bear weight → do not run; recommend professional assessment.
   - `emergency` — chest pain, fainting, abnormal breathlessness → stop activity, seek urgent medical help.
5. Each level produces: plain-language action, plan effect, and a re-check schedule. Never a condition name, never a treatment, never a medication.
6. `injury_flags` row created/updated; active flags feed the Phase 04 safety context and block progression.
7. Re-check prompt appears on Today while a flag is active; user can resolve it, and resolution requires an explicit confirmation that pain has settled.
8. Clinician-guidance flag: when set, the app shows "following your clinician's guidance" and stops proposing its own escalation changes.
9. Pain history view: location, intensity trend over time — presented as recorded data the user can show a professional, explicitly not as an assessment.

Non-functional: classification pure and exhaustively table-tested against the doc's signal table · disclaimer visible on every pain screen · no free-text condition output anywhere in the codebase.

## Architecture
```
PainReport -> rules/pain-risk-classifier.ts (PURE) -> RiskLevel + ActionSet
           -> injury_flags (persist active flag)
           -> rules/safety-guardrails.ts reads active flags as a veto input
```
`ActionSet` is drawn from a fixed catalogue in `pain-action-catalogue.ts`: a constant map of `RiskLevel -> { headline, actions[], planEffect, recheckDays }`. All copy is pre-written and reviewable in one file. This is the enforcement mechanism for "no diagnosis" — the classifier selects from a catalogue, it does not compose text.

Emergency handling is checked **first**, before any other rule, and returns immediately.

## Related Code Files (all new)
Pure rules:
- `backend/src/rules/pain-risk-classifier.ts`
- `backend/src/rules/pain-action-catalogue.ts` — all user-facing safety copy, one file, reviewable
- `backend/src/rules/pain-risk-classifier.spec.ts` — exhaustive table tests
- `backend/src/rules/safety-guardrails.ts` — extend with the injury-flag veto
Backend:
- `backend/src/safety/safety.module.ts`, `safety.controller.ts`, `safety.service.ts`
- `backend/src/safety/dto/pain-report.dto.ts`, `dto/risk-assessment.dto.ts`
- Prisma: `injury_flags` (location, level, active, clinicianGuidanceActive, recheckDueAt, history JSON)
Android:
- `.../ui/pain/BodyMapScreen.kt` — front/back silhouette w/ tappable regions
- `.../ui/pain/PainDetailScreen.kt` — intensity, timing, trend, swelling, gait, weight-bearing
- `.../ui/pain/EmergencyScreen.kt` — full-screen, non-dismissible, urgent-help guidance
- `.../ui/pain/RiskResultScreen.kt` — level, actions, plan effect, re-check date, disclaimer
- `.../ui/pain/PainHistoryScreen.kt`
- `.../ui/pain/PainViewModel.kt`, `.../data/PainRepository.kt`, `.../data/dto/PainDto.kt`
- `.../ui/common/MedicalDisclaimer.kt` — reused on every safety screen

## Implementation Steps
1. `pain-action-catalogue.ts`: write all four action sets as constants, in plain language, with no condition names and no treatment instructions. Review this file line by line against `docs/training-safety-and-nutrition.md` §2 before writing any logic — the copy is the product here.
2. `pain-risk-classifier.ts`: `classify(report) -> { level, actions, reasons[] }`.
   - Step 1: emergency symptoms → return `emergency` immediately.
   - Step 2: cannot bear weight OR swelling OR gait change OR sharp pain → `high`.
   - Step 3: localized AND (worsening trend OR pain during runs at intensity ≥4) → `elevated`.
   - Step 4: otherwise → `low`.
   - Unknown/missing answers count toward the **higher** level, never the lower.
3. `pain-risk-classifier.spec.ts`: one case per row of the doc's signal table, plus all-missing-fields (→ elevated minimum), plus a combinatorial sweep asserting the invariant "adding a concerning symptom never lowers the risk level" (monotonicity). Monotonicity is the property that makes the classifier trustworthy.
4. Prisma migration for `injury_flags` incl. `clinicianGuidanceActive` and `recheckDueAt`.
5. Extend `safety-guardrails.ts`: active `high` flag → no running sessions scheduled, quality removed; `elevated` → quality removed, volume held; `low` → no volume increase this week. Add tests asserting a plan generated with an active high flag contains no run sessions.
6. Wire the veto so it applies to **both** Phase 04 generation and Phase 06 adjustment proposals. Add a test that an otherwise-valid progression proposal is blocked while a flag is active.
7. `safety.service.ts` + controller: `POST /safety/pain` (returns assessment), `GET /safety/flags`, `POST /safety/flags/:id/recheck`, `POST /safety/flags/:id/resolve`, `PUT /safety/flags/:id/clinician-guidance`.
8. Android `BodyMapScreen`: simple front/back silhouette drawable with tappable region overlays (knee, shin, calf, ankle, foot, hip, hamstring, quad, back, other). Keep regions coarse — precision here implies a diagnostic capability the app does not have.
9. `PainDetailScreen`: the six structured questions, all single-tap.
10. `EmergencyScreen`: triggered by the emergency question; full screen, high contrast, cannot be dismissed into the normal flow without an explicit acknowledgement; states stop activity and seek urgent medical assistance. No training content on this screen at all.
11. `RiskResultScreen`: risk level, action list, plan effect, re-check date, `MedicalDisclaimer`. No condition names anywhere.
12. Today integration: active flag renders a prominent banner replacing the normal workout card when level is `high`; re-check prompt when `recheckDueAt` has passed.
13. `PainHistoryScreen`: chronological list + intensity trend, framed as "a record to share with a professional".
14. Add `MedicalDisclaimer` to every pain screen and to the app's Profile/About.
15. Manual walkthrough of all four levels end-to-end, verifying the plan actually changes for each.
16. Final review pass: grep the entire codebase and copy catalogue for condition names (`fracture`, `tendinitis`, `strain`, `syndrome`, etc.) — must return zero results in user-facing strings. Add this as a CI check.

## Todo List
- [ ] `pain-action-catalogue.ts` written + reviewed against docs §2
- [ ] Pure `pain-risk-classifier.ts` w/ emergency-first ordering
- [ ] Exhaustive table tests + monotonicity invariant test
- [ ] Migration: `injury_flags` w/ clinician + re-check fields
- [ ] Safety guardrails extended w/ injury-flag veto + tests
- [ ] Veto applied to both generation and adjustment paths
- [ ] Safety endpoints (report / flags / recheck / resolve / clinician)
- [ ] Body map screen w/ coarse regions
- [ ] Pain detail 6-question flow
- [ ] Non-dismissible emergency screen
- [ ] Risk result screen w/ catalogue copy + disclaimer
- [ ] Today banner + re-check prompt for active flags
- [ ] Pain history view
- [ ] `MedicalDisclaimer` on every safety surface
- [ ] All four levels manually walked end-to-end
- [ ] CI check: zero condition names in user-facing strings

## Success Criteria
Every row of `docs/training-safety-and-nutrition.md` §2 has a passing test producing the documented action. Reporting gait-altering pain with swelling yields `high`, removes all running from the upcoming plan, and recommends professional assessment without naming any condition. Emergency symptoms produce the emergency screen before any training logic runs. No progression is possible while any flag is active. Zero condition names or treatment suggestions in any user-facing string.

## Risk Assessment
| Risk | Mitigation |
| --- | --- |
| Classifier under-calls a serious symptom | Conservative-under-uncertainty rule (step 2) + monotonicity test; missing data escalates. |
| Copy drifts into diagnosis over time | All copy in one catalogue file + CI condition-name check (step 16). |
| Safety veto bypassed by a later code path | Single chokepoint in `safety-guardrails.ts`; test asserting generation and adjustment both respect flags. |
| User ignores a high flag and runs anyway | App cannot prevent it; log it, do not scold, re-prompt at next check-in. Out of our control — documented. |
| Body map implies diagnostic precision | Coarse regions + disclaimer framing it as a record, not an assessment. |
| Emergency screen shown for non-emergencies | Acceptable asymmetry — a false alarm is far cheaper than a miss. |

## Security Considerations
Pain data is the most sensitive data in the app. Same transport/storage controls as other health data, plus: never include pain content in any analytics or crash report; include `injury_flags` and pain history in the export/delete workflow; if a clinician-share feature is ever built, it must be read-only and explicitly consented per `docs/system-architecture.md` §6. Legal posture: the app is a personal training tool, not a medical device — the disclaimer must be visible, not buried, and no marketing or in-app copy may imply clinical capability.

## Next Steps
MVP complete after this phase. Recommended next: run the full loop for 4+ weeks against real data before adding anything. Then, in order — LLM explanation layer over the existing deterministic reasons (the reasons already exist; the LLM only rephrases them), Connect IQ PoC (`docs/proof-of-concept-plan.md`), web analytics view.
