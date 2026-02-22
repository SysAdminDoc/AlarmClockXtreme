package com.sysadmindoc.alarmclock.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sysadmindoc.alarmclock.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val accentColor: androidx.compose.ui.graphics.Color
)

val onboardingPages = listOf(
    OnboardingPage(
        Icons.Default.Alarm,
        "Never Miss a Wake-Up",
        "Reliable alarms that bypass Doze mode and battery restrictions. Your alarm fires when it should, every time.",
        AccentBlue
    ),
    OnboardingPage(
        Icons.Default.Psychology,
        "Wake Up for Real",
        "Dismiss challenges like math problems, phone shaking, number sequences, and memory patterns make sure you're actually awake.",
        SnoozeYellow
    ),
    OnboardingPage(
        Icons.Default.WbSunny,
        "Your Morning Dashboard",
        "See today's weather, calendar events, and next alarm at a glance. Plan your day before you even get out of bed.",
        DismissGreen
    ),
    OnboardingPage(
        Icons.Default.Shield,
        "Privacy First",
        "No ads. No tracking. No analytics. Open source and available on both Google Play and F-Droid. Your data stays on your device.",
        AccentRed
    )
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.size - 1

    // Permission launcher for last page
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { onComplete() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        // Skip button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (!isLastPage) {
                TextButton(onClick = onComplete) {
                    Text("Skip", color = TextMuted)
                }
            }
        }

        // Pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingPageContent(onboardingPages[page])
        }

        // Indicators + button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page number
            Text(
                "${pagerState.currentPage + 1} of ${onboardingPages.size}",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                repeat(onboardingPages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) AccentBlue
                                else AccentBlue.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            // Action button
            Button(
                onClick = {
                    if (isLastPage) {
                        // Request permissions on last page
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
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    when {
                        isLastPage -> "Grant Permissions & Get Started"
                        pagerState.currentPage == 0 -> "Next: Dismiss Challenges"
                        pagerState.currentPage == 1 -> "Next: Morning Dashboard"
                        else -> "Next: Privacy"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon with gradient background
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(page.accentColor.copy(alpha = 0.3f), page.accentColor.copy(alpha = 0.05f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                page.icon,
                contentDescription = page.title,
                tint = page.accentColor,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            page.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            page.description,
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}
