package com.healthycoach.data.dto

import kotlinx.serialization.Serializable

/**
 * Normalized activity shape sent to the backend. Matches backend's ActivityImportDto
 * (backend/src/activities/dto/activity-import.dto.ts) — keep fields in sync with that file.
 * Missing data (e.g. no HR stream) must be sent as null, never inferred/imputed — see
 * docs/system-architecture.md §2.
 */
@Serializable
data class ActivityDto(
    /** ISO-8601 instant, UTC, e.g. "2026-09-07T05:12:00Z". */
    val startedAt: String,
    /** Athlete's local UTC offset at the time of the activity, in minutes (e.g. +420 for ICT). */
    val tzOffsetMinutes: Int,
    val durationSec: Long,
    val distanceM: Double,
    val avgHrBpm: Int? = null,
    val maxHrBpm: Int? = null,
    val elevationGainM: Double? = null,
    val source: String,
    val externalId: String,
)

/** Per-item outcome for a batch import, mirrors backend's response contract. */
@Serializable
data class ActivityImportResultItem(
    val externalId: String,
    val status: String, // "created" | "duplicate" | "rejected"
    val reason: String? = null,
)

@Serializable
data class ActivityImportResponse(
    val results: List<ActivityImportResultItem>,
)

@Serializable
data class ActivityImportRequest(
    val activities: List<ActivityDto>,
)
