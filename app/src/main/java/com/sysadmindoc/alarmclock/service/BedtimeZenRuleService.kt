@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.sysadmindoc.alarmclock.service

import android.net.Uri
import android.service.notification.ConditionProviderService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the app-created bedtime DND rule.
 *
 * The rule criteria live in the condition URI so the system can re-bind this
 * service after process death without needing Hilt or DataStore to answer the
 * current active/inactive state.
 */
class BedtimeZenRuleService : ConditionProviderService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val subscribedConditionIds = linkedSetOf<Uri>()
    private var tickerStarted = false

    override fun onConnected() {
        BedtimeZenRuleManager.lastConditionId(this)?.let { conditionId ->
            publish(conditionId)
        }
    }

    override fun onRequestConditions(relevance: Int) {
        val ids = synchronized(subscribedConditionIds) {
            subscribedConditionIds.toList()
        }
        if (ids.isEmpty()) {
            BedtimeZenRuleManager.lastConditionId(this)?.let { publish(it) }
        } else {
            ids.forEach(::publish)
        }
    }

    override fun onSubscribe(conditionId: Uri) {
        synchronized(subscribedConditionIds) {
            subscribedConditionIds += conditionId
        }
        publish(conditionId)
        startMinuteTicker()
    }

    override fun onUnsubscribe(conditionId: Uri) {
        synchronized(subscribedConditionIds) {
            subscribedConditionIds -= conditionId
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun publish(conditionId: Uri) {
        notifyCondition(BedtimeZenRuleManager.buildCondition(this, conditionId))
    }

    private fun startMinuteTicker() {
        if (tickerStarted) return
        tickerStarted = true
        serviceScope.launch {
            while (isActive) {
                delay(CONDITION_REFRESH_MS)
                val ids = synchronized(subscribedConditionIds) {
                    subscribedConditionIds.toList()
                }
                ids.ifEmpty {
                    BedtimeZenRuleManager.lastConditionId(this@BedtimeZenRuleService)
                        ?.let(::listOf)
                        .orEmpty()
                }.forEach(::publish)
            }
        }
    }

    private companion object {
        const val CONDITION_REFRESH_MS = 60_000L
    }
}
