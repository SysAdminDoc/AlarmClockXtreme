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
        /** Vendor-specific DontKillMyApp page with up-to-date remediation. */
        val dontKillMyAppUrl: String,
        val settingsIntent: Intent? = null
    )

    fun getManufacturer(): String = Build.MANUFACTURER.lowercase()

    /**
     * True when this OEM is known to kill background apps beyond stock Doze.
     * Derived from [getGuidance] so the two can never drift — every flagged OEM
     * always has actionable steps (guarded by ManufacturerCompatTest).
     */
    fun needsBatteryGuidance(manufacturer: String = getManufacturer()): Boolean =
        getGuidance(manufacturer) != null

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

    fun getGuidance(manufacturer: String = getManufacturer()): BatteryGuidance? {
        return when (manufacturer.lowercase()) {
            "samsung" -> BatteryGuidance(
                manufacturer = "Samsung",
                title = "Prevent Samsung from killing alarms",
                steps = listOf(
                    "Open Settings > Battery",
                    "Tap 'Background usage limits'",
                    "Tap 'Never sleeping apps'",
                    "Add AlarmClockXtreme to the list"
                ),
                dontKillMyAppUrl = "https://dontkillmyapp.com/samsung"
            )
            "xiaomi", "redmi", "poco" -> BatteryGuidance(
                manufacturer = "Xiaomi",
                title = "Enable Autostart on Xiaomi",
                steps = listOf(
                    "Open Settings > Apps > Manage apps",
                    "Find AlarmClockXtreme and tap it",
                    "Enable 'Autostart'",
                    "Set Battery saver to 'No restrictions'"
                ),
                dontKillMyAppUrl = "https://dontkillmyapp.com/xiaomi"
            )
            "oneplus" -> BatteryGuidance(
                manufacturer = "OnePlus",
                title = "Disable battery optimization on OnePlus",
                steps = listOf(
                    "Open Settings > Battery > Battery optimization",
                    "Find AlarmClockXtreme",
                    "Select 'Don't optimize'"
                ),
                dontKillMyAppUrl = "https://dontkillmyapp.com/oneplus"
            )
            "huawei", "honor" -> BatteryGuidance(
                manufacturer = "Huawei",
                title = "Allow background activity on Huawei",
                steps = listOf(
                    "Open Settings > Battery > App launch",
                    "Find AlarmClockXtreme",
                    "Disable 'Manage automatically'",
                    "Enable all three toggles manually"
                ),
                dontKillMyAppUrl = "https://dontkillmyapp.com/huawei"
            )
            "oppo", "realme" -> BatteryGuidance(
                manufacturer = manufacturer.replaceFirstChar { it.uppercase() },
                title = "Allow background activity on ColorOS",
                steps = listOf(
                    "Open Settings > Battery > App battery management",
                    "Find AlarmClockXtreme",
                    "Enable 'Allow background activity' and 'Allow auto-launch'",
                    "Set Battery optimization to 'Don't optimize'"
                ),
                dontKillMyAppUrl = "https://dontkillmyapp.com/${manufacturer.lowercase()}"
            )
            "vivo", "iqoo" -> BatteryGuidance(
                manufacturer = "Vivo",
                title = "Allow background activity on Vivo",
                steps = listOf(
                    "Open Settings > Battery > Background power consumption",
                    "Find AlarmClockXtreme and allow high background power",
                    "Open Settings > Apps > Auto-start and enable AlarmClockXtreme"
                ),
                dontKillMyAppUrl = "https://dontkillmyapp.com/vivo"
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
