package com.healthycoach.health

import com.healthycoach.data.dto.ActivityDto

/**
 * Abstraction over "where completed runs come from on this device". Branch A implements this
 * against Health Connect (see HealthConnectSource); a future Branch B (FIT/TCX file import)
 * would implement the same interface so ActivityRepository never needs to know which source
 * is active.
 */
interface ActivitySource {
    /**
     * Returns activities completed since the last sync. Implementations decide internally
     * whether that means a full window read (first sync) or a delta read (subsequent syncs).
     */
    suspend fun fetchNewActivities(): List<ActivityDto>
}
