package com.sysadmindoc.alarmclock.integration.dirigera

import java.net.URLDecoder
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DirigeraClientTest {

    private data class Recorded(
        val method: String,
        val url: HttpUrl,
        val authorization: String?,
        val body: String?
    )

    /** Stands in for the hub: records every call and answers from [respond]. */
    private class FakeHub(private val respond: (Request) -> Pair<Int, String>) : Interceptor {
        val requests = mutableListOf<Recorded>()

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val body = request.body?.let { requestBody ->
                Buffer().also { requestBody.writeTo(it) }.readUtf8()
            }
            requests += Recorded(request.method, request.url, request.header("Authorization"), body)
            val (code, json) = respond(request)
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("fake")
                .body(json.toResponseBody("application/json".toMediaType()))
                .build()
        }

        fun session() = DirigeraSession(
            client = OkHttpClient.Builder().addInterceptor(this).build(),
            pinnedFingerprint = "",
            trustManager = null
        )
    }

    private val client = DirigeraClient(OkHttpClient())

    private fun formFields(body: String?): Map<String, String> =
        body.orEmpty().split("&").filter { it.isNotEmpty() }.associate { pair ->
            val (key, value) = pair.split("=", limit = 2)
            URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(value, "UTF-8")
        }

    @Test
    fun challengeMatchesTheRfc7636Example() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            DirigeraPkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
        )
    }

    @Test
    fun verifierIsTheLongestAllowedAndUsesOnlyUnreservedCharacters() {
        val first = DirigeraPkce.newVerifier()
        val second = DirigeraPkce.newVerifier()

        assertTrue(first, Regex("^[A-Za-z0-9._~-]{128}$").matches(first))
        assertFalse(first == second)
    }

    @Test
    fun pairingWaitsForTheButtonThenTradesTheCodeForAToken() = runTest {
        var tokenCalls = 0
        val hub = FakeHub { request ->
            when (request.url.encodedPath) {
                "/v1/oauth/authorize" -> 200 to """{"code":"abc123"}"""
                "/v1/oauth/token" -> {
                    tokenCalls++
                    if (tokenCalls < 3) {
                        403 to """{"error":"Button not pressed"}"""
                    } else {
                        200 to """{"access_token":"eyJhbGciOiJFUzI1NiJ9.payload.sig"}"""
                    }
                }
                else -> 404 to "{}"
            }
        }
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val requestsWhenPrompted = mutableListOf<Int>()

        val result = client.pair(
            rawHost = "192.168.1.50",
            session = hub.session(),
            clientName = "AlarmClockXtreme",
            onPressButton = { requestsWhenPrompted += hub.requests.size },
            verifier = verifier,
            pollIntervalMillis = 2_000L,
            maxPolls = 5
        )

        assertEquals(DirigeraPairResult.Paired("eyJhbGciOiJFUzI1NiJ9.payload.sig", null), result)
        assertEquals("the prompt comes once, right after the code", listOf(1), requestsWhenPrompted)

        val authorize = hub.requests.first()
        assertEquals("GET", authorize.method)
        assertEquals("https", authorize.url.scheme)
        assertEquals("192.168.1.50", authorize.url.host)
        assertEquals(8443, authorize.url.port)
        assertEquals("homesmart.local", authorize.url.queryParameter("audience"))
        assertEquals("code", authorize.url.queryParameter("response_type"))
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", authorize.url.queryParameter("code_challenge"))
        assertEquals("S256", authorize.url.queryParameter("code_challenge_method"))

        val tokenRequests = hub.requests.drop(1)
        assertEquals(3, tokenRequests.size)
        tokenRequests.forEach { request ->
            assertEquals("POST", request.method)
            assertEquals("/v1/oauth/token", request.url.encodedPath)
            assertEquals(
                mapOf(
                    "code" to "abc123",
                    "name" to "AlarmClockXtreme",
                    "grant_type" to "authorization_code",
                    "code_verifier" to verifier
                ),
                formFields(request.body)
            )
        }
    }

    @Test
    fun pairingGivesUpWhenTheButtonIsNeverPressed() = runTest {
        val hub = FakeHub { request ->
            if (request.url.encodedPath == "/v1/oauth/authorize") {
                200 to """{"code":"abc123"}"""
            } else {
                403 to """{"error":"Button not pressed"}"""
            }
        }

        val result = client.pair("192.168.1.50", hub.session(), "AlarmClockXtreme", maxPolls = 2)

        assertEquals(DirigeraPairResult.ButtonNotPressed, result)
        assertEquals(2, hub.requests.count { it.url.encodedPath == "/v1/oauth/token" })
    }

    @Test
    fun pairingStopsWhenTheHubRefusesToIssueACode() = runTest {
        val hub = FakeHub { 500 to "{}" }
        var prompted = false

        val result = client.pair(
            "192.168.1.50", hub.session(), "AlarmClockXtreme",
            onPressButton = { prompted = true }
        )

        assertEquals(DirigeraPairResult.Failed("HTTP_500"), result)
        assertEquals(1, hub.requests.size)
        assertFalse(prompted)
    }

    @Test
    fun publicHostsAndExplicitPortsAreRefusedWithoutAnyRequest() = runTest {
        val hub = FakeHub { 200 to """{"code":"abc123"}""" }

        assertEquals(DirigeraPairResult.InvalidHost, client.pair("example.com", hub.session(), "x"))
        assertEquals(DirigeraPairResult.InvalidHost, client.pair("192.168.1.50:8443", hub.session(), "x"))
        assertEquals(DirigeraPairResult.InvalidHost, client.pair("[fd00::5]:8443", hub.session(), "x"))
        assertTrue(hub.requests.isEmpty())
        assertEquals("[fd00::5]", DirigeraClient.sanitiseHost("[fd00::5]"))
        assertEquals("gw2-1a2b3c.local", DirigeraClient.sanitiseHost("gw2-1a2b3c.local"))
    }

    @Test
    fun parseLightsKeepsOnlyLightsAndReadsTheirCapabilities() {
        val json = """
            [
              {"id":"aaaa-1111_1","type":"light","deviceType":"light","isReachable":true,
               "attributes":{"customName":"Bedroom","isOn":false,"lightLevel":40,
                 "colorTemperature":2700,"colorTemperatureMin":4000,"colorTemperatureMax":2202},
               "capabilities":{"canSend":[],"canReceive":["customName","isOn","lightLevel","colorTemperature"]}},
              {"id":"bbbb-2222_1","type":"light","deviceType":"light","isReachable":false,
               "attributes":{"customName":"","isOn":false,"lightLevel":100},
               "capabilities":{"canReceive":["isOn","lightLevel"]}},
              {"id":"cccc-3333_1","type":"controller","deviceType":"lightController",
               "attributes":{"customName":"Remote"}},
              {"id":"dddd-4444_1","type":"light","deviceType":"light",
               "attributes":{"customName":"Aaa lamp","isOn":true},
               "capabilities":{"canReceive":["isOn"]}},
              {"id":"eeee-5555_1","deviceType":"light",
               "attributes":{"customName":"Hall","lightLevel":10,"colorTemperature":3000,
                 "colorTemperatureMin":2700,"colorTemperatureMax":6500}}
            ]
        """.trimIndent()

        val lights = DirigeraClient.parseLights(json)

        assertEquals(
            listOf(
                DirigeraLight("dddd-4444_1", "Aaa lamp", true, false, null, null),
                DirigeraLight("bbbb-2222_1", "bbbb-2222_1", false, true, null, null),
                DirigeraLight("aaaa-1111_1", "Bedroom", true, true, 2202, 4000),
                DirigeraLight("eeee-5555_1", "Hall", true, true, 2700, 6500)
            ),
            lights
        )
        assertNull(DirigeraClient.parseLights("not json"))
    }

    @Test
    fun listLightsSendsTheBearerTokenAndReportsARevokedOne() {
        val token = "eyJhbGciOiJFUzI1NiJ9.payload.sig"
        val hub = FakeHub { request ->
            if (request.header("Authorization") == "Bearer $token") {
                200 to """[{"id":"aaaa-1111_1","deviceType":"light","attributes":{"customName":"Desk","isOn":true}}]"""
            } else {
                401 to "{}"
            }
        }

        val loaded = client.listLights("192.168.1.50", token, hub.session())
        val revoked = client.listLights("192.168.1.50", "otherToken", hub.session())
        val malformed = client.listLights("192.168.1.50", "bad token", hub.session())

        assertEquals(
            DirigeraLightsResult.Loaded(
                listOf(DirigeraLight("aaaa-1111_1", "Desk", true, false, null, null)),
                observedFingerprint = null
            ),
            loaded
        )
        assertEquals(DirigeraLightsResult.Unauthorized, revoked)
        assertEquals(DirigeraLightsResult.Unauthorized, malformed)
        assertEquals("a malformed token never goes on the wire", 2, hub.requests.size)
        assertEquals("GET", hub.requests.first().method)
        assertEquals("/v1/devices", hub.requests.first().url.encodedPath)
        assertEquals(8443, hub.requests.first().url.port)
    }

    @Test
    fun switchingOnSendsLevelAndColorFirstThenPowerThenTheLevelAgain() {
        val hub = FakeHub { 202 to "" }

        val ok = client.setLight(
            session = hub.session(),
            rawHost = "192.168.1.50",
            rawToken = "tok.en",
            deviceId = "aaaa-1111_1",
            level = 40,
            kelvin = 2202,
            turnOn = true,
            transitionMillis = 0L
        )

        assertTrue(ok)
        assertEquals(
            listOf(
                """[{"attributes":{"lightLevel":40}}]""",
                """[{"attributes":{"colorTemperature":2202}}]""",
                """[{"attributes":{"isOn":true}}]""",
                """[{"attributes":{"lightLevel":40}}]"""
            ),
            hub.requests.map { it.body }
        )
        hub.requests.forEach { request ->
            assertEquals("PATCH", request.method)
            assertEquals("/v1/devices/aaaa-1111_1", request.url.encodedPath)
            assertEquals("Bearer tok.en", request.authorization)
        }
    }

    @Test
    fun aRampStepFadesOnlyWhatTheLightSupports() {
        val hub = FakeHub { 202 to "" }

        val ok = client.setLight(
            session = hub.session(),
            rawHost = "192.168.1.50",
            rawToken = "tok.en",
            deviceId = "bbbb-2222_1",
            level = 50,
            kelvin = null,
            turnOn = false,
            transitionMillis = 60_000L
        )

        assertTrue(ok)
        assertEquals(
            listOf("""[{"attributes":{"lightLevel":50},"transitionTime":60000}]"""),
            hub.requests.map { it.body }
        )
    }

    @Test
    fun aRejectedCommandStopsTheRestForThatLight() {
        val hub = FakeHub { 500 to "{}" }

        val ok = client.setLight(
            session = hub.session(),
            rawHost = "192.168.1.50",
            rawToken = "tok.en",
            deviceId = "aaaa-1111_1",
            level = 40,
            kelvin = 2202,
            turnOn = true,
            transitionMillis = 0L
        )

        assertFalse(ok)
        assertEquals(1, hub.requests.size)
    }

    @Test
    fun lightIdsAreSanitisedAndDeduplicated() {
        assertEquals(
            listOf("aaaa-1111_1", "bbbb-2222_1"),
            DirigeraClient.parseLightIds(" aaaa-1111_1, bbbb-2222_1,aaaa-1111_1,,bad id,")
        )
    }
}
