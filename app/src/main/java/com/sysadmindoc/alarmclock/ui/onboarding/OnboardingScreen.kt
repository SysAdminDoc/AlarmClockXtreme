package com.sysadmindoc.alarmclock.ui.onboarding

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.AccentBlue
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.SurfaceLight
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import com.sysadmindoc.alarmclock.util.ManufacturerCompat
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val accentColor: Color,
    val highlights: List<String>
)

private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Default.Alarm,
        title = "Reliable wake-ups",
        description = "Alarm Clock Xtreme is built to stay dependable even when Android is trying to save power in the background.",
        accentColor = AccentBlue,
        highlights = listOf(
            "Exact alarms designed to survive Doze mode",
            "Battery guidance for aggressive device vendors"
        )
    ),
    OnboardingPage(
        icon = Icons.Default.Psychology,
        title = "Wake up for real",
        description = "Challenge-based dismiss flows help make sure you are actually awake before the alarm stops.",
        accentColor = SnoozeYellow,
        highlights = listOf(
            "Math, shake, memory, steps, barcode, and more",
            "Layer multiple challenges when you need extra certainty"
        )
    ),
    OnboardingPage(
        icon = Icons.Default.WbSunny,
        title = "Start the day informed",
        description = "See weather, calendar, and your next alarm in one place so the morning feels calmer from the first glance.",
        accentColor = DismissGreen,
        highlights = listOf(
            "A quick daily dashboard with forecast and events",
            "Useful context without ads or noisy clutter"
        )
    ),
    OnboardingPage(
        icon = Icons.Default.Shield,
        title = "Private by default",
        description = "No ads. No tracking. No account.",
        accentColor = AccentRed,
        highlights = listOf(
            "Permissions are optional and can be changed later",
            "Data stays on your device unless you configure an integration"
        )
    )
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex
    var readiness by remember { mutableStateOf(OnboardingReadiness.from(context)) }
    var testAlarmStatus by remember { mutableStateOf("") }
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        readiness = OnboardingReadiness.from(context)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val updatedReadiness = OnboardingReadiness.from(context)
                if (updatedReadiness.testAlarmReady && !readiness.testAlarmReady) {
                    testAlarmStatus = "Test alarm completed. Your device opened the alarm screen successfully."
                }
                readiness = updatedReadiness
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SurfaceLight,
                        SurfaceDark
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            onboardingPages[pagerState.currentPage].accentColor.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppStatusChip(
                        label = "Welcome",
                        icon = Icons.Default.Alarm,
                        color = onboardingPages[pagerState.currentPage].accentColor
                    )
                    Text(
                        text = "Alarm Clock Xtreme",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Dependable mornings with less friction",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (!isLastPage) {
                    TextButton(onClick = onComplete) {
                        Text("Set up later", color = TextMuted)
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(page = onboardingPages[page], isLastPage = page == onboardingPages.lastIndex)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(onboardingPages.size) { index ->
                        val isActive = index == pagerState.currentPage
                        val dotWidth by animateDpAsState(
                            targetValue = if (isActive) 20.dp else 8.dp,
                            animationSpec = tween(durationMillis = 250),
                            label = "dotWidth$index"
                        )
                        Box(
                            modifier = Modifier
                                .size(dotWidth, 8.dp)
                                .clip(RoundedCornerShape(if (isActive) 0.dp else 4.dp))
                                .background(
                                    if (isActive) onboardingPages[pagerState.currentPage].accentColor
                                    else onboardingPages[pagerState.currentPage].accentColor.copy(alpha = 0.22f)
                                )
                        )
                    }
                }

                AppStatusChip(
                    label = onboardingPages[pagerState.currentPage].title,
                    icon = onboardingPages[pagerState.currentPage].icon,
                    color = onboardingPages[pagerState.currentPage].accentColor
                )

                if (isLastPage) {
                    AppSurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        highlighted = true,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppStatusChip(
                                label = "Recommended setup",
                                icon = Icons.Default.Shield,
                                color = onboardingPages[pagerState.currentPage].accentColor
                            )
                            AppStatusChip(
                                label = "Optional",
                                color = TextMuted
                            )
                        }

                        ReadinessMiniRow(
                            icon = Icons.Default.Alarm,
                            title = "Exact alarm access",
                            ready = readiness.exactAlarmReady,
                            onAction = { context.openExactAlarmSettings() }
                        )
                        ReadinessMiniRow(
                            icon = Icons.Default.NotificationsActive,
                            title = "Alarm notifications",
                            ready = readiness.notificationsReady,
                            onAction = {
                                val perms = buildList {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    add(Manifest.permission.READ_CALENDAR)
                                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }.toTypedArray()
                                if (perms.isNotEmpty()) {
                                    permissionLauncher.launch(perms)
                                }
                            }
                        )
                        if (readiness.fullScreenRelevant) {
                            ReadinessMiniRow(
                                icon = Icons.Default.NotificationsActive,
                                title = "Full-screen alarm access",
                                ready = readiness.fullScreenReady == true,
                                onAction = { context.openFullScreenAlarmSettings() }
                            )
                        }
                        ReadinessMiniRow(
                            icon = Icons.Default.BatteryAlert,
                            title = "Battery protection",
                            ready = readiness.batteryReady,
                            onAction = { ManufacturerCompat.requestBatteryOptimizationExemption(context) }
                        )
                        ReadinessMiniRow(
                            icon = Icons.Default.Alarm,
                            title = "Test alarm",
                            ready = readiness.testAlarmReady,
                            actionLabel = "Run",
                            onAction = {
                                OnboardingTestAlarm.schedule(context).fold(
                                    onSuccess = {
                                        testAlarmStatus = "Test alarm scheduled. It will ring in 10 seconds."
                                        readiness = OnboardingReadiness.from(context)
                                    },
                                    onFailure = { error ->
                                        testAlarmStatus = error.message ?: "Could not schedule the test alarm."
                                    }
                                )
                            }
                        )
                        Text(
                            text = testAlarmStatus.ifBlank {
                                "Set these before relying on an overnight alarm. Calendar and weather permissions are optional and can be changed later."
                            },
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = {
                        if (isLastPage) {
                            val perms = buildList {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                add(Manifest.permission.READ_CALENDAR)
                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }.toTypedArray()
                            if (perms.isNotEmpty()) {
                                permissionLauncher.launch(perms)
                            } else {
                                onComplete()
                            }
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = onboardingPages[pagerState.currentPage].accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isLastPage) "Enable alerts, calendar, and weather" else "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (isLastPage) {
                    TextButton(onClick = onComplete) {
                        Text("Continue without permissions", color = TextMuted)
                    }
                }

                Text(
                    text = if (isLastPage) {
                        "Use each Review action for platform settings that Android cannot grant inside the app."
                    } else {
                        "Step ${pagerState.currentPage + 1} of ${onboardingPages.size}"
                    },
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    isLastPage: Boolean
) {
    val compact = isLastPage
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            highlighted = true,
            contentPadding = PaddingValues(if (compact) 14.dp else 18.dp)
        ) {
            if (compact) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OnboardingIconTile(
                        page = page,
                        size = 72.dp,
                        iconSize = 36.dp
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = page.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Finish with the reliability switches that matter before an overnight alarm.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                OnboardingIconTile(
                    page = page,
                    size = 112.dp,
                    iconSize = 52.dp
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (!compact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    page.highlights.forEach { highlight ->
                        FeatureRow(
                            text = highlight,
                            accent = page.accentColor
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun OnboardingIconTile(
    page: OnboardingPage,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        page.accentColor.copy(alpha = 0.28f),
                        page.accentColor.copy(alpha = 0.06f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = page.title,
            tint = page.accentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun FeatureRow(text: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accent)
        )
        Text(
            text = text,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReadinessMiniRow(
    icon: ImageVector,
    title: String,
    ready: Boolean,
    actionLabel: String = "Review",
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (ready) DismissGreen else SnoozeYellow,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAction, enabled = !ready) {
            Text(if (ready) "Ready" else actionLabel)
        }
    }
}

private data class OnboardingReadiness(
    val exactAlarmReady: Boolean,
    val notificationsReady: Boolean,
    val fullScreenRelevant: Boolean,
    val fullScreenReady: Boolean?,
    val batteryReady: Boolean,
    val testAlarmReady: Boolean
) {
    companion object {
        fun from(context: Context): OnboardingReadiness {
            val exact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
            } else {
                true
            }
            val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            val fullScreenRelevant = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            val fullScreen = if (fullScreenRelevant) {
                runCatching {
                    context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent()
                }.getOrNull()
            } else {
                null
            }
            return OnboardingReadiness(
                exactAlarmReady = exact,
                notificationsReady = notifications,
                fullScreenRelevant = fullScreenRelevant,
                fullScreenReady = fullScreen,
                batteryReady = ManufacturerCompat.isIgnoringBatteryOptimizations(context),
                testAlarmReady = OnboardingTestAlarm.isCompleted(context)
            )
        }
    }
}

private fun Context.openExactAlarmSettings() {
    val primary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:$packageName")
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
    }
    startActivityWithFallback(primary)
}

private fun Context.openFullScreenAlarmSettings() {
    val primary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = Uri.parse("package:$packageName")
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
    }
    startActivityWithFallback(primary)
}

private fun Context.startActivityWithFallback(primary: Intent) {
    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
    }
    runCatching { startActivity(primary.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
