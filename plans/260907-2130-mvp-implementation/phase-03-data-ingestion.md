# Phase 03 — Data ingestion

## Context Links
- [plan.md](./plan.md) · [phase-01 gate](./phase-01-verify-health-connect-data-path.md) · [phase-02](./phase-02-project-scaffolding.md)
- `docs/system-architecture.md` §2 Ingestion ("không suy diễn dữ liệu thiếu thành fact")
- `docs/code-standards.md` §2 idempotency, §4 import tests

## Overview
- **Priority:** P0 — the rules engine is worthless without real activity data.
- **Status:** done — **Branch A selected by Phase 01 GO result**.
- **Description:** Get completed runs from the watch into `completed_activities`, normalized, deduplicated, with provenance.

## Key Insights
- **Why the backend contract is identical for both branches:** the backend accepts a normalized `ActivityImportDto`. Health Connect vs FIT parsing is an *Android-side* difference only. This is why the Phase 01 gate is cheap to lose — a NO-GO costs one Android module, not a redesign.
- **Why idempotency matters here specifically:** the app will re-sync the same 60-day window on every open. Without a unique key on `(source, external_id)` you get duplicate activities, which corrupts weekly mileage, which corrupts load progression, which produces an unsafe plan. The dedup constraint is a *safety* control, not a tidiness one.
- **Missing data must stay missing.** If Health Connect gives no HR, store `null` — never impute. Phase 04 rules must branch on data availability (docs explicitly forbid inferring missing data as fact).
- **Where dedup lives:** database unique constraint + `upsert`. Not application-level "check then insert" — that races.

## Requirements
Functional:
1. Android reads completed runs from the chosen source.
2. Normalize to: `startedAt` (UTC + original tz offset), `durationSec`, `distanceM`, `avgHrBpm?`, `maxHrBpm?`, `avgPaceSecPerKm` (derived), `elevationGainM?`, `source`, `externalId`.
3. `POST /activities/import` accepts a batch (array), upserts by `(source, externalId)`, returns per-item `created | duplicate | rejected + reason`.
4. `GET /activities?from&to` returns the list for UI/rules.
5. Sync trigger: manual "Sync" button + automatic on app foreground, max once per 15 min.
6. Import state visible in UI: last sync time, count imported, errors.

Non-functional: import of 60 days must complete <10s on a normal connection · malformed input rejected with a stable error code, never a 500 · re-import of the same window is a no-op.

## Architecture
**Branch A — Health Connect (preferred, if Phase 01 = GO)**
`HealthConnectSource.kt` reads `ExerciseSessionRecord` filtered to running types, plus per-session aggregates for distance/HR/elevation. `externalId` = HC record `metadata.id`. A `changesToken` is stored in DataStore so subsequent syncs read only deltas (`getChanges`) instead of the full window — cheaper and avoids re-processing.

**Branch B — FIT/TCX upload (fallback, if Phase 01 = NO-GO)**
Android `FileImportSource.kt` uses the Storage Access Framework to pick `.fit`/`.tcx` files exported from Garmin Connect, and uploads the **raw file** to `POST /activities/upload` (multipart). Backend parses with a TS FIT parser (`fit-file-parser`) or an XML parser for TCX. Parsing server-side, not on-device: easier to test with fixtures, easier to fix without shipping an APK. `externalId` = FIT `file_id` serial+timestamp, or SHA-256 of file bytes as fallback.

Both branches converge on the same `ActivityImportDto` and the same service.

## Related Code Files (all new)
Backend (both branches):
- `backend/src/activities/activities.module.ts`, `activities.controller.ts`, `activities.service.ts`
- `backend/src/activities/dto/activity-import.dto.ts` (class-validator)
- `backend/src/activities/activity-normalizer.ts` — pure: derive pace, validate plausibility
- `backend/src/activities/activities.service.spec.ts`, `activity-normalizer.spec.ts`
Backend (branch B only):
- `backend/src/activities/parsers/fit-parser.ts`, `parsers/tcx-parser.ts`, `parsers/parsers.spec.ts`
- `backend/test/fixtures/` — ≥3 real FIT + 1 TCX + 1 truncated/corrupt file
Android:
- `.../health/ActivitySource.kt` (interface — both branches implement it)
- `.../health/HealthConnectSource.kt` **or** `.../health/FileImportSource.kt`
- `.../health/HealthConnectPermissions.kt` (branch A)
- `.../data/dto/ActivityDto.kt`, `.../data/ActivityRepository.kt`, `.../data/HealthCoachApi.kt`
- `.../ui/sync/SyncViewModel.kt`, `.../ui/sync/SyncStatusCard.kt`

