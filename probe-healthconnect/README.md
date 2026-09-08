# probe-healthconnect

Throwaway Android app for Phase 01 of the HealthyCoach MVP plan
(`plans/260907-2130-mvp-implementation/phase-01-verify-health-connect-data-path.md`).

**This is not part of the product.** It exists only to answer one question: does Garmin Connect
actually write exercise sessions (with distance/HR/duration) into Health Connect on a real phone?
Delete or archive this folder once the phase-01 verification report is written.

## Opening the project

1. Open Android Studio (Koala/Ladybug or newer).
2. `File > Open` and select this `probe-healthconnect/` folder.
3. Let Android Studio generate the Gradle wrapper and sync — the wrapper files
   (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) are intentionally not checked in here.
4. Build/run configuration `app` should appear automatically.

## Running it — real device required

**Do not use an emulator.** Emulators have no Garmin Connect app and no real Health Connect
sync history, so a "no sessions found" result would be a false NO-GO. Run only on the user's
actual phone with Garmin Connect installed, in developer mode with USB debugging enabled.

Before running, confirm in Android Settings → Health Connect → App permissions that Garmin
Connect has been granted **write** access to Exercise/Distance/Heart Rate (see phase-01 doc,
step 11).

## What it does

- Checks Health Connect SDK availability; deep-links to Play Store if the provider needs
  installing/updating.
- Requests read permission for Exercise, Distance, Heart Rate, Total Calories Burned, Steps,
  and Speed records.
- Reads the last 60 days of `ExerciseSessionRecord`s and, per session, the distance/HR/speed
  records in that session's time range.
- Renders a scrollable Compose list (start/end, type, origin package, distance, duration,
  avg/max HR, HR/speed sample counts).
- Logs the same results as JSON via `Log.d("HealthConnectProbe", ...)` so they can be copied
  out of `adb logcat` into the phase-01 verification report.

## Security note

Read-only. Nothing is persisted or sent off-device. Do not commit any exported JSON/logcat
output containing real health data to git.
