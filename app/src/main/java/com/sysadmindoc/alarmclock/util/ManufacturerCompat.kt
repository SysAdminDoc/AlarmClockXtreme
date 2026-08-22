package com.sysadmindoc.alarmclock.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import androidx.annotation.StringRes
import com.sysadmindoc.alarmclock.R

/**
 * Handles manufacturer-specific battery optimization detection and guidance.
 * Many OEMs (Samsung, Xiaomi, OnePlus, Huawei) aggressively kill background apps
 * beyond stock Android's Doze behavior, breaking alarm delivery.
 *
 * Reference: https://dontkillmyapp.com
 */
object ManufacturerCompat {

    /** Platform-neutral description of one best-effort OEM settings target. */
    data class SettingsIntentSpec(
        val action: String? = null,
        val packageName: String? = null,
        val componentPackage: String? = null,
        val componentClass: String? = null,
        val intExtras: Map<String, Int> = emptyMap()
    ) {
        fun toIntent(): Intent {
            val intent = if (action == null) Intent() else Intent(action)
            packageName?.let { intent.setPackage(it) }
            if (componentPackage != null && componentClass != null) {
                intent.component = ComponentName(componentPackage, componentClass)
            }
            intExtras.forEach { (key, value) -> intent.putExtra(key, value) }
            return intent
        }
    }

    data class BatteryGuidance(
        val manufacturer: String,
        // An id: this is a plain object with no Context, and the title is a
        // heading on the readiness card.
        @StringRes val titleRes: Int,
        val steps: List<String>,
        /** Vendor-specific DontKillMyApp page with up-to-date remediation. */
        val dontKillMyAppUrl: String,
        /** Best-effort vendor settings candidates; every launch has a generic fallback. */
        val settingsIntents: List<SettingsIntentSpec> = emptyList()
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

    /**
     * OEM activity names are undocumented and change between vendor releases.
     * Keep this list best-effort, retain the generic Android fallback, and keep
     * the public DontKillMyApp guide available when a candidate is stale.
     */
    fun openBatterySettings(context: Context): Boolean {
        getGuidance()?.settingsIntents?.forEach { candidate ->
            val launched = runCatching {
                context.startActivity(
                    candidate.toIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.isSuccess
            if (launched) return true
        }
        return requestBatteryOptimizationExemption(context)
    }

    fun requestBatteryOptimizationExemption(context: Context): Boolean {
        try {
            if (!isIgnoringBatteryOptimizations(context)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            }
            return true
        } catch (e: Exception) {
            // Fallback to general battery settings if direct request fails
            try {
                val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                return true
            } catch (_: Exception) {
                return false
            }
        }
    }

    fun getGuidance(manufacturer: String = getManufacturer()): BatteryGuidance? {
        return when (manufacturer.lowercase()) {
            "samsung" -> BatteryGuidance(
                manufacturer = "Samsung",
                titleRes = R.string.oem_samsung_title,
                steps = listOf(
                    "Open Settings > Battery",
                    "Tap 'Background usage limits'",
                    "Tap 'Never sleeping apps'",
                    "Add AlarmClockXtreme to the list"
                ),
                dontKillMyAppUrl = "https://dontkillmyapp.com/samsung",
                settingsIntents = listOf(
                    SettingsIntentSpec(
                        action = "com.samsung.android.sm.ACTION_OPEN_CHECKABLE_LISTACTIVITY",
                        packageName = "com.samsung.android.lool",
                        intExtras = mapOf("activity_type" to 2)
                    )
                )
            )
            "xiaomi", "redmi", "poco" -> BatteryGuidance(
                manufacturer = "Xiaomi",
                titleRes = R.string.oem_xiaomi_title,
                steps = listOf(
                    "Open Settings > Apps > Manage apps",
                    "Find AlarmClockXtreme and tap it",
                    "Enable 'Autostart'",
                    "Set Battery saver to 'No restrictions'"
                ),
                dontKillMyAppUrl = "https://dontkillmyapp.com/xiaomi",
                settingsIntents = listOf(
                    componentIntent(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    ),
                    SettingsIntentSpec(
                        action = "miui.intent.action.OP_AUTO_START",
                        packageName = "com.miui.securitycenter"
                    )
                )
            )
            "oneplus" -> BatteryGuidance(
                manufacturer = "OnePlus",
                titleRes = R.string.oem_oneplus_title,
                steps = listOf(
                    "Open Settings > Battery > Battery optimization",
                    "Find AlarmClockXtreme",
                    "Select 'Don't optimize'"
                ),
                dontKillMyAppUrl = "https://dontkillmyapp.com/oneplus",
                settingsIntents = listOf(
                    componentIntent(
                        "com.oneplus.security",
                        "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                    ),
                    componentIntent(
                        "com.android.settings",
                        "com.android.settings.Settings\$HighPowerApplicationsActivity"
                    )
                )
            )
            "huawei", "honor" -> BatteryGuidance(
                manufacturer = "Huawei",
                titleRes = R.string.oem_huawei_title,
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
                titleRes = R.string.oem_coloros_title,
                steps = listOf(
                    "Open Settings > Battery > App battery management",
                    "Find AlarmClockXtreme",
                    "Enable 'Allow background activity' and 'Allow auto-launch'",
                    "Set Battery optimization to 'Don't optimize'"
                ),
                dontKillMyAppUrl = "https://dontkillmyapp.com/${manufacturer.lowercase()}",
                settingsIntents = colorOsStartupIntents()
            )
            "vivo", "iqoo" -> BatteryGuidance(
                manufacturer = "Vivo",
                titleRes = R.string.oem_vivo_title,
                steps = listOf(
                    "Open Settings > Battery > Background power consumption",
                    "Find AlarmClockXtreme and allow high background power",
                    "Open Settings > Apps > Auto-start and enable AlarmClockXtreme"
                ),
                dontKillMyAppUrl = "https://dontkillmyapp.com/vivo",
                settingsIntents = listOf(
                    componentIntent(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    ),
                    componentIntent(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                    ),
                    componentIntent(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
                    )
                )
            )
            else -> null
        }
    }

    private fun colorOsStartupIntents(): List<SettingsIntentSpec> = listOf(
        componentIntent(
            "com.coloros.safecenter",
            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
        ),
        componentIntent(
            "com.coloros.safecenter",
            "com.coloros.safecenter.startupapp.StartupAppListActivity"
        ),
        componentIntent(
            "com.oppo.safe",
            "com.oppo.safe.permission.startup.StartupAppListActivity"
        ),
        componentIntent(
            "com.coloros.safecenter",
            "com.coloros.safecenter.permission.startupmanager.StartupAppListActivity"
        )
    )

    private fun componentIntent(packageName: String, className: String): SettingsIntentSpec =
        SettingsIntentSpec(
            componentPackage = packageName,
            componentClass = className
        )

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
