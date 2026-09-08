package com.healthycoach.probehealthconnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TAG = "HealthConnectProbe"
private val DISPLAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())

/** One-screen result for a single exercise session. Throwaway data class, no persistence. */
data class SessionResult(
    val start: Instant,
    val end: Instant,
    val exerciseType: Int,
    val originPackage: String,
    val distanceMeters: Double?,
    val avgHeartRate: Long?,
    val maxHeartRate: Long?,
    val heartRateSampleCount: Int,
    val speedSampleCount: Int,
) {
    val durationText: String
        get() {
            val d = Duration.between(start, end)
            val h = d.toHours()
            val m = d.toMinutesPart()
            val s = d.toSecondsPart()
            return if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s"
        }

    val startText: String get() = DISPLAY_FMT.format(start)
    val endText: String get() = DISPLAY_FMT.format(end)

    fun toJson(): JSONObject = JSONObject().apply {
        put("start", start.toString())
        put("end", end.toString())
        put("exerciseType", exerciseType)
        put("originPackage", originPackage)
        put("distanceMeters", distanceMeters ?: JSONObject.NULL)
        put("avgHeartRate", avgHeartRate ?: JSONObject.NULL)
        put("maxHeartRate", maxHeartRate ?: JSONObject.NULL)
        put("heartRateSampleCount", heartRateSampleCount)
        put("speedSampleCount", speedSampleCount)
    }
}

sealed class ProbeUiState {
    object Idle : ProbeUiState()
    object CheckingAvailability : ProbeUiState()
    object NeedsProviderUpdate : ProbeUiState()
    object NeedsPermissions : ProbeUiState()
    object Loading : ProbeUiState()
    data class Loaded(val sessions: List<SessionResult>) : ProbeUiState()
    data class Error(val message: String) : ProbeUiState()
}

/** All Health Connect permissions this probe requests. */
val REQUIRED_PERMISSIONS: Set<String> = setOf(
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(DistanceRecord::class),
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(SpeedRecord::class),
)

/**
 * Single ViewModel holding probe state. No repository/DI layer per phase-01 spec — this is
 * throwaway KISS code, deleted after the Health Connect verification gate.
 */
class ProbeViewModel : ViewModel() {

    private val _state = MutableStateFlow<ProbeUiState>(ProbeUiState.Idle)
    val state: StateFlow<ProbeUiState> = _state

    fun checkAvailability(context: Context): Int {
        val status = HealthConnectClient.getSdkStatus(context)
        _state.update {
            if (status == HealthConnectClient.SDK_AVAILABLE) ProbeUiState.NeedsPermissions
            else ProbeUiState.NeedsProviderUpdate
        }
        return status
    }

    fun onPermissionsResult(granted: Set<String>, context: Context) {
        if (granted.containsAll(REQUIRED_PERMISSIONS)) {
            loadSessions(context)
        } else {
            _state.update { ProbeUiState.Error("Missing permissions: ${REQUIRED_PERMISSIONS - granted}") }
        }
    }

    fun loadSessions(context: Context) {
        _state.update { ProbeUiState.Loading }
        viewModelScope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(context)
                val end = Instant.now()
                val start = end.minus(Duration.ofDays(60))
                val range = TimeRangeFilter.between(start, end)

                val exerciseResponse = client.readRecords(
                    ReadRecordsRequest(ExerciseSessionRecord::class, range)
                )

                val results = exerciseResponse.records.map { session ->
                    val sessionRange = TimeRangeFilter.between(session.startTime, session.endTime)

                    val distanceRecords = client.readRecords(
                        ReadRecordsRequest(DistanceRecord::class, sessionRange)
                    ).records
                    val totalDistance = distanceRecords.sumOf { it.distance.inMeters }

                    val hrRecords = client.readRecords(
                        ReadRecordsRequest(HeartRateRecord::class, sessionRange)
                    ).records
                    val hrSamples = hrRecords.flatMap { it.samples }
                    val avgHr = if (hrSamples.isNotEmpty()) hrSamples.map { it.beatsPerMinute }.average().toLong() else null
                    val maxHr = hrSamples.maxOfOrNull { it.beatsPerMinute }

                    val speedRecords = client.readRecords(
                        ReadRecordsRequest(SpeedRecord::class, sessionRange)
                    ).records
                    val speedSampleCount = speedRecords.sumOf { it.samples.size }

                    SessionResult(
                        start = session.startTime,
                        end = session.endTime,
                        exerciseType = session.exerciseType,
                        originPackage = session.metadata.dataOrigin.packageName,
                        distanceMeters = if (distanceRecords.isNotEmpty()) totalDistance else null,
                        avgHeartRate = avgHr,
                        maxHeartRate = maxHr,
                        heartRateSampleCount = hrSamples.size,
                        speedSampleCount = speedSampleCount,
                    )
                }

                logResultsAsJson(results)
                _state.update { ProbeUiState.Loaded(results) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read Health Connect records", e)
                _state.update { ProbeUiState.Error(e.message ?: "Unknown error reading Health Connect") }
            }
        }
    }

    private fun logResultsAsJson(results: List<SessionResult>) {
        val array = JSONArray()
        results.forEach { array.put(it.toJson()) }
        // Deliberately logcat-only (no remote logging, no persistence) — copy from logcat per
        // phase-01 step 9. Chunk in case logcat truncates long lines.
        val json = array.toString(2)
        json.chunked(3500).forEachIndexed { i, chunk ->
            Log.d(TAG, "RESULTS_JSON[$i]: $chunk")
        }
        Log.d(TAG, "RESULTS_JSON_DONE: ${results.size} sessions")
    }
}
