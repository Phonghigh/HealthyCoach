package com.healthycoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import dagger.hilt.android.AndroidEntryPoint
import com.healthycoach.ui.AppNavHost
import com.healthycoach.ui.theme.HealthyCoachTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthyCoachTheme {
                Scaffold { innerPadding ->
                    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize().padding(innerPadding)) {
                        AppNavHost()
                    }
                }
            }
        }
    }
}
