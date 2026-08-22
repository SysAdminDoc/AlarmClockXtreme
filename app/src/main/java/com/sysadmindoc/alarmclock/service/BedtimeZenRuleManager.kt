@file:Suppress("DEPRECATION")

package com.sysadmindoc.alarmclock.service

import android.annotation.SuppressLint
import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import android.service.notification.ZenPolicy
import com.sysadmindoc.alarmclock.MainActivity
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.util.AlarmTimeFormatter
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

data class BedtimeZenRuleStatus(
    val enabled: Boolean,
    val accessGranted: Boolean,
    val active: Boolean,
    val summary: String,
    val ruleId: String = "",
    val error: String? = null
)

data class BedtimeZenSchedule(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val endSource: EndSource
) {
    enum class EndSource { NEXT_ALARM, SLEEP_GOAL }

    val start: LocalTime = LocalTime.of(startHour, startMinute)
    val end: LocalTime = LocalTime.of(endHour, endMinute)
}

object BedtimeZenRuleManager {
    private const val PREFS_NAME = "bedtime_zen_rule_state"
    private const val KEY_RULE_ID = "rule_id"
    private const val KEY_LAST_CONDITION_ID = "last_condition_id"
    private const val RULE_NAME = "AlarmClockXtreme Bedtime DND"
    private const val CONDITION_PATH = "bedtime"
    private const val PARAM_START = "start"
    private const val PARAM_END = "end"
    private const val PARAM_END_SOURCE = "end_source"

    fun isPolicyAccessGranted(context: Context): Boolean {
        val manager = notificationManager(context)
        return manager.isNotificationPolicyAccessGranted
    }

    fun currentStatus(
        context: Context,
        settings: AppSettings,
        nextAlarmTriggerMillis: Long?
    ): BedtimeZenRuleStatus {
        val schedule = buildSchedule(settings, nextAlarmTriggerMillis)
        val conditionId = buildConditionId(context, schedule)
        val condition = buildCondition(context, conditionId)
        val accessGranted = isPolicyAccessGranted(context)
        return BedtimeZenRuleStatus(
            enabled = settings.bedtimeDndEnabled,
            accessGranted = accessGranted,
            active = settings.bedtimeDndEnabled && condition.state == Condition.STATE_TRUE,
            summary = statusSummary(settings, schedule, accessGranted, condition.state),
            ruleId = storedRuleId(context).orEmpty()
        )
    }

    fun syncRule(
        context: Context,
        settings: AppSettings,
        nextAlarmTriggerMillis: Long?
    ): BedtimeZenRuleStatus {
        val appContext = context.applicationContext
        val schedule = buildSchedule(settings, nextAlarmTriggerMillis)
        val conditionId = buildConditionId(appContext, schedule)
        persistLastConditionId(appContext, conditionId)
        val accessGranted = isPolicyAccessGranted(appContext)
        val condition = buildCondition(appContext, conditionId)

        if (!settings.bedtimeDndEnabled) {
            if (accessGranted) {
                removeStoredRule(appContext)
            }
            return BedtimeZenRuleStatus(
                enabled = false,
                accessGranted = accessGranted,
                active = false,
                summary = "Off",
                ruleId = storedRuleId(appContext).orEmpty()
            )
        }

        if (!accessGranted) {
            return BedtimeZenRuleStatus(
                enabled = true,
                accessGranted = false,
                active = false,
                summary = "Needs DND access",
                ruleId = storedRuleId(appContext).orEmpty()
            )
        }

        val manager = notificationManager(appContext)
        return runCatching {
            val owner = providerComponent(appContext)
            val existingRuleId = storedRuleId(appContext)
                ?.takeIf { manager.getAutomaticZenRule(it) != null }
                ?: findExistingRuleId(manager, owner)

            val rule = buildRule(
                context = appContext,
                owner = owner,
                conditionId = conditionId,
                enabled = true,
                schedule = schedule
            )
            val ruleId = if (existingRuleId.isNullOrBlank()) {
                manager.addAutomaticZenRule(rule)
            } else {
                manager.updateAutomaticZenRule(existingRuleId, rule)
                existingRuleId
            }
            persistRuleId(appContext, ruleId)
            setRuleStateIfSupported(manager, ruleId, condition)

            BedtimeZenRuleStatus(
                enabled = true,
                accessGranted = true,
                active = condition.state == Condition.STATE_TRUE,
                summary = statusSummary(settings, schedule, accessGranted = true, condition.state),
                ruleId = ruleId
            )
        }.getOrElse { error ->
            BedtimeZenRuleStatus(
                enabled = true,
                accessGranted = true,
                active = false,
                summary = "Rule sync failed",
                ruleId = storedRuleId(appContext).orEmpty(),
                error = error.message ?: error::class.java.simpleName
            )
        }
    }

