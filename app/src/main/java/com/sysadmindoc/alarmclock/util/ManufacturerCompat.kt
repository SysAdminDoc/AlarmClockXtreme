package com.sysadmindoc.alarmclock.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri

/**
 * Handles manufacturer-specific battery optimization detection and guidance.
 * Many OEMs (Samsung, Xiaomi, OnePlus, Huawei) aggressively kill background apps
 * beyond stock Android's Doze behavior, breaking alarm delivery.
 *
 * Reference: https://dontkillmyapp.com
 */
object ManufacturerCompat {

    data class BatteryGuidance(
        val manufacturer: String,
        val title: String,
        val steps: List<String>,
        val settingsIntent: Intent? = null
    )

    fun getManufacturer(): String = Build.MANUFACTURER.lowercase()

    fun needsBatteryGuidance(): Boolean {
        return getManufacturer() in listOf("samsung", "xiaomi", "oneplus", "huawei", "oppo", "vivo", "realme")
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestBatteryOptimizationExemption(context: Context) {
        try {
            if (!isIgnoringBatteryOptimizations(context)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            // Fallback to general battery settings if direct request fails
            try {
                val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) { /* Device doesn't support this */ }
        }
    }

    fun getGuidance(): BatteryGuidance? {
        return when (getManufacturer()) {
            "samsung" -> BatteryGuidance(
                manufacturer = "Samsung",
                title = "Prevent Samsung from killing alarms",
                steps = listOf(
                    "Open Settings > Battery",
                    "Tap 'Background usage limits'",
                    "Tap 'Never sleeping apps'",
                    "Add AlarmClockXtreme to the list"
                )
            )
            "xiaomi" -> BatteryGuidance(
                manufacturer = "Xiaomi",
                title = "Enable Autostart on Xiaomi",
                steps = listOf(
                    "Open Settings > Apps > Manage apps",
                    "Find AlarmClockXtreme and tap it",
                    "Enable 'Autostart'",
                    "Set Battery saver to 'No restrictions'"
                )
            )
            "oneplus" -> BatteryGuidance(
                manufacturer = "OnePlus",
                title = "Disable battery optimization on OnePlus",
                steps = listOf(
                    "Open Settings > Battery > Battery optimization",
                    "Find AlarmClockXtreme",
                    "Select 'Don't optimize'"
                )
            )
            "huawei" -> BatteryGuidance(
                manufacturer = "Huawei",
                title = "Allow background activity on Huawei",
                steps = listOf(
                    "Open Settings > Battery > App launch",
                    "Find AlarmClockXtreme",
                    "Disable 'Manage automatically'",
                    "Enable all three toggles manually"
                )
            )
            else -> null
        }
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
