package com.sysadmindoc.alarmclock.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * MainActivity's `acx://navigate` filter is exported and unpermissioned, so
 * anything reaching NavController has to survive a hostile caller.
 */
class AppNavigationRouteResolverTest {
    @Test
    fun `launcher shortcut targets resolve`() {
        assertEquals("timer", resolveDeepLinkRoute(listOf("timer")))
        assertEquals("bedtime", resolveDeepLinkRoute(listOf("bedtime")))
        assertEquals("alarm_edit/0", resolveDeepLinkRoute(listOf("alarm_edit", "0")))
    }

    @Test
    fun `every bottom nav tab is reachable`() {
        bottomNavItems.forEach { item ->
            assertEquals(
                item.screen.route,
                resolveDeepLinkRoute(listOf(item.screen.route))
            )
        }
    }

    @Test
    fun `unknown routes are rejected instead of crashing NavController`() {
        assertNull(resolveDeepLinkRoute(listOf("not_a_route")))
        assertNull(resolveDeepLinkRoute(listOf("../../etc/passwd")))
        assertNull(resolveDeepLinkRoute(emptyList()))
        assertNull(resolveDeepLinkRoute(listOf("")))
    }

    @Test
    fun `alarm edit requires a numeric id`() {
        assertNull(resolveDeepLinkRoute(listOf("alarm_edit", "abc")))
        assertNull(resolveDeepLinkRoute(listOf("alarm_edit")))
        assertNull(resolveDeepLinkRoute(listOf("alarm_edit", "1", "2")))
        assertEquals("alarm_edit/-1", resolveDeepLinkRoute(listOf("alarm_edit", "-1")))
    }

    @Test
    fun `routes needing caller-supplied state stay unreachable from outside`() {
        assertNull(resolveDeepLinkRoute(listOf(Screen.Onboarding.route)))
        assertNull(resolveDeepLinkRoute(listOf(Screen.SharedAlarmImport.route)))
    }

    @Test
    fun `multi segment paths are not joined blindly`() {
        assertNull(resolveDeepLinkRoute(listOf("settings", "timer")))
    }
}
