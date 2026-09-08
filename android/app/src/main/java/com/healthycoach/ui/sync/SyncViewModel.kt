package com.healthycoach.ui.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthycoach.data.ActivityRepository
import com.healthycoach.data.SyncResult
import com.healthycoach.health.HealthConnectSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

private val SYNC_THROTTLE = Duration.ofMinutes(15)

/**
 * Owns the sync lifecycle: manual trigger (Sync button) + automatic trigger on app foreground,
 * throttled so foreground syncs never fire more than once per SYNC_THROTTLE window. Manual
 * syncs always bypass the throttle.
 */
class SyncViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ActivityRepository(HealthConnectSource(application.applicationContext))

    private val _syncResult = MutableStateFlow<SyncResult>(SyncResult.Idle)
    val syncResult: StateFlow<SyncResult> = _syncResult

    private var lastSyncAttemptAt: Instant? = null

    /** Called from onResume/onStart. Skips if within the throttle window unless [force]. */
    fun onAppForeground(force: Boolean = false) {
        val last = lastSyncAttemptAt
        if (!force && last != null && Duration.between(last, Instant.now()) < SYNC_THROTTLE) {
            return
        }
        sync()
    }

    /** Manual "Sync" button — always bypasses the throttle. */
    fun syncNow() = onAppForeground(force = true)

    private fun sync() {
        lastSyncAttemptAt = Instant.now()
        _syncResult.value = SyncResult.Syncing
        viewModelScope.launch {
            _syncResult.value = repository.sync()
        }
    }
}