    fun lastConditionId(context: Context): Uri? {
        return statePrefs(context).getString(KEY_LAST_CONDITION_ID, null)
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
    }

    fun buildCondition(context: Context, conditionId: Uri): Condition {
        val parsed = parseConditionId(conditionId)
        val valid = Condition.isValidId(conditionId, context.packageName)
        if (!valid || parsed == null) {
            return Condition(
                conditionId,
                "Bedtime DND schedule unavailable",
                Condition.STATE_ERROR
            )
        }

        val active = isWithinWindow(LocalTime.now(), parsed.start, parsed.end)
        val state = if (active) Condition.STATE_TRUE else Condition.STATE_FALSE
        val summary = if (active) {
            "Bedtime DND active until ${formatTime(parsed.end, is24h = false)}"
        } else {
            "Bedtime DND starts at ${formatTime(parsed.start, is24h = false)}"
        }
        return Condition(conditionId, summary, state)
    }

    fun buildSchedule(
        settings: AppSettings,
        nextAlarmTriggerMillis: Long?
    ): BedtimeZenSchedule {
        val start = LocalTime.of(settings.bedtimeHour, settings.bedtimeMinute)
        val now = System.currentTimeMillis()
        val end = if (nextAlarmTriggerMillis != null && nextAlarmTriggerMillis > now) {
            Instant.ofEpochMilli(nextAlarmTriggerMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        } else {
            start.plusMinutes((settings.sleepGoalHours * 60L) + settings.sleepGoalMinutes)
        }
        val source = if (nextAlarmTriggerMillis != null && nextAlarmTriggerMillis > now) {
            BedtimeZenSchedule.EndSource.NEXT_ALARM
        } else {
            BedtimeZenSchedule.EndSource.SLEEP_GOAL
        }
        return BedtimeZenSchedule(
            startHour = start.hour,
            startMinute = start.minute,
            endHour = end.hour,
            endMinute = end.minute,
            endSource = source
        )
    }

    private fun buildConditionId(context: Context, schedule: BedtimeZenSchedule): Uri {
        return Uri.Builder()
            .scheme(Condition.SCHEME)
            .authority(context.packageName)
            .appendPath(CONDITION_PATH)
            .appendQueryParameter(PARAM_START, formatQueryTime(schedule.start))
            .appendQueryParameter(PARAM_END, formatQueryTime(schedule.end))
            .appendQueryParameter(PARAM_END_SOURCE, schedule.endSource.name.lowercase(Locale.US))
            .build()
    }

    private fun parseConditionId(conditionId: Uri): BedtimeZenSchedule? {
        if (conditionId.scheme != Condition.SCHEME ||
            conditionId.pathSegments.firstOrNull() != CONDITION_PATH) {
            return null
        }
        val start = conditionId.getQueryParameter(PARAM_START)?.parseQueryTime() ?: return null
        val end = conditionId.getQueryParameter(PARAM_END)?.parseQueryTime() ?: return null
        val source = when (conditionId.getQueryParameter(PARAM_END_SOURCE)) {
            BedtimeZenSchedule.EndSource.NEXT_ALARM.name.lowercase(Locale.US) ->
                BedtimeZenSchedule.EndSource.NEXT_ALARM
            else -> BedtimeZenSchedule.EndSource.SLEEP_GOAL
        }
        return BedtimeZenSchedule(
            startHour = start.hour,
            startMinute = start.minute,
            endHour = end.hour,
            endMinute = end.minute,
            endSource = source
        )
    }

    private fun isWithinWindow(now: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        return when {
            start == end -> true
            start.isBefore(end) -> !now.isBefore(start) && now.isBefore(end)
            else -> !now.isBefore(start) || now.isBefore(end)
        }
    }

    private fun statusSummary(
        settings: AppSettings,
        schedule: BedtimeZenSchedule,
        accessGranted: Boolean,
        conditionState: Int
    ): String {
        if (!settings.bedtimeDndEnabled) return "Off"
        if (!accessGranted) return "Needs DND access"
        return if (conditionState == Condition.STATE_TRUE) {
            "Active until ${formatTime(schedule.end, settings.is24HourFormat)}"
        } else {
            "Ready at ${formatTime(schedule.start, settings.is24HourFormat)}"
        }
    }

    @SuppressLint("NewApi")
    private fun buildRule(
        context: Context,
        owner: ComponentName,
        conditionId: Uri,
        enabled: Boolean,
        schedule: BedtimeZenSchedule
    ): AutomaticZenRule {
        val triggerDescription =
            "${formatTime(schedule.start, false)}-${formatTime(schedule.end, false)}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return AutomaticZenRule.Builder(RULE_NAME, conditionId)
                .setOwner(owner)
                .setConfigurationActivity(ComponentName(context, MainActivity::class.java))
                .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                .setEnabled(enabled)
                .setZenPolicy(alarmsOnlyPolicy())
                .setType(AutomaticZenRule.TYPE_SCHEDULE_TIME)
                .setTriggerDescription(triggerDescription)
                .setIconResId(R.drawable.ic_alarm)
                .setManualInvocationAllowed(false)
                .build()
        }

        return AutomaticZenRule(
            RULE_NAME,
            owner,
            conditionId,
            NotificationManager.INTERRUPTION_FILTER_ALARMS,
            enabled
        ).also { rule ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                rule.setConfigurationActivity(ComponentName(context, MainActivity::class.java))
                rule.setZenPolicy(alarmsOnlyPolicy())
            }
        }
    }

    @SuppressLint("NewApi")
    private fun alarmsOnlyPolicy(): ZenPolicy {
        return ZenPolicy.Builder()
            .allowAlarms(true)
            .allowCalls(ZenPolicy.PEOPLE_TYPE_NONE)
            .allowMessages(ZenPolicy.PEOPLE_TYPE_NONE)
            .allowRepeatCallers(false)
            .allowReminders(false)
            .allowEvents(false)
            .allowMedia(false)
            .allowSystem(false)
            .hideAllVisualEffects()
            .build()
    }

    private fun notificationManager(context: Context): NotificationManager {
        return context.getSystemService(NotificationManager::class.java)
    }

    private fun providerComponent(context: Context): ComponentName {
        return ComponentName(context, BedtimeZenRuleService::class.java)
    }

    private fun findExistingRuleId(
        manager: NotificationManager,
        owner: ComponentName
    ): String? {
        return runCatching {
            manager.automaticZenRules.entries.firstOrNull { (_, rule) ->
                rule.owner == owner || rule.name == RULE_NAME
            }?.key
        }.getOrNull()
    }

    private fun removeStoredRule(context: Context) {
        val ruleId = storedRuleId(context) ?: return
        runCatching {
            notificationManager(context).removeAutomaticZenRule(ruleId)
        }
        persistRuleId(context, null)
    }

    private fun storedRuleId(context: Context): String? {
        return statePrefs(context).getString(KEY_RULE_ID, null)
            ?.takeIf { it.isNotBlank() }
    }

    private fun persistRuleId(context: Context, ruleId: String?) {
        statePrefs(context).edit().apply {
            if (ruleId.isNullOrBlank()) remove(KEY_RULE_ID) else putString(KEY_RULE_ID, ruleId)
        }.apply()
    }

    private fun persistLastConditionId(context: Context, conditionId: Uri) {
        statePrefs(context).edit()
            .putString(KEY_LAST_CONDITION_ID, conditionId.toString())
            .apply()
    }

    private fun statePrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @SuppressLint("NewApi")
    private fun setRuleStateIfSupported(
        manager: NotificationManager,
        ruleId: String,
        condition: Condition
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            runCatching { manager.setAutomaticZenRuleState(ruleId, condition) }
        }
    }

    private fun String.parseQueryTime(): LocalTime? {
        return runCatching {
            val parts = split(":")
            LocalTime.of(parts[0].toInt(), parts[1].toInt())
        }.getOrNull()
    }

    private fun formatQueryTime(time: LocalTime): String {
        return "%02d:%02d".format(Locale.US, time.hour, time.minute)
    }

    // The rule name shows up in the system Do Not Disturb screen, so it
    // follows the phone's locale. formatQueryTime above is the wire format
    // inside the condition URI and stays Locale.US on purpose.
    private fun formatTime(time: LocalTime, is24h: Boolean): String =
        AlarmTimeFormatter.format(time.hour, time.minute, is24h)
}
