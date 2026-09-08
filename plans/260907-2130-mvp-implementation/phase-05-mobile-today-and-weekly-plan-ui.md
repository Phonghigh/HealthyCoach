# Phase 05 — Mobile Today + weekly plan UI

## Context Links
- [plan.md](./plan.md) · [phase-04](./phase-04-rules-engine-core.md)
- `docs/product-scope-and-interfaces.md` §2 (Today answers 5 questions), §5 UX principles
- `docs/core-workflows.md` §3
- `docs/project-overview-pdr.md` §6 ("xem được bài hôm nay, target và lý do trong một màn hình")

## Overview
- **Priority:** P0 — this is the only screen the user opens daily.
- **Status:** pending
- **Description:** Compose UI: minimal one-time setup, Today card, weekly plan list, workout detail. Read-only view of Phase 04 output plus the Phase 03 sync status.

## Key Insights
- **Why Today is one card, not a dashboard:** the product thesis is that wearable apps show numbers instead of answering "what do I do today?". A metrics grid here would reproduce the exact problem the product exists to solve. One decision per screen (`docs/product-scope-and-interfaces.md` §5).
- **The five questions Today must answer:** what workout · what target · am I recovered enough · what fuel · what changed in the plan. Anything not serving one of those is out.
- **Why only 3 tabs, not the 5 from the docs:** docs specify Today/Plan/Progress/Coach/Profile. Coach is the LLM (deferred) and Progress is analytics (deferred). Ship Today/Plan/Profile. Adding tabs later is trivial; building empty ones now is waste.
- **Why the setup screen is not a form flow:** race date and baseline are fixed for this user. A one-screen editable summary beats a 6-step wizard nobody but the author will ever run.
- **Offline read matters:** the user may open Today with no signal. Cache the current week locally so Today renders without network.

## Requirements
Functional:
1. **Setup screen** (first launch only): race date (pre-filled 22/01/2027), goal type (completion), baseline weekly km (pre-filled 25), run days, long-run day, max long-run duration. Save → `POST /profile` → trigger `POST /plans/generate`.
2. **Today screen:** workout type + distance/duration · pace range + HR zone + RPE · short reason lines (max 3) · fueling summary if long run (Phase 07 fills this) · CTA to workout detail · sync status · empty states (no plan / rest day / no data source).
3. **Plan screen:** current week as 7 day rows (type, distance, done/planned marker) + week summary (target km, sessions, long run) + ability to page to next/previous week.
4. **Workout detail:** warm-up → main set → cool-down, targets per segment, the reason text, and the `AppliedConstraint` list rendered as "why this week looks like this".
5. **Profile screen:** view/edit setup values, re-generate plan, show `rulesVersion`.
6. Loading, error, and empty states for every screen. Error state must never be a blank screen.

Non-functional: Today renders from cache in <1s offline · text scales with system font size · sufficient contrast · no health jargon without an inline explanation.

## Architecture
MVVM + Hilt. `Screen -> ViewModel -> Repository -> (Retrofit | Room cache)`. Unidirectional data flow: ViewModel exposes a single `StateFlow<UiState>` sealed class (`Loading | Empty | Error | Content`) — this is what makes empty/error states impossible to forget, since the compiler demands every branch.

Room holds a small cache of the current week + today's workout. Network is the source of truth; cache is a read-through fallback with a "last updated" stamp shown to the user rather than silently serving stale data.

No business logic in the app. If the UI needs to decide something about training, that decision belongs in Phase 04 on the backend. The Android app is a rendering surface — this keeps the deterministic, tested logic in one place and makes a future iOS/web client free.

## Related Code Files (all new)
- `.../ui/setup/SetupScreen.kt`, `SetupViewModel.kt`
- `.../ui/today/TodayScreen.kt`, `TodayViewModel.kt`, `TodayWorkoutCard.kt`, `ReasonList.kt`
- `.../ui/plan/PlanScreen.kt`, `PlanViewModel.kt`, `WeekSummaryHeader.kt`, `DayRow.kt`
- `.../ui/workout/WorkoutDetailScreen.kt`, `WorkoutDetailViewModel.kt`, `SegmentCard.kt`
- `.../ui/profile/ProfileScreen.kt`, `ProfileViewModel.kt`
- `.../ui/common/UiState.kt`, `ErrorView.kt`, `EmptyView.kt`, `LoadingView.kt`
- `.../ui/theme/Color.kt`, `Type.kt`, `Theme.kt`
- `.../data/dto/PlanDto.kt`, `WorkoutDto.kt`, `ProfileDto.kt`
- `.../data/PlanRepository.kt`, `ProfileRepository.kt`
- `.../data/local/AppDatabase.kt`, `local/PlanDao.kt`, `local/PlanEntity.kt`
- `.../di/AppModule.kt`
- `.../ui/today/TodayViewModelTest.kt`, `.../ui/plan/PlanViewModelTest.kt`
Backend additions:
- `backend/src/profile/profile.module.ts`, `profile.controller.ts`, `profile.service.ts`
- `backend/src/plans/dto/today-response.dto.ts` — one endpoint returning everything Today needs

