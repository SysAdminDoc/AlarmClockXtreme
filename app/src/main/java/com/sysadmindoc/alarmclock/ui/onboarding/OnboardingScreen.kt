package com.sysadmindoc.alarmclock.ui.onboarding

import android.Manifest
import android.os.Build
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onComplete()
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PermissionChip(Icons.Default.NotificationsActive, "Alerts")
                            PermissionChip(Icons.Default.CalendarMonth, "Calendar")
                            PermissionChip(Icons.Default.LocationOn, "Weather")
                        }
                        Text(
                            text = "Recommended for weather, calendar, and dependable alerts. Optional, and easy to change later.",
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
                        text = if (isLastPage) "Enable recommended permissions" else "Continue",
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
                        "The app still works without them, and every permission can be revisited later from Settings."
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
            Box(
                modifier = Modifier
                    .size(if (compact) 76.dp else 112.dp)
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
                    modifier = Modifier.size(if (compact) 38.dp else 52.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = page.title,
                    style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = page.description,
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
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
                .clip(RoundedCornerShape(2.dp))
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
private fun PermissionChip(icon: ImageVector, label: String) {
    AppStatusChip(
        label = label,
        icon = icon,
        color = MaterialTheme.colorScheme.primary
    )
}
