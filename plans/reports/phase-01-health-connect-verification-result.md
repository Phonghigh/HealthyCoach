# Phase 01 — Health Connect verification result

date: 260908-1100 | decision: **GO (branch A — Health Connect)**

## Setup notes (for future reference / anyone re-running this probe)
- Device: Google Pixel (Android 14+, built-in Health Connect, no separate APK needed).
- Blocker hit: app never appeared in Health Connect's app list, permission request silently
  no-op'd (no dialog, `granted` set came back empty for all 6 permissions).
- Root cause: `MainActivity` was missing the Android-14-required intent-filter that registers
  an app as a Health Connect participant:
  ```xml
  <intent-filter>
      <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
      <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
  </intent-filter>
  ```
  The `activity-alias` with `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE` we'd added is a
  different, older rationale intent — it does NOT register the app. Both are needed. Fixed in
  `probe-healthconnect/app/src/main/AndroidManifest.xml`. Source:
  https://eevis.codes/blog/2024-01-12/exploring-health-connect-pt-1-setting-up-permissions/
- After the manifest fix + reinstall, permission dialog appeared, all 6 granted, probe read
  60 days of exercise sessions successfully.

## Result: does Garmin write to Health Connect?
**Yes.** 4 sessions with `originPackage: com.garmin.android.apps.connectmobile` found in the last
60 days (2026-09-04 through 2026-09-07), each with:
- Distance (10–15 km range)
- Avg/max heart rate (e.g. avg 156–166, max 170–180 bpm)
- **245–271 heart rate samples per session** — sample-level, not a single aggregate.
- **243–268 speed samples per session** — also sample-level.

This is richer than the phase doc's "assume nothing, might be summary-only" caution — Garmin is
writing near-full-resolution HR and speed streams, not just session totals.

## Caveat found (methodology note, not a blocker)
14 Strava-origin sessions also appear in the same window. For sessions between 2026-09-04 and
2026-09-07, Strava and Garmin sessions overlap in time and the probe's `HeartRateRecord`/
`SpeedRecord` reads are scoped to the **session's time range**, not filtered by `dataOrigin`. That
means the "Strava" session results also picked up Garmin's HR/speed records where the time ranges
overlap (both apps wrote a record for the same run). Older Strava-only sessions (Aug 12–27, before
Garmin's HC write access started showing up) correctly show `avgHeartRate: null` — confirming this
is a read-scoping quirk, not app misbehavior.
**Action for Phase 03:** the real ingestion pipeline must key off `metadata.dataOrigin.packageName`
per record (not just per session) and/or dedupe overlapping sessions from multiple apps for the
same activity, otherwise HR data could get double-counted or misattributed.

## Decision for Phase 03
Build ingestion against **Health Connect, branch A**, filtering to
`dataOrigin.packageName == "com.garmin.android.apps.connectmobile"` for the source of truth on
distance/duration/HR, with per-record origin filtering (see caveat above) rather than time-range-only
scoping.

## Todo list status (from phase-01)
All items complete — probe built, manifest fixed, permissions granted, ≥1 Garmin session confirmed
with HR samples, this report written. **GO decision recorded.**
