package com.sysadmindoc.alarmclock.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.squareup.moshi.Moshi
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DismissActionExecutorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun webhookExecutesHttpsPost() = runTest {
        val requests = mutableListOf<Request>()
        val executor = executorWith(requestRecorder(requests))
        val result = executor.execute(
            Alarm(
                id = 42,
                label = "Work",
                dismissActionType = "WEBHOOK",
                dismissActionPayload = "https://example.com/hook"
            )
        )

        assertEquals(DismissActionResult.Success, result)
        assertEquals("POST", requests.single().method)
        assertEquals("https://example.com/hook", requests.single().url.toString())
        assertTrue(requests.single().body != null)
    }

    @Test
    fun webhookRejectsPlainHttp() = runTest {
        val requests = mutableListOf<Request>()
        val executor = executorWith(requestRecorder(requests))
        val result = executor.execute(
            Alarm(
                dismissActionType = "WEBHOOK",
                dismissActionPayload = "http://example.com/hook"
            )
        )

        assertTrue(result is DismissActionResult.Failure)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun broadcastIntentIsPackageScopedAndCarriesAlarmFields() {
        val intent = DismissActionExecutor.buildBroadcastIntent(
            context = context,
            alarm = Alarm(id = 7, label = "Gym"),
            action = "com.example.ALARM_DISMISSED"
        )

        assertEquals("com.example.ALARM_DISMISSED", intent.action)
        assertEquals(context.packageName, intent.`package`)
        assertEquals(7L, intent.getLongExtra("alarmId", -1))
        assertEquals("Gym", intent.getStringExtra("label"))
    }

    @Test
    fun broadcastActionValidationRejectsUnsafeInput() {
        assertTrue(DismissActionExecutor.isAllowedBroadcastAction("com.example.ALARM_DISMISSED"))
        assertFalse(DismissActionExecutor.isAllowedBroadcastAction(""))
        assertFalse(DismissActionExecutor.isAllowedBroadcastAction("1bad.START"))
        assertFalse(DismissActionExecutor.isAllowedBroadcastAction("com.example.ALARM DISMISSED"))
    }

    @Test
    fun hueSceneTargetSupportsSceneOnlyAndGroupScenePayloads() {
        assertEquals(HueSceneTarget(sceneId = "scene_123"), HueSceneTarget.parse("scene_123"))
        assertEquals(HueSceneTarget(sceneId = "scene_123", groupId = "5"), HueSceneTarget.parse("5:scene_123"))
        assertEquals(null, HueSceneTarget.parse("bad scene"))
        assertEquals(null, HueSceneTarget.parse("5:bad scene"))
    }

    @Test
    fun hueV2RequestUsesSceneRecallEndpoint() {
        val request = DismissActionExecutor.buildHueSceneRequestV2(
            bridgeIp = "192.168.1.2",
            apiKey = "app_key",
            sceneId = "scene_123"
        )

        assertEquals("PUT", request.method)
        assertEquals("https://192.168.1.2/clip/v2/resource/scene/scene_123", request.url.toString())
        assertEquals("app_key", request.header("hue-application-key"))
    }

    @Test
    fun hueV1RequestUsesGroupActionFallback() {
        val request = DismissActionExecutor.buildHueSceneRequestV1(
            bridgeIp = "bridge.local",
            apiKey = "app_key",
            target = HueSceneTarget(sceneId = "scene_123", groupId = "5")
        )

        assertEquals("PUT", request.method)
        assertEquals("http://bridge.local/api/app_key/groups/5/action", request.url.toString())
    }

    @Test
    fun hueSceneExecutesV2Request() = runTest {
        val requests = mutableListOf<Request>()
        val executor = executorWith(
            client = requestRecorder(requests),
            settings = AppSettings(
                hueBridgeIp = "192.168.1.2",
                hueApiKey = "app_key",
                hueLightIds = "1"
            )
        )

        val result = executor.execute(
            Alarm(
                dismissActionType = "HUE_SCENE",
                dismissActionPayload = "scene_123"
            )
        )

        assertEquals(DismissActionResult.Success, result)
        assertEquals("https://192.168.1.2/clip/v2/resource/scene/scene_123", requests.single().url.toString())
    }

    private fun executorWith(
        client: OkHttpClient,
        settings: AppSettings = AppSettings()
    ): DismissActionExecutor {
        val preferences = mockk<PreferencesManager>(relaxed = true)
        coEvery { preferences.getCurrentSettings() } returns settings
        return DismissActionExecutor(
            context = context,
            preferencesManager = preferences,
            client = client,
            moshi = Moshi.Builder().build()
        )
    }

    private fun requestRecorder(requests: MutableList<Request>): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                requests += chain.request()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            })
            .build()
}
