package com.healthycoach.health

import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord

/**
 * Read-only Health Connect permissions this app requests. Same set verified working in the
 * Phase 01 probe (see plans/reports/phase-01-health-connect-verification-result.md).
 */
object HealthConnectPermissions {
    val REQUIRED: Set<String> = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SpeedRecord::class),
    )

    /** Activity result contract for requesting the permission set above. */
    val requestPermissionsContract = PermissionController.createRequestPermissionResultContract()
}