## Implementation Steps
1. Define `ActivityImportDto` + validators: `distanceM > 0 && < 200000`, `durationSec > 60 && < 86400`, `startedAt` not in the future. Reject implausible rows with `INVALID_ACTIVITY` rather than storing garbage.
2. `activity-normalizer.ts`: pure function `normalize(raw) -> NormalizedActivity | RejectionReason`. Derives `avgPaceSecPerKm = durationSec / (distanceM/1000)`. Unit-test the boundaries.
3. Prisma: add `@@unique([source, externalId])` on `completed_activities`; migration.
4. `activities.service.ts`: `importBatch()` → normalize each → `prisma.upsert` on the composite key → return per-item outcome. Wrap in one transaction.
5. `activities.controller.ts`: `POST /activities/import`, `GET /activities?from&to`. Guarded by device key.
6. Unit tests: empty batch · duplicate in the same batch · duplicate against DB · malformed · missing HR (must succeed with null).
7. **Branch A:** port the Phase 01 probe read logic into `HealthConnectSource.kt` behind the `ActivitySource` interface; add permission request flow; map records → `ActivityDto`; persist `changesToken` in DataStore.
   **Branch B:** `FileImportSource.kt` using `ActivityResultContracts.OpenMultipleDocuments`; multipart upload; backend `fit-parser.ts` extracting session summary fields; test against fixtures including the corrupt file (must return `PARSE_FAILED`, not crash).
8. `ActivityRepository.kt`: call source → POST batch → expose `SyncResult` state.
9. `SyncStatusCard.kt`: last sync timestamp, imported/duplicate/rejected counts, error text. Put it on the Today screen (Phase 05 integrates it properly).
10. Add the 15-minute throttle + foreground trigger in `SyncViewModel`.
11. End-to-end check: run outdoors → sync → `GET /activities` shows the run with correct distance and pace.

## Todo List
- [x] `ActivityImportDto` + validation rules
- [x] Pure `activity-normalizer` + unit tests
- [x] Unique constraint migration `(source, externalId)`
- [x] `importBatch` upsert w/ per-item outcomes
- [x] Import/list endpoints behind device-key guard
- [x] Service unit tests incl. duplicate + malformed + null-HR cases (21/21 tests pass)
- [x] Android `ActivitySource` interface
- [x] Branch A: HealthConnectSource + permissions + changes token
- [-] Branch B: file picker + server-side FIT/TCX parser + fixtures — N/A — branch A selected
- [x] `ActivityRepository` + sync throttle + foreground trigger
- [x] Sync status UI
- [x] E2E: real run appears in backend with correct distance/pace (live-verified with local Postgres + server)

## Success Criteria
A real outdoor run appears in `completed_activities` within one sync, with distance within 1% of Garmin Connect's figure. Syncing 3× in a row imports it once. A corrupt/implausible input yields a typed error, never a 500 or a bad row. Every activity row has non-null `source` and `externalId`.

## Risk Assessment
| Risk | Mitigation |
| --- | --- |
| HC returns a summary only, no HR | Store nulls; Phase 04 falls back to RPE-based load. Documented in Phase 01 report. |
| Timezone bugs corrupt weekly aggregation | Store UTC + offset; do week bucketing in the athlete's local tz explicitly, with a unit test crossing a week boundary at 23:00 local. |
| Duplicates from re-sync | DB unique constraint (step 3) — the only reliable defence. |
| FIT parser library gaps (branch B) | Only session-summary fields are needed; if the lib fails, TCX (plain XML) is a trivial fallback. |
| Garmin Connect sync lag to HC | Not fixable by us. Show "last synced" honestly; never claim data is complete. |

## Security Considerations
Health Connect permissions requested at point of use with a clear rationale screen; user can revoke in Android Settings and the app must degrade gracefully (show "no data source connected", not crash). Uploaded FIT files may contain home GPS coordinates — store only what's used, do not log file contents, and delete raw uploads after parsing. All traffic over HTTPS with the device-key header.

## Next Steps
Phase 04 consumes `GET /activities` for baseline analysis. If branch B, add a Phase 05 UI affordance explaining the manual export→import ritual.
