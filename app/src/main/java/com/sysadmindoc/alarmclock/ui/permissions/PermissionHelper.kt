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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.*
import androidx.compose.ui.res.stringResource
import com.sysadmindoc.alarmclock.R

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
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionState by remember(context) { mutableStateOf(checkPermissions(context)) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionState = checkPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return permissionState
}

/**
 * Permission request card displayed in settings or onboarding.
 * Shows which permissions are missing and allows one-tap request.
 */
@Composable
fun PermissionRequestCard(
    modifier: Modifier = Modifier,
    includeNotifications: Boolean = true,
    onPermissionsGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permState by remember(context) { mutableStateOf(checkPermissions(context)) }
    val totalCount = buildList {
        if (includeNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add("notifications")
        }
        add("calendar")
        add("location")
    }.size

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permState = checkPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Build the list of permissions to request
    val permissionsToRequest = remember(permState, includeNotifications) {
        buildList {
            if (includeNotifications &&
                !permState.hasNotifications &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
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
    ) { _ ->
        permState = checkPermissions(context)
        val notificationsReady = !includeNotifications || permState.hasNotifications
        if (notificationsReady && permState.hasCalendar && permState.hasLocation) {
            onPermissionsGranted()
        }
    }

    // Don't show if all granted
    val notificationsReady = !includeNotifications || permState.hasNotifications
    if (notificationsReady && permState.hasCalendar && permState.hasLocation) return

    val missingCount = listOf(
        notificationsReady,
        permState.hasCalendar,
        permState.hasLocation
    ).count { !it }
    val grantedCount = totalCount - missingCount

    AppSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        highlighted = true
    ) {
        AppSectionTitle(
            title = if (includeNotifications) "Recommended permissions" else "Context permissions",
            description = if (includeNotifications) {
                "A few optional permissions unlock weather, calendar, and clearer alarm alerts."
            } else {
                "Optional context that makes the Today tab and morning briefing more useful."
            },
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

        if (includeNotifications && !permState.hasNotifications) {
            PermissionItem(
                icon = Icons.Default.NotificationsActive,
                title = stringResource(R.string.permission_notifications),
                description = stringResource(R.string.permission_show_active_alarms_timer_finish)
            )
        }
        if (!permState.hasCalendar) {
            PermissionItem(
                icon = Icons.Default.CalendarMonth,
                title = stringResource(R.string.permission_calendar),
                description = stringResource(R.string.permission_bring_today_s_events_into)
            )
        }
        if (!permState.hasLocation) {
            PermissionItem(
                icon = Icons.Default.LocationOn,
                title = stringResource(R.string.permission_location),
                description = stringResource(R.string.permission_show_local_weather_without_asking)
            )
        }

        Button(
            onClick = {
                if (permissionsToRequest.isNotEmpty()) {
                    launcher.launch(permissionsToRequest)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
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
            text = stringResource(R.string.permission_android_will_still_ask_confirm),
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PermissionItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceCard.copy(alpha = 0.78f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(description, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                }
                AppStatusChip(
                    label = stringResource(R.string.permission_missing),
                    color = SnoozeYellow
                )
            }
        }
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
