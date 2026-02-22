package com.sysadmindoc.alarmclock.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import android.content.pm.PackageManager
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Permissions",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!permState.hasNotifications) {
                PermissionItem(Icons.Default.Notifications, "Notifications", "Show alarm and timer alerts")
            }
            if (!permState.hasCalendar) {
                PermissionItem(Icons.Default.CalendarMonth, "Calendar", "Show today's events on dashboard")
            }
            if (!permState.hasLocation) {
                PermissionItem(Icons.Default.LocationOn, "Location", "Weather for your area on dashboard")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (permissionsToRequest.isNotEmpty()) {
                        launcher.launch(permissionsToRequest)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Grant Permissions")
            }
        }
    }
}

@Composable
private fun PermissionItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, color = TextPrimary, fontSize = 14.sp)
            Text(description, color = TextMuted, fontSize = 11.sp)
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
