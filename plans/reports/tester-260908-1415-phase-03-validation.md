# Phase 03 Validation Report

**Date:** 2026-09-08 | **Status:** FAIL (critical DTO mismatch)

## 1. Backend Build & Tests

**Result:** PASS ✓

- `npm ci` — success
- `npm run lint` — success (no errors)
- `npm run build` — success (resolved Prisma client generation issue)
- `npm test` — all 4 suites pass, 20 tests pass

## 2. Activities Tests (Verbose)

**Result:** PASS ✓

All required test cases covered:
- ✓ Empty batch returns empty array without transaction
- ✓ Duplicate within same batch flagged as `duplicate`
- ✓ Duplicate against existing DB row flagged as `duplicate`
- ✓ Malformed/implausible row rejected (test name: "rejects a malformed/implausible row without touching the DB for that row")
- ✓ Null HR accepted & stored as null (test name: "accepts a row with null HR and stores HR as null (never imputed)")

Boundary validation tests all pass:
- distanceM: min=1, max=199999 ✓
- durationSec: min=61, max=86399 ✓

## 3. Live Migration & API Test

**Result:** PASS ✓

- Docker compose up (port 54329) — success
- Prisma migrate deploy — success (2 migrations found, no pending)
- Migration contains unique constraint on (source, external_id) ✓
- POST /activities/import, first new batch → status: `created` ✓
- POST /activities/import, same batch again → status: `duplicate` ✓ (idempotent, no duplicate row created)
- Null HR/elevation fields accepted ✓

## 4. DTO Validation Bounds

**Result:** PASS ✓

Backend `activity-import.dto.ts`:
- distanceM: @IsNumber() @Min(1) @Max(199999) — matches phase doc ✓
- durationSec: @IsInt() @Min(61) @Max(86399) — matches phase doc ✓
- startedAt: @MaxDate() — not in future ✓

## 5. Android Static Code Review

### 5a. Android 14 Intent-Filter (Regression Check)

**Result:** PASS ✓

AndroidManifest.xml lines 41-44:
```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
    <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
</intent-filter>
```

Also includes `ViewPermissionUsageActivity` alias (lines 49-57) — matches Phase 01 requirement.

### 5b. Origin Filtering in HealthConnectSource.kt

**Result:** PASS ✓

**Is the fix really in the code?** YES, confirmed.

Lines 96-101 (DistanceRecord):
```kotlin
val originPackage = session.metadata.dataOrigin.packageName
val distanceM = client.readRecords(ReadRecordsRequest(DistanceRecord::class, sessionRange))
    .records
    .filter { it.metadata.dataOrigin.packageName == originPackage }
    .sumOf { it.distance.inMeters }
```

Lines 104-107 (HeartRateRecord):
```kotlin
val hrSamples = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, sessionRange))
    .records
    .filter { it.metadata.dataOrigin.packageName == originPackage }
    .flatMap { it.samples }
```

Lines 114-116 (SpeedRecord):
```kotlin
client.readRecords(ReadRecordsRequest(SpeedRecord::class, sessionRange))
    .records
    .filter { it.metadata.dataOrigin.packageName == originPackage }
```

**Conclusion:** All three record types (Distance, HeartRate, Speed) are filtered by `dataOrigin.packageName == originPackage`, not time range alone. The Phase 01 data-contamination fix is real and in place.

### 5c. DTO Field Name Mismatch (CRITICAL)

**Result:** FAIL ✗ — Silent field-name mismatch between backend & Android

**Backend response** (activities.service.ts):
```typescript
export interface ImportOutcome {
  externalId: string;
  status: ImportOutcomeStatus;  // ← field is "status"
  reason?: string;
}
```

Confirmed by live API test: `{"results":[{"externalId":"test-run-002-new","status":"created"}]}`

**Android DTO** (ActivityDto.kt, lines 28-31):
```kotlin
@Serializable
data class ActivityImportResultItem(
    val externalId: String,
    val outcome: String,  // ← WRONG: expects "outcome" not "status"
    val reason: String? = null,
)
```

**Impact:** When Android makes a POST to `/activities/import`, the response parsing will FAIL because Kotlinx serialization expects a field named `outcome` but the backend returns `status`. This breaks real sync even though tests pass locally — exact scenario Phase 01 testing was meant to catch.

---

## Summary

| Item | Status | Notes |
|------|--------|-------|
| Backend build/lint/test | PASS | Clean build, 20 tests pass |
| Activities tests | PASS | All 5 required test cases covered |
| Live migration | PASS | Applied cleanly, unique constraint verified |
| API idempotency | PASS | create→duplicate pattern confirmed |
| DTO validation bounds | PASS | Match phase doc exactly |
| Android manifest (regression) | PASS | VIEW_PERMISSION_USAGE intent-filter present |
| Origin filtering (regression) | PASS | Code filters by dataOrigin.packageName, not time-range alone |
| **DTO field name sync** | **FAIL** | Backend: `status`, Android: `outcome` — silent mismatch |

## Blocking Issues

**CRITICAL:** Fix the field name mismatch in Android's `ActivityImportResultItem.kt`. Change `outcome` to `status` to match backend's `ImportOutcome` interface. This breaks JSON deserialization in real sync.

## Unresolved Questions

- None

---

**Session:** https://claude.ai/code/session_01KFrrfM7TdehwCRvBxRABk3
