package com.healthycoach.health

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_RUNNING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_TRAIL_RUNNING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_TREADMILL_RUNNING
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.healthycoach.data.dto.ActivityDto
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

private val Context.syncDataStore by preferencesDataStore(name = "health_connect_sync")
private val CHANGES_TOKEN_KEY = stringPreferencesKey("hc_changes_token")

private val RUNNING_EXERCISE_TYPES = setOf(
    EXERCISE_TYPE_RUNNING,
    EXERCISE_TYPE_TRAIL_RUNNING,
    EXERCISE_TYPE_TREADMILL_RUNNING,
)

/**
 * Reads completed runs from Health Connect. Critical rule (see
 * plans/reports/phase-01-health-connect-verification-result.md): HR/Distance/Speed reads for a
 * session must be filtered by dataOrigin.packageName matching the session's own origin, never
 * scoped by time range alone — multiple apps (Garmin, Strava) can write overlapping sessions to
 * Health Connect, and a naive time-range read cross-contaminates one app's samples into another
 * app's session.
 */
class HealthConnectSource(private val context: Context) : ActivitySource {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    override suspend fun fetchNewActivities(): List<ActivityDto> {
        val storedToken = context.syncDataStore.data.first()[CHANGES_TOKEN_KEY]
        val sessions = if (storedToken == null) {
            readFullWindow()
        } else {
            readSinceToken(storedToken)
        }
        persistLatestToken()
        return sessions
    }

    private suspend fun readFullWindow(): List<ActivityDto> {
        val end = Instant.now()
        val start = end.minus(Duration.ofDays(60))
        val range = TimeRangeFilter.between(start, end)

        val response = client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, range))
        return response.records
            .filter { it.exerciseType in RUNNING_EXERCISE_TYPES }
            .map { toActivityDto(it) }
    }

    private suspend fun readSinceToken(token: String): List<ActivityDto> {
        val changes = mutableListOf<ExerciseSessionRecord>()
        var nextToken = token
        var hasMore = true
        while (hasMore) {
            val response = client.getChanges(nextToken)
            response.changes.forEach { change ->
                if (change is UpsertionChange && change.record is ExerciseSessionRecord) {
                    val record = change.record as ExerciseSessionRecord
                    if (record.exerciseType in RUNNING_EXERCISE_TYPES) {
                        changes.add(record)
                    }
                }
            }
            hasMore = response.hasMore
            nextToken = response.nextChangesToken
        }
        return changes.map { toActivityDto(it) }
    }

    private suspend fun persistLatestToken() {
        val newToken = client.getChangesToken(
            ChangesTokenRequest(setOf(ExerciseSessionRecord::class))
        )
        context.syncDataStore.edit { prefs -> prefs[CHANGES_TOKEN_KEY] = newToken }
    }

    private suspend fun toActivityDto(session: ExerciseSessionRecord): ActivityDto {
        val originPackage = session.metadata.dataOrigin.packageName
        val sessionRange = TimeRangeFilter.between(session.startTime, session.endTime)

        val distanceM = client.readRecords(ReadRecordsRequest(DistanceRecord::class, sessionRange))
            .records
            .filter { it.metadata.dataOrigin.packageName == originPackage }
            .sumOf { it.distance.inMeters }

        val hrSamples = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, sessionRange))
            .records
            .filter { it.metadata.dataOrigin.packageName == originPackage }
            .flatMap { it.samples }

        val avgHrBpm = if (hrSamples.isNotEmpty()) hrSamples.map { it.beatsPerMinute }.average().toInt() else null
        val maxHrBpm = hrSamples.maxOfOrNull { it.beatsPerMinute }?.toInt()

        // Speed records are read for future use (e.g. pace curves) but not persisted in the MVP
        // dto — reading here still requires the same origin filter to avoid contamination.
        client.readRecords(ReadRecordsRequest(SpeedRecord::class, sessionRange))
            .records
            .filter { it.metadata.dataOrigin.packageName == originPackage }

        val durationSec = Duration.between(session.startTime, session.endTime).seconds
        val tzOffsetMinutes = session.startZoneOffset?.totalSeconds?.div(60)
            ?: ZoneId.systemDefault().rules.getOffset(session.startTime).totalSeconds / 60

        return ActivityDto(
            startedAt = session.startTime.toString(),
            tzOffsetMinutes = tzOffsetMinutes,
            durationSec = durationSec,
            distanceM = distanceM,
            avgHrBpm = avgHrBpm,
            maxHrBpm = maxHrBpm,
            elevationGainM = null, // Health Connect exercise sessions don't expose elevation gain per session in this SDK version
            source = "health_connect",
            externalId = session.metadata.id,
        )
    }
}
