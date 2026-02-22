package com.sysadmindoc.alarmclock.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sysadmindoc.alarmclock.ui.alarmedit.AlarmEditScreen
import com.sysadmindoc.alarmclock.ui.alarmlist.AlarmListScreen
import com.sysadmindoc.alarmclock.ui.bedtime.BedtimeScreen
import com.sysadmindoc.alarmclock.ui.dashboard.DashboardScreen
import com.sysadmindoc.alarmclock.ui.onboarding.OnboardingScreen
import com.sysadmindoc.alarmclock.ui.stats.StatsScreen
import com.sysadmindoc.alarmclock.ui.settings.SettingsScreen
import com.sysadmindoc.alarmclock.ui.stopwatch.StopwatchScreen
import com.sysadmindoc.alarmclock.ui.theme.*
import com.sysadmindoc.alarmclock.ui.timer.TimerScreen

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object AlarmList : Screen("alarm_list")
    data object AlarmEdit : Screen("alarm_edit/{alarmId}") {
        fun createRoute(alarmId: Long) = "alarm_edit/$alarmId"
    }
    data object Timer : Screen("timer")
    data object Stopwatch : Screen("stopwatch")
    data object Settings : Screen("settings")
    data object Bedtime : Screen("bedtime")
    data object Stats : Screen("stats")
    data object Onboarding : Screen("onboarding")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "My Day", Icons.Default.WbSunny),
    BottomNavItem(Screen.AlarmList, "Alarm", Icons.Default.Alarm),
    BottomNavItem(Screen.Bedtime, "Bedtime", Icons.Default.Bedtime),
    BottomNavItem(Screen.Timer, "Timer", Icons.Default.Timer),
    BottomNavItem(Screen.Settings, "Settings", Icons.Default.Settings),
)

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // First-launch check
    val prefs = remember { context.getSharedPreferences("app_prefs", 0) }
    val hasCompletedOnboarding = remember { prefs.getBoolean("onboarding_complete", false) }
    val startDest = if (hasCompletedOnboarding) Screen.AlarmList.route else Screen.Onboarding.route

    val showBottomBar = currentDestination?.route?.let { route ->
        route in bottomNavItems.map { it.screen.route }
    } ?: false

    Scaffold(
        containerColor = SurfaceDark,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = SurfaceMedium,
                    contentColor = TextPrimary
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) AccentBlue else TextMuted
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    color = if (selected) AccentBlue else TextMuted
                                )
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = AccentBlue.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        prefs.edit().putBoolean("onboarding_complete", true).apply()
                        navController.navigate(Screen.AlarmList.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }

            composable(Screen.AlarmList.route) {
                AlarmListScreen(
                    onAddAlarm = { navController.navigate(Screen.AlarmEdit.createRoute(-1)) },
                    onEditAlarm = { id -> navController.navigate(Screen.AlarmEdit.createRoute(id)) },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(
                route = Screen.AlarmEdit.route,
                arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
            ) {
                AlarmEditScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Timer.route) {
                TimerScreen()
            }

            composable(Screen.Bedtime.route) {
                BedtimeScreen()
            }

            composable(Screen.Stopwatch.route) {
                StopwatchScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToStats = {
                        navController.navigate(Screen.Stats.route)
                    },
                    onNavigateToStopwatch = {
                        navController.navigate(Screen.Stopwatch.route)
                    }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen()
            }
        }
    }
}
