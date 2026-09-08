package com.healthycoach.data

import com.healthycoach.data.dto.ActivityDto
import com.healthycoach.data.dto.ActivityImportRequest
import com.healthycoach.data.dto.ActivityImportResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Backend endpoints for activity ingestion (Phase 03). Built on top of ApiClient's shared
 * Retrofit instance — do not duplicate the OkHttp/device-key setup here.
 */
interface HealthCoachApi {
    @POST("activities/import")
    suspend fun importActivities(@Body request: ActivityImportRequest): ActivityImportResponse

    @GET("activities")
    suspend fun getActivities(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): List<ActivityDto>

    companion object {
        val instance: HealthCoachApi by lazy {
            ApiClient.retrofit.create(HealthCoachApi::class.java)
        }
    }
}