## Implementation Steps
1. Backend: add `GET /today` returning `{ date, workout, targets, reasons[], fueling?, lastSyncAt, planStatus }`. **One endpoint, one round-trip** — Today must not orchestrate 4 calls; that guarantees partial-load flicker.
2. Backend: `GET /profile`, `PUT /profile`; `PUT` triggers plan regeneration.
3. Android: define `UiState<T>` sealed interface in `ui/common/`.
4. Theme: colors, typography, spacing. Keep it plain Material 3 — visual design is not the risk here.
5. `SetupScreen`: form with pre-filled values, day-of-week multi-select chips, save button. Show only when profile absent (flag in DataStore).
6. `PlanRepository`: Retrofit call → map DTO → write to Room → emit. On network failure emit cached data flagged `stale`.
7. `TodayViewModel`: expose `StateFlow<UiState<TodayUi>>`; refresh on resume; trigger Phase 03 sync before fetching.
8. `TodayWorkoutCard`: workout name, distance/duration, target block (pace range, zone, RPE), reason list, CTA. Rest day = distinct, positively-framed card, not an empty state.
9. `ReasonList`: max 3 lines, each a short sentence from the backend. Never render raw rule names.
10. `PlanScreen`: `LazyColumn` of `DayRow`s + `WeekSummaryHeader`; horizontal pager or prev/next buttons for weeks.
11. `WorkoutDetailScreen`: segments list; a "Why this plan?" expandable section rendering `AppliedConstraint[]` — progressive disclosure per `docs/product-scope-and-interfaces.md`.
12. `ProfileScreen`: edit values, regenerate button (with a confirm dialog — regeneration rewrites the plan), show `rulesVersion` and last sync.
13. Wire the Phase 03 `SyncStatusCard` into Today as a compact strip.
14. ViewModel unit tests with a fake repository: loading → content, error path, empty-plan path, stale-cache path.
15. Manual pass: airplane mode (cached render), fresh install (setup → plan → today), rest day, no-activities-yet.

## Todo List
- [ ] `GET /today` aggregate endpoint + DTO
- [ ] `GET/PUT /profile` w/ regeneration trigger
- [ ] `UiState` sealed interface + Loading/Error/Empty components
- [ ] Material 3 theme
- [ ] Setup screen w/ pre-filled fixed values
- [ ] Room cache + PlanRepository read-through w/ stale flag
- [ ] Today screen: workout card, targets, reasons, sync strip, rest-day + empty states
- [ ] Plan screen: week summary + day rows + week paging
- [ ] Workout detail w/ segments + "why this plan" disclosure
- [ ] Profile screen w/ regenerate confirm
- [ ] ViewModel tests (content/error/empty/stale)
- [ ] Manual offline + fresh-install passes

## Success Criteria
From a fresh install: setup completes in <2 min, a plan generates, and Today shows today's workout with target and reason **on one screen without scrolling** on a standard phone. Airplane mode still renders Today from cache with a visible "last updated" stamp. Every screen has a non-blank error and empty state.

## Risk Assessment
| Risk | Mitigation |
| --- | --- |
| Compose learning curve (new to user) | Screens are simple lists/cards; use official Compose samples. Budget extra time on Today, then reuse its patterns. |
| Today grows into a dashboard | Hard rule: nothing on Today unless it answers one of the five questions. Re-read §2 of product-scope doc before adding anything. |
| Cache serves stale plan silently | Always show "last updated"; never hide staleness. |
| Business logic leaking into ViewModels | Code review check: no pace/load arithmetic in Kotlin. Formatting only. |

## Security Considerations
Device key stored via `local.properties` → `BuildConfig` for MVP; note in README that this is acceptable only because the app is single-user and not distributed — a shipped app would need EncryptedSharedPreferences or per-device tokens. No health data in logcat in release builds (strip with a ProGuard/Timber release tree). Screenshots of health data: not restricted for MVP, note as a deferred consideration.

## Next Steps
Phase 06 adds the check-in entry point on Today and the workout detail screen. Phase 07 fills the fueling slot already reserved in the Today card DTO.
