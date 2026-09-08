---
title: "HealthyCoach MVP — AI Marathon Coach (rules-first, Android + NestJS)"
description: "Phased plan for single-user marathon coach MVP: Health Connect ingestion, deterministic rules engine, Compose UI, check-in driven plan adjustment."
status: pending
priority: P1
effort: ~26d (solo, sustainable pace)
branch: worktree-eager-orbiting-sphinx
tags: [mvp, android, kotlin, nestjs, postgres, rules-engine, health-connect]
created: 2026-09-07
---

# HealthyCoach MVP — Implementation Plan

Race: 22/01/2027. Baseline: 20–35 km/week. Single user, no auth. Rules-only, **no LLM in MVP**.

## Fixed decisions (do not re-open)
- Android native (Kotlin + Jetpack Compose). No iOS, no web in MVP.
- Backend NestJS (TypeScript) + PostgreSQL, hosted on Railway/Render/Fly ($10–50/mo).
- All training/safety/nutrition/adjustment logic = plain deterministic TS modules, table-driven unit tests.
- Primary data path = Android Health Connect (assumes Garmin Connect writes there). Fallback = manual FIT/TCX upload.
- Garmin Connect IQ / on-watch sync: **out of scope**, gated behind `docs/proof-of-concept-plan.md`.
- Onboarding = minimal one-time setup screen, values pre-filled. Generic dynamic form deferred.

## Blocking gate
**Phase 01 is a hard gate.** Do not scaffold ingestion until Garmin→Health Connect sync is confirmed on the real device. Outcome selects Phase 03 branch A (Health Connect) or B (FIT/TCX upload).

## Phases
| # | Phase | Status | One-liner |
| --- | --- | --- | --- |
| 01 | [Verify Health Connect data path](./phase-01-verify-health-connect-data-path.md) | pending | On-device probe app: do Garmin runs appear in Health Connect? Gate for all ingestion work. |
| 02 | [Project scaffolding](./phase-02-project-scaffolding.md) | pending | Monorepo, NestJS skeleton, Postgres schema + migrations, Compose app shell, CI. |
| 03 | [Data ingestion](./phase-03-data-ingestion.md) | pending | Read activities (Health Connect or FIT/TCX), normalize, idempotent upload to backend. |
| 04 | [Rules engine core](./phase-04-rules-engine-core.md) | pending | Baseline analysis, roadmap phases, weekly plan generation, load progression + safety guardrails. |
| 05 | [Today + weekly plan UI](./phase-05-mobile-today-and-weekly-plan-ui.md) | pending | Today card (workout/target/reason/fuel), week view, workout detail. |
| 06 | [Check-in + plan adjustment](./phase-06-checkin-and-plan-adjustment.md) | pending | <30s post-run check-in; rule-driven adjustment w/ explanation + audit log. |
| 07 | [Fueling / nutrition card](./phase-07-fueling-nutrition-card.md) | pending | Carb/h → gel count math, hydration guidance, post-run intake + GI confirmation. |
| 08 | [Pain check-in + safety escalation](./phase-08-pain-checkin-safety-escalation.md) | pending | Body map, risk classification, escalation actions. No diagnosis. |

## Key dependencies
- 01 → 03 (data source branch selection). 02 → everything else.
- 03 → 04 (baseline analysis needs real activity history; seed fixtures unblock 04 early).
- 04 → 05, 06, 07. 06 → 07 (fuel confirmation flows through check-in). 06 → 08 (pain is a check-in branch).
- 08 safety rules **override** 04/06 outputs — safety engine runs last in pipeline.

## MVP acceptance (from docs §6)
Today screen answers 5 questions in one screen · check-in yields conclusion + action · missed session never creates two adjacent hard days · long run has fueling plan · pain red flag stops/reduces load and points to professional help, never diagnoses.

## Deferred (explicitly not in this plan)
LLM explanation layer · Connect IQ data field & workout push · web app · route planner · food photo logging · multi-user/auth · readiness score from HRV.
