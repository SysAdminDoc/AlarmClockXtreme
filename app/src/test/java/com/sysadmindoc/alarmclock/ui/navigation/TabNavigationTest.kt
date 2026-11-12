package com.sysadmindoc.alarmclock.ui.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.Navigator
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tapping Alarms from Settings left Settings on screen: the first tab switch
 * saved Settings under the Alarms destination and Alarms then "restored" it.
 * After onboarding the graph's start destination was gone from the stack, so
 * tab switches stopped popping at all.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class TabNavigationTest {
    private val alarms = Screen.AlarmList.route
    private val settings = Screen.Settings.route
    private val today = Screen.Dashboard.route

    private fun controller(start: String): TestNavHostController =
        TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
            graph = createGraph(startDestination = start) {
                screen(Screen.Onboarding.route)
                screen(alarms)
                screen(settings)
                screen(today)
            }
        }

    // A plain destination on the controller's "test" navigator; the real
    // graph's composable() needs a ComposeNavigator the test provider lacks.
    private fun NavGraphBuilder.screen(route: String) =
        destination(NavDestinationBuilder(provider.getNavigator<Navigator<NavDestination>>("test"), route))

    private fun TestNavHostController.routes() =
        backStack.filterNot { it.destination is NavGraph }.map { it.destination.route }

    @Test
    fun `Alarms tab leaves Settings however Settings was opened`() {
        // A plain navigate, as the gear used to do and a deep link still can:
        // the first tab pop then filed Settings' state under Alarms.
        val nav = controller(start = alarms)
        nav.navigate(settings)
        nav.navigateToTab(alarms)
        assertEquals(listOf(alarms), nav.routes())
    }

    @Test
    fun `Alarms tab leaves Settings after the gear opened it`() {
        val nav = controller(start = alarms)
        nav.navigateToTab(settings)
        nav.navigateToTab(alarms)
        assertEquals(listOf(alarms), nav.routes())
    }

    @Test
    fun `Alarms tab still works after a detour through Today`() {
        val nav = controller(start = alarms)
        nav.navigate(settings)
        nav.navigateToTab(today)
        nav.navigateToTab(alarms)
        assertEquals(listOf(alarms), nav.routes())
    }

    @Test
    fun `tabs replace each other after onboarding finishes`() {
        val nav = controller(start = Screen.Onboarding.route)
        nav.navigate(alarms) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
        nav.navigateToTab(settings)
        assertEquals(listOf(alarms, settings), nav.routes())
        nav.navigateToTab(today)
        assertEquals(listOf(alarms, today), nav.routes())
        nav.navigateToTab(alarms)
        assertEquals(listOf(alarms), nav.routes())
    }

    @Test
    fun `a tab comes back as it was left`() {
        val nav = controller(start = alarms)
        nav.navigateToTab(settings)
        val settingsEntry = nav.currentBackStackEntry!!.id
        nav.navigateToTab(today)
        nav.navigateToTab(settings)
        assertEquals(settingsEntry, nav.currentBackStackEntry!!.id)
    }
}
