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
| FIT/TCX parsing | TS library (e.g. `fit-file-parser`/similar) or a small dedicated parser module | MVP data path before Garmin API approval |
| Hosting | Small VPS or managed PaaS (Railway/Render/Fly.io) within $10-50/mo | Matches budget; Postgres + Node fit standard free/low tiers |
| LLM (later, not MVP) | Deferred — bolt on Claude/GPT API after rules engine proven | User chose rules-first |

## Trade-off flagged (not blocking, for the record)
Python was the stronger fit specifically for the deterministic rules/data-analysis layer (baseline
calc, load progression, nutrition math) — richer numeric/data ecosystem. User chose TS/NestJS anyway
for stack unification with a future web app and to stay in one language end-to-end as solo dev. This
is a reasonable trade (KISS: one language, less context switching) — noting it so it's a documented
choice, not an oversight.

## What's still missing before a full detailed implementation plan (unblocked items can proceed; these can't)
1. **Garmin Connect IQ device capability confirmation** — PoC plan (docs/proof-of-concept-plan.md) not
   yet run; `setWorkout()`/data field behavior on Forerunner 165 unverified. Blocks watch-side plan detail.
2. **FIT/TCX sample files** — need real exported files from user's watch to build/test the parser against
   real field structure, not just spec docs.
3. **Health Connect vs manual FIT import decision** — Android native can read Health Connect (if user's
   watch syncs there via Garmin Connect app) as an alternative/complement to manual FIT upload. Not yet
   discussed — worth a follow-up question when planning the data-ingestion phase.
4. **Product type (personal vs commercial)** — still undecided; doesn't block stack, but affects whether
   to build multi-user auth from day one or single-user first. Should be answered before schema design.
5. **Race date/runner baseline fixed?** — affects whether onboarding needs a generic form or can hardcode
   one profile initially. Not yet answered.
6. **Clinical review of safety rules/nutrition content** — no reviewer identified; MVP rules will be
   engineering-authored best-effort, flagged as unreviewed until someone signs off.

None of items 1-3 block starting backend/rules-engine scaffolding; they block the Garmin-sync and
FIT-import phases specifically. Items 4-6 should be resolved before schema/auth design (next planning
session), not before scaffolding.

## Next steps
User to confirm whether to proceed to `/plan` (detailed implementation plan) now covering scaffolding +
FIT import + rules engine (Garmin-sync deferred to its own PoC-gated phase), or resolve items 4-5 first.

## Open questions
- Health Connect vs FIT upload as MVP data source — not yet decided (item 3 above).
- Items 4 and 5 above remain open from prior docs, unresolved this session.
