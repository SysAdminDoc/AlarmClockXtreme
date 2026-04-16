package com.sysadmindoc.alarmclock.ui.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.*

/**
 * Tracks which optional permissions have been granted.
 */
data class PermissionState(
    val hasNotifications: Boolean = false,
    val hasCalendar: Boolean = false,
    val hasLocation: Boolean = false
)

/**
 * Checks current permission states.
 */
@Composable
fun rememberPermissionState(): PermissionState {
    val context = LocalContext.current
    return remember {
        PermissionState(
            hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            } else true,
            hasCalendar = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                    PackageManager.PERMISSION_GRANTED,
            hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
}

/**
 * Permission request card displayed in settings or onboarding.
 * Shows which permissions are missing and allows one-tap request.
 */
@Composable
fun PermissionRequestCard(
    onPermissionsGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    var permState by remember { mutableStateOf(checkPermissions(context)) }
    val totalCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 3 else 2

    // Build the list of permissions to request
    val permissionsToRequest = remember(permState) {
        buildList {
            if (!permState.hasNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (!permState.hasCalendar) {
                add(Manifest.permission.READ_CALENDAR)
            }
            if (!permState.hasLocation) {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permState = checkPermissions(context)
        if (permState.hasNotifications && permState.hasCalendar && permState.hasLocation) {
            onPermissionsGranted()
        }
    }

    // Don't show if all granted
    if (permState.hasNotifications && permState.hasCalendar && permState.hasLocation) return

    val missingCount = listOf(
        permState.hasNotifications,
        permState.hasCalendar,
        permState.hasLocation
    ).count { !it }
    val grantedCount = totalCount - missingCount

    AppSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        highlighted = true
    ) {
        AppSectionTitle(
            title = "Recommended permissions",
            description = "A few optional permissions unlock weather, calendar, and clearer alarm alerts.",
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppStatusChip(
                        label = "$grantedCount of $totalCount ready",
                        icon = Icons.Default.CheckCircle,
                        color = DismissGreen
                    )
                    AppStatusChip(
                        label = if (missingCount == 1) "Final step" else "$missingCount missing",
                        icon = Icons.Default.Security,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        if (!permState.hasNotifications) {
            PermissionItem(
                icon = Icons.Default.NotificationsActive,
                title = "Notifications",
                description = "Show active alarms, timer finish alerts, and wake-up reminders."
            )
        }
        if (!permState.hasCalendar) {
            PermissionItem(
                icon = Icons.Default.CalendarMonth,
                title = "Calendar",
                description = "Bring today’s events into the dashboard and morning briefing."
            )
        }
        if (!permState.hasLocation) {
            PermissionItem(
                icon = Icons.Default.LocationOn,
                title = "Location",
                description = "Show local weather without asking you to set a city every time."
            )
        }

        Button(
            onClick = {
                if (permissionsToRequest.isNotEmpty()) {
                    launcher.launch(permissionsToRequest)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (missingCount == 1) {
                    "Enable final permission"
                } else {
                    "Enable $missingCount permissions"
                }
            )
        }

        Text(
            text = "Android will still ask you to confirm each request, and you can change them later in system settings.",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PermissionItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(description, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        }
        AppStatusChip(
            label = "Missing",
            color = SnoozeYellow
        )
    }
}

private fun checkPermissions(context: android.content.Context): PermissionState {
    return PermissionState(
        hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else true,
        hasCalendar = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED,
        hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    )
}
