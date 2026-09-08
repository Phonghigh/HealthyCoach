package com.healthycoach.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.healthycoach.ui.checkin.CheckinScreen
import com.healthycoach.ui.plan.PlanScreen
import com.healthycoach.ui.today.TodayScreen

private sealed class BottomTab(val route: String, val label: String) {
    data object Today : BottomTab("today", "Today")
    data object Plan : BottomTab("plan", "Plan")
    data object Checkin : BottomTab("checkin", "Check-in")
}

private val bottomTabs = listOf(BottomTab.Today, BottomTab.Plan, BottomTab.Checkin)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomTabs.forEach { tab ->
                    val icon = when (tab) {
                        BottomTab.Today -> Icons.Filled.Home
                        BottomTab.Plan -> Icons.Filled.DateRange
                        BottomTab.Checkin -> Icons.Filled.CheckCircle
                    }
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.Today.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
        ) {
            composable(BottomTab.Today.route) { TodayScreen() }
            composable(BottomTab.Plan.route) { PlanScreen() }
            composable(BottomTab.Checkin.route) { CheckinScreen() }
        }
    }
}
