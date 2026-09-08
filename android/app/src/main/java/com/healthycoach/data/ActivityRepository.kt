package com.healthycoach.data

import com.healthycoach.data.dto.ActivityImportRequest
import com.healthycoach.health.ActivitySource
import java.time.Instant

/** Outcome of a single sync pass, exposed to the UI via SyncViewModel's StateFlow. */
sealed class SyncResult {
    object Idle : SyncResult()
    object Syncing : SyncResult()
    data class Success(
        val imported: Int,
        val duplicate: Int,
        val rejected: Int,
        val lastSyncAt: Instant,
    ) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

/**
 * Pulls new activities from the device's ActivitySource (Health Connect for now) and pushes
 * them to the backend as a batch. Backend owns dedup via the (source, externalId) unique
 * constraint — this class never tries to dedup client-side.
 */
class ActivityRepository(
    private val source: ActivitySource,
    private val api: HealthCoachApi = HealthCoachApi.instance,
) {
    suspend fun sync(): SyncResult {
        return try {
            val activities = source.fetchNewActivities()
            if (activities.isEmpty()) {
                return SyncResult.Success(imported = 0, duplicate = 0, rejected = 0, lastSyncAt = Instant.now())
            }
            val response = api.importActivities(ActivityImportRequest(activities))
            val imported = response.results.count { it.status == "created" }
            val duplicate = response.results.count { it.status == "duplicate" }
            val rejected = response.results.count { it.status == "rejected" }
            SyncResult.Success(imported, duplicate, rejected, lastSyncAt = Instant.now())
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Unknown sync error")
        }
    }
}
