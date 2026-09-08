package com.healthycoach.ui.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.healthycoach.data.SyncResult
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneId

private val DISPLAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")
    .withZone(ZoneId.systemDefault())

/**
 * Shows last sync time, imported/duplicate/rejected counts, and any error. Standalone
 * Composable — Phase 05 wires it into TodayScreen.
 */
@Composable
fun SyncStatusCard(state: SyncResult, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (state) {
                is SyncResult.Idle -> Text("Not synced yet")
                is SyncResult.Syncing -> {
                    Text("Syncing…")
                    CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                }
                is SyncResult.Success -> {
                    Text("Last synced: ${DISPLAY_FMT.format(state.lastSyncAt)}")
                    Text("Imported: ${state.imported} · Duplicate: ${state.duplicate} · Rejected: ${state.rejected}")
                }
                is SyncResult.Error -> {
                    Text("Sync failed")
                    Text(state.message)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncStatusCardSuccessPreview() {
    SyncStatusCard(
        state = SyncResult.Success(imported = 2, duplicate = 1, rejected = 0, lastSyncAt = Instant.now()),
    )
}

@Preview(showBackground = true)
@Composable
private fun SyncStatusCardErrorPreview() {
    SyncStatusCard(state = SyncResult.Error("Network unreachable"))
}
