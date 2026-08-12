package com.sysadmindoc.alarmclock.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.ui.alarmedit.AlarmEditScreen
import com.sysadmindoc.alarmclock.ui.alarmlist.AlarmListScreen
import com.sysadmindoc.alarmclock.ui.bedtime.BedtimeScreen
import com.sysadmindoc.alarmclock.ui.components.BottomNavContainer
import com.sysadmindoc.alarmclock.ui.dashboard.DashboardScreen
import com.sysadmindoc.alarmclock.ui.onboarding.OnboardingScreen
import com.sysadmindoc.alarmclock.ui.stats.StatsScreen
import com.sysadmindoc.alarmclock.ui.settings.SettingsScreen
import com.sysadmindoc.alarmclock.ui.share.SharedAlarmImportScreen
import com.sysadmindoc.alarmclock.ui.stopwatch.StopwatchScreen
import com.sysadmindoc.alarmclock.ui.theme.*
import com.sysadmindoc.alarmclock.ui.timer.TimerScreen
import com.sysadmindoc.alarmclock.ui.worldclock.WorldClockScreen
import com.sysadmindoc.alarmclock.ui.news.NewsScreen
import com.sysadmindoc.alarmclock.util.ReliabilityDoctor

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
    data object WorldClock : Screen("world_clock")
    data object SharedAlarmImport : Screen("shared_alarm_import")
    // v1.8.0
    data object News : Screen("news")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

/**
 * The full list of bottom-nav destinations. The visible subset is computed by
 * [visibleBottomNavItems] based on the user's preferences — Alarms and
 * Settings are always visible; Weather / Timer / World / News can be hidden.
 *
 * v1.8.0: renamed Dashboard's label "Today" → "Weather" since the screen is
 * essentially a weather hub now (centered conditions, hourly, 3-day, sunrise/
 * sunset, UV, plus the new Windy radar embed). Calendar still lives there
 * but is below the fold and toggleable. Added News as a sibling tab.
 */
val bottomNavItems = listOf(
    // v1.8.1: short tab labels — at 6 tabs the M3 indicator pill is 60dp
    // wide and "Weather" got truncated to "Weathe" inside it. The screen
    // hero still reads "Weather", so we shorten just the *nav label* to
    // "Today" (which is also accurate — it's a daily-overview hub with
    // weather + radar + calendar). "Settings" fits at default fontWeight.
    BottomNavItem(Screen.Dashboard, "Today", Icons.Default.WbSunny),
    BottomNavItem(Screen.AlarmList, "Alarms", Icons.Default.Alarm),
    BottomNavItem(Screen.Timer, "Timer", Icons.Default.Timer),
    BottomNavItem(Screen.WorldClock, "World", Icons.Default.Language),
    BottomNavItem(Screen.News, "News", Icons.AutoMirrored.Filled.Article),
    BottomNavItem(Screen.Settings, "Settings", Icons.Default.Settings),
)

private fun visibleBottomNavItems(
    showDashboard: Boolean,
    showTimer: Boolean,
    showWorld: Boolean,
    showNews: Boolean,
): List<BottomNavItem> = bottomNavItems.filter {
    when (it.screen) {
        Screen.Dashboard -> showDashboard
        Screen.Timer -> showTimer
        Screen.WorldClock -> showWorld
        Screen.News -> showNews
        else -> true
    }
}

private const val ONBOARDING_VERSION = 1
private const val ONBOARDING_DONE_KEY = "onboarding_complete_v$ONBOARDING_VERSION"

@OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    sharedAlarmDraft: Alarm? = null,
    onSharedAlarmConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // v1.13.0 (roadmap N11): Adaptive primary navigation. Compose's stable
    // WindowSizeClass API ([android-developers.googleblog.com/...](https://android-developers.googleblog.com/2024/09/jetpack-compose-apis-for-building-adaptive-layouts-material-guidance-now-stable.html))
    // lets us swap the persistent bottom NavigationBar for a NavigationRail
    // on `MEDIUM` and `EXPANDED` widths — i.e., 8" tablets, foldables in
    // book/tabletop posture, Chromebooks, and Samsung DeX. On COMPACT
    // (every standard phone in portrait) we keep the existing bottom bar
    // verbatim so phone users see zero change.
    val windowWidth = (context as? ComponentActivity)?.let { activity ->
        calculateWindowSizeClass(activity).widthSizeClass
    } ?: WindowWidthSizeClass.Compact
    val useNavigationRail = windowWidth != WindowWidthSizeClass.Compact

    // First-launch check — suffix the key so a future onboarding redesign
    // can re-show the flow to existing users by bumping the version.
    val prefs = remember { context.getSharedPreferences("app_prefs", 0) }
    val hasCompletedOnboarding = remember { prefs.getBoolean(ONBOARDING_DONE_KEY, false) }
    val reliabilityChecklistDue = remember { ReliabilityDoctor.isChecklistDue(context) }
    val startDest = if (hasCompletedOnboarding && !reliabilityChecklistDue) {
        Screen.AlarmList.route
    } else {
        Screen.Onboarding.route
    }

    // v1.7.1: User-controlled tab visibility. We grab the cached snapshot
    // straight from PreferencesManager via Hilt so the nav bar updates the
    // moment a toggle flips, without forcing every screen to read from
    // DataStore directly.
    val preferencesManager = remember(context) {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            AppNavigationEntryPoint::class.java
        ).preferencesManager()
    }
    val settings by preferencesManager.settings.collectAsStateWithLifecycle(
        initialValue = preferencesManager.getCachedSettings()
    )
    val visibleTabs = visibleBottomNavItems(
        showDashboard = settings.showDashboardTab,
        showTimer = settings.showTimerTab,
        showWorld = settings.showWorldClockTab,
        showNews = settings.showNewsTab,
    )

    val showBottomBar = currentDestination?.route?.let { route ->
        route in bottomNavItems.map { it.screen.route }
    } ?: false

    LaunchedEffect(Unit) {
        val data = (context as? android.app.Activity)?.intent?.data ?: return@LaunchedEffect
        if (data.scheme == "acx" && data.host == "navigate") {
            val targetRoute = data.pathSegments.joinToString("/")
            if (targetRoute.isNotBlank()) {
                navController.navigate(targetRoute) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
            }
        }
    }

    // If the user is currently on a tab that's been hidden, bounce them to
    // Alarms (the always-visible "home" tab). Without this they'd be stuck on
    // Today, Timer, or World with no way back via the bottom nav.
    LaunchedEffect(visibleTabs, currentDestination?.route) {
        val current = currentDestination?.route ?: return@LaunchedEffect
        val isManagedTab = current in bottomNavItems.map { it.screen.route }
        if (isManagedTab && visibleTabs.none { it.screen.route == current }) {
            navController.navigate(Screen.AlarmList.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                launchSingleTop = true
            }
        }
    }

    val onTabClick: (Screen) -> Unit = { screen ->
        navController.navigate(screen.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = SurfaceDark,
        bottomBar = {
            // v1.13.0 (roadmap N11): the bottom bar only renders on COMPACT.
            // Wider windows get a NavigationRail instead — see the Row branch
            // in the content slot below.
            if (showBottomBar && !useNavigationRail) {
                BottomNavContainer {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        contentColor = TextPrimary,
                        tonalElevation = 0.dp
                    ) {
                        visibleTabs.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true

                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    // v1.8.1: with 6 tabs in 1080px, showing
                                    // every label forced ellipsis ("Weath…"/
                                    // "Setti…") which read as broken layout.
                                    // The Material 3 idiom for crowded bars
                                    // is `alwaysShowLabel = false`: only the
                                    // selected tab carries its label, others
                                    // sit as confident icons. We use Material
                                    // 3's default `labelMedium` (no fontWeight
                                    // override) so the label has the most
                                    // breathing room inside the pill.
                                    Text(
                                        text = item.label,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                selected = selected,
                                alwaysShowLabel = true,
                                onClick = { onTabClick(item.screen) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color.Transparent,
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        // v1.13.0 (roadmap N11): On MEDIUM/EXPANDED width classes, place a
        // NavigationRail on the leading edge and let the NavHost fill the
        // rest of the row. The bottom bar is hidden in that case (see slot
        // above). Compact widths skip the rail entirely and the original
        // NavHost-only layout is preserved.
        if (useNavigationRail && showBottomBar) {
            Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                NavigationRail(
                    containerColor = SurfaceDark,
                    contentColor = TextPrimary
                ) {
                    visibleTabs.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true
                        NavigationRailItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text(text = item.label, maxLines = 1) },
                            selected = selected,
                            alwaysShowLabel = true,
                            onClick = { onTabClick(item.screen) },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(
                        navController = navController,
                        startDest = startDest,
                        prefs = prefs,
                        openReadinessChecklist = reliabilityChecklistDue,
                        sharedAlarmDraft = sharedAlarmDraft,
                        onSharedAlarmConsumed = onSharedAlarmConsumed
                    )
                }
            }
            return@Scaffold
        }

        AppNavHost(
        navController = navController,
        startDest = startDest,
        prefs = prefs,
        openReadinessChecklist = reliabilityChecklistDue,
        sharedAlarmDraft = sharedAlarmDraft,
        onSharedAlarmConsumed = onSharedAlarmConsumed,
        modifier = Modifier.padding(padding)
    )
}
}

/**
 * v1.13.0 (roadmap N11): extracted from the inline NavHost so the rail
 * branch (MEDIUM/EXPANDED) and the bar branch (COMPACT) can both render
 * the same navigation graph without duplicating ~100 LoC.
 *
 * `prefs` is plumbed through because onboarding completion is persisted
 * via SharedPreferences here (not a ViewModel).
 */
@Composable
private fun AppNavHost(
    navController: NavHostController,
    startDest: String,
    prefs: android.content.SharedPreferences,
    openReadinessChecklist: Boolean,
    sharedAlarmDraft: Alarm?,
    onSharedAlarmConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(sharedAlarmDraft) {
        if (sharedAlarmDraft != null) {
            navController.navigate(Screen.SharedAlarmImport.route) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it / 4 }) + fadeIn()
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 6 }) + fadeOut(targetAlpha = 0.72f)
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it / 4 }) + fadeOut(targetAlpha = 0.72f)
        }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                openReadinessChecklist = openReadinessChecklist,
                onComplete = {
                    ReliabilityDoctor.markChecklistReviewed(context)
                    prefs.edit().putBoolean(ONBOARDING_DONE_KEY, true).apply()
                    navController.navigate(Screen.AlarmList.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onOpenAlarms = {
                    navController.navigate(Screen.AlarmList.route) {
                        launchSingleTop = true
                    }
                }
            )
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

        composable(Screen.SharedAlarmImport.route) {
            val draft = sharedAlarmDraft
            if (draft == null) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            } else {
                SharedAlarmImportScreen(
                    alarm = draft,
                    onCancel = {
                        onSharedAlarmConsumed()
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.AlarmList.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = false
                                }
                                launchSingleTop = true
                            }
                        }
                    },
                    onSaved = { savedId ->
                        onSharedAlarmConsumed()
                        navController.navigate(Screen.AlarmEdit.createRoute(savedId)) {
                            popUpTo(Screen.SharedAlarmImport.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(Screen.Timer.route) {
            TimerScreen(
                onOpenStopwatch = { navController.navigate(Screen.Stopwatch.route) }
            )
        }

        composable(Screen.Bedtime.route) {
            BedtimeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Stopwatch.route) {
            StopwatchScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.WorldClock.route) {
            WorldClockScreen()
        }

        composable(Screen.News.route) {
            NewsScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                },
                onNavigateToStopwatch = {
                    navController.navigate(Screen.Stopwatch.route)
                },
                onNavigateToBedtime = {
                    navController.navigate(Screen.Bedtime.route)
                },
                onOpenOnboardingChecklist = {
                    navController.navigate(Screen.Onboarding.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

/** Hilt entry point so the (non-VM) navigation composable can read settings. */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
internal interface AppNavigationEntryPoint {
    fun preferencesManager(): com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
}
