package com.sysadmindoc.alarmclock.ui.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.sysadmindoc.alarmclock.R

/**
 * A button that asks for one runtime permission. After a second "Don't allow"
 * Android stops showing the dialog, so the button then opens the app's system
 * settings, the only place left to turn it on. [onResult] runs after every
 * answer so the caller can re-read the grant.
 */
@Composable
internal fun PermissionRequestButton(
    permission: String,
    label: String,
    onResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    var openSettingsInstead by rememberSaveable(permission) { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        openSettingsInstead = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        onResult()
    }
    OutlinedButton(
        onClick = {
            if (openSettingsInstead) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            } else {
                launcher.launch(permission)
            }
        },
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(if (openSettingsInstead) stringResource(R.string.permission_open_app_settings) else label)
    }
}
