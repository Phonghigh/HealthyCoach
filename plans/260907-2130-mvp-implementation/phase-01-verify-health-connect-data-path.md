# Phase 01 — Verify Health Connect data path (BLOCKING GATE)

## Context Links
- [plan.md](./plan.md)
- `plans/reports/brainstorm-260907-2130-tech-stack.md` (risk section: HC sync unverified)
- `docs/system-architecture.md` §2 Ingestion
- Health Connect docs: developer.android.com/health-and-fitness/guides/health-connect

## Overview
- **Priority:** P0 — blocks Phase 03, informs Phase 02 module layout.
- **Status:** pending
- **Description:** Build a throwaway Android probe app that reads `ExerciseSessionRecord` from Health Connect and prints what it finds. Answer one question: does the user's Garmin Connect app actually write run activities (with distance/duration/HR/route) into Health Connect on their phone?

## Key Insights
- **Why first:** every downstream ingestion decision depends on this. Building 2 weeks of HC plumbing then discovering Garmin doesn't write there is the single biggest schedule risk in this project.
- Garmin Connect *does* offer Health Connect write on recent Android versions, but it is **opt-in per data type** and historically has written summary-level data more reliably than per-lap/streaming data. Assume nothing about granularity — measure it.
- Health Connect requires Android 14+ (built into OS) or the Health Connect APK on Android 9–13.
- The probe is deliberately throwaway — do NOT invest in architecture here. KISS.

## Requirements
Functional:
1. Request Health Connect read permissions: `ExerciseSessionRecord`, `DistanceRecord`, `HeartRateRecord`, `TotalCaloriesBurnedRecord`, `StepsRecord`, `SpeedRecord`.
2. Read last 60 days of exercise sessions; render a scrollable list.
3. For each session show: start/end time, exercise type, **originating package name** (`metadata.dataOrigin.packageName`), distance, duration, avg/max HR, sample count for HR and Speed.
4. Export the raw findings as JSON to a file/logcat so results can be pasted into the report.

Non-functional: single Activity, no backend, no persistence, no tests. Delete or archive after the gate.

## Architecture
Single-module Android app. `HealthConnectClient.getOrCreate(context)` → permission contract → `readRecords(ReadRecordsRequest(...))` inside a coroutine → state to one Compose list. No repository layer, no DI, no ViewModel indirection beyond one `ViewModel` holding the result list.

## Related Code Files (all new, throwaway)
- `probe-healthconnect/app/build.gradle.kts` — deps: `androidx.health.connect:connect-client`, Compose BOM.
- `probe-healthconnect/app/src/main/AndroidManifest.xml` — HC permission declarations + `<queries>` for the HC package + the `ACTION_SHOW_PERMISSIONS_RATIONALE` intent filter.
- `probe-healthconnect/app/src/main/java/.../MainActivity.kt`
- `probe-healthconnect/app/src/main/java/.../HealthConnectProbe.kt` — read + format logic.
- `plans/reports/phase-01-health-connect-verification-result.md` — the deliverable.

## Implementation Steps
1. Install Android Studio; create empty Compose project `probe-healthconnect`, minSdk 28, targetSdk 34+.
2. Add `androidx.health.connect:connect-client` dependency; sync Gradle.
3. Declare read permissions in the manifest as `<uses-permission android:name="android.permission.health.READ_EXERCISE" />` (and READ_DISTANCE, READ_HEART_RATE, READ_STEPS, READ_TOTAL_CALORIES_BURNED, READ_SPEED).
4. Add the permissions-rationale activity alias/intent filter (Health Connect refuses to grant without it).
5. In `MainActivity`: check `HealthConnectClient.getSdkStatus()`; if `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED`, deep-link to Play Store for the HC APK.
6. Launch the `PermissionController.createRequestPermissionResultContract()` launcher; request the permission set.
7. On grant, read `ExerciseSessionRecord` for `TimeRangeFilter.between(now-60d, now)`.
8. For each session, issue aggregate/read calls for distance, HR, speed constrained to that session's time range.
9. Render the list; log the same data as JSON.
10. **On the user's actual phone** (not emulator — emulators have no Garmin app): install, grant, run.
11. Before running: open Garmin Connect → Settings → Health Connect (or Android Settings → Health Connect → App permissions) and confirm Garmin is granted **write** access to exercise/distance/HR.
12. Do a short outdoor run (or use an already-synced recent run), wait for Garmin Connect sync, re-run the probe.
13. Write `plans/reports/phase-01-health-connect-verification-result.md` recording: HC availability, which permissions Garmin holds, whether sessions appear, `packageName` of the origin, and **granularity** — is HR a series of samples or a single aggregate? Is there per-lap data? Is there route/GPS?

## Todo List
- [ ] Android Studio + SDK installed, device in developer mode
- [ ] Probe project created, HC client dependency added
- [ ] Manifest permissions + rationale intent filter
- [ ] SDK availability check + Play Store fallback
- [ ] Permission request flow working
- [ ] ExerciseSessionRecord read + per-session distance/HR/speed
- [ ] Compose list + JSON log
- [ ] Garmin Connect granted HC write permission (verified in Settings)
- [ ] Run on real device after a Garmin-recorded activity
- [ ] Verification report written with GO / NO-GO decision

## Success Criteria
**GO (branch A):** ≥1 Garmin-originated `ExerciseSessionRecord` visible with correct distance + duration. Record whether HR samples exist — this determines how rich Phase 04's baseline analysis can be.
**NO-GO (branch B):** no Garmin sessions after confirmed sync → Phase 03 builds the FIT/TCX upload path instead. Document exactly why (permission missing / app doesn't write / OS version).
**Either way the gate passes** — the point is a decided, evidenced answer, not a specific answer.

## Risk Assessment
| Risk | Mitigation |
| --- | --- |
| Garmin writes only summary data, no HR samples | Acceptable for MVP — rules engine needs distance/duration/avg HR/RPE, not streams. Note the limit in the report. |
| HC permissions silently denied (no rationale activity) | Step 4 is mandatory; verify grant state in Android Settings → Health Connect. |
| Emulator has no Garmin app → false NO-GO | Test only on the physical phone. |
| Time sunk into probe polish | Hard-limit to ~1 day. It is disposable. |

## Security Considerations
Health data is sensitive. Probe requests **read-only**, stores nothing off-device, and the JSON export must not be committed to git. Delete the probe app from the device after the gate. Never log data to a remote service.

## Next Steps
Report feeds Phase 03's branch choice and Phase 02's `ingestion` module shape. Proceed to Phase 02 in parallel — scaffolding is source-agnostic and not blocked by this gate.
