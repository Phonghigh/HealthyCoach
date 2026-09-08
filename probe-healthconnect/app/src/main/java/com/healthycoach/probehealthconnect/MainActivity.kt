package com.healthycoach.probehealthconnect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController

/**
 * Single-Activity throwaway probe. No navigation, no DI. Its only job: prove (or disprove)
 * that Garmin Connect writes exercise data into Health Connect. See phase-01 plan doc.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: ProbeViewModel by viewModels()

    private val requestPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.onPermissionsResult(granted, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val status = viewModel.checkAvailability(this)
        if (status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            openPlayStoreForHealthConnect()
        } else if (status == HealthConnectClient.SDK_AVAILABLE) {
            requestPermissions.launch(REQUIRED_PERMISSIONS)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by viewModel.state.collectAsState()
                    ProbeScreen(
                        state = state,
                        onRequestPermissions = { requestPermissions.launch(REQUIRED_PERMISSIONS) },
                        onOpenPlayStore = { openPlayStoreForHealthConnect() },
                    )
                }
            }
        }
    }

    private fun openPlayStoreForHealthConnect() {
        val uri = Uri.parse(
            "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"
        )
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            putExtra("overlay", true)
            putExtra("callerId", packageName)
        }
        startActivity(intent)
    }
}

@Composable
fun ProbeScreen(
    state: ProbeUiState,
    onRequestPermissions: () -> Unit,
    onOpenPlayStore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Health Connect Probe (throwaway)", style = MaterialTheme.typography.titleLarge)

        when (state) {
            is ProbeUiState.Idle, is ProbeUiState.CheckingAvailability ->
                Text("Checking Health Connect availability...")

            is ProbeUiState.NeedsProviderUpdate -> {
                Text("Health Connect provider needs installing/updating.")
                Button(onClick = onOpenPlayStore) { Text("Open Play Store") }
            }

            is ProbeUiState.NeedsPermissions -> {
                Text("Health Connect permissions required.")
                Button(onClick = onRequestPermissions) { Text("Grant permissions") }
            }

            is ProbeUiState.Loading -> Text("Loading last 60 days of exercise sessions...")

            is ProbeUiState.Error -> Text("Error: ${state.message}")

            is ProbeUiState.Loaded -> {
                Text("${state.sessions.size} exercise session(s) found")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.sessions) { session -> SessionCard(session) }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: SessionResult) {
    Card(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${session.startText} -> ${session.endText} (${session.durationText})")
            Text("Type: ${session.exerciseType}  |  Origin: ${session.originPackage}")
            Text(
                "Distance: ${session.distanceMeters?.let { "%.0f m".format(it) } ?: "n/a"}  |  " +
                    "HR avg/max: ${session.avgHeartRate ?: "n/a"}/${session.maxHeartRate ?: "n/a"}"
            )
            Text("HR samples: ${session.heartRateSampleCount}  |  Speed samples: ${session.speedSampleCount}")
        }
    }
}
