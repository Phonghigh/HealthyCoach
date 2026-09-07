# Brainstorm: HealthyCoach tech stack + what's missing for a detailed plan

date: 260907-2130 | scope: stack decision + gap list for detailed implementation plan

## Problem statement
Docs (project-overview-pdr, product-scope-and-interfaces, assumptions-and-open-questions) define product
direction but no stack/architecture decision. Need enough decided to write a real implementation plan.

## Decisions (confirmed w/ user via AskUserQuestion)
- Team: solo dev.
- Mobile platform (phase 1): Android only.
- Dev background: JS/TS, Python, open to learning.
- Hosting budget: $10-50/mo OK.
- Phase 1 scope: mobile + Garmin only, no web app yet.
- Timeline: no hard deadline (race 22/01/2027 is far out) — sustainable pace.
- AI approach: rules-first (deterministic training load/safety/nutrition logic), LLM layered on later.
- Mobile stack: **Native Android (Kotlin)** — chosen over React Native despite being a new language,
  because it's the better long-term fit for Health Connect API + Garmin FIT SDK (fewer bridge layers).
- Backend stack: **Node.js/TypeScript (NestJS) + PostgreSQL** — chosen over Python/FastAPI so backend
  language matches a future web app (TS) and user's existing JS/TS strength, even though Python was
  flagged as a data/rules-logic strength. User's call — noted as trade-off below.

## Final stack
| Layer | Choice | Why |
| --- | --- | --- |
| Mobile (Android) | Kotlin, native (Jetpack Compose recommended for UI) | Health Connect + Garmin FIT SDK are Java/Kotlin-native; avoids bridge overhead |
| Garmin watch | Connect IQ, Monkey C (Garmin's only option — same regardless of app stack) | Mandatory for on-watch data field / workout sync |
| Backend | Node.js + NestJS (TypeScript) | Structured, testable modules; matches user's JS/TS strength; shares types with future web app |
| Database | PostgreSQL | Structured relational data: athlete profile, plans, activities, check-ins |
| Rules engine | Plain TS modules inside NestJS, unit-tested, separate from any LLM code path | Docs' NFR requires load/safety/nutrition/adjustment logic to be deterministic & testable, independent of LLM |
| FIT/TCX parsing | TS library (e.g. `fit-file-parser`/similar) or a small dedicated parser module | Fallback path if Health Connect sync doesn't pan out |
| Hosting | Small VPS or managed PaaS (Railway/Render/Fly.io) within $10-50/mo | Matches budget; Postgres + Node fit standard free/low tiers |
| LLM (later, not MVP) | Deferred — bolt on Claude/GPT API after rules engine proven | User chose rules-first |

## Trade-off flagged (not blocking, for the record)
Python was the stronger fit specifically for the deterministic rules/data-analysis layer (baseline
calc, load progression, nutrition math) — richer numeric/data ecosystem. User chose TS/NestJS anyway
for stack unification with a future web app and to stay in one language end-to-end as solo dev. This
is a reasonable trade (KISS: one language, less context switching) — noting it so it's a documented
choice, not an oversight.

## Follow-up decisions (resolved same session)
- Product type: **personal tool for now** — single-user MVP, no auth/multi-tenancy. Add later if it grows.
- Race/baseline: **fixed to user** — hardcode 22/01/2027 race date + current baseline for MVP; generic
  onboarding form deferred until the core loop (import → plan → check-in → adjust) works end-to-end.
- MVP data source: **Health Connect integration first** (not manual FIT upload) — user's Garmin Connect
  app is assumed to sync activities to Android Health Connect; this needs on-device confirmation before
  planning starts (see risk below), but is the default path to build against.

## Risk from this session
Health Connect integration was chosen without confirming the user's Garmin Connect app actually writes
activity data to Health Connect on their device. **Must verify this first** (open Health Connect app,
check if Garmin activities appear) before committing scaffolding work to that path. If it doesn't sync,
fall back to manual FIT/TCX upload (docs' originally stated fallback) without much rework — same backend
ingestion/parsing logic, different Android-side data source.

## Next steps
All blocking items resolved. Ready for `/plan` — detailed implementation plan for MVP scaffolding
(Kotlin Android app, NestJS+Postgres backend, rules engine skeleton) + Health Connect data ingestion
(pending on-device sync verification) + single-user fixed-profile onboarding. Garmin watch/Connect IQ
sync stays deferred behind its own PoC (docs/proof-of-concept-plan.md).

## Open questions
None blocking. One verification task before ingestion work starts: confirm Garmin Connect → Health
Connect sync actually happens on user's device (see Risk above).
