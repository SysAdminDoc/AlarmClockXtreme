package com.sysadmindoc.alarmclock.domain

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.sysadmindoc.alarmclock.data.remote.RadioStationDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RadioStationFilterTest {

    private fun dto(
        uuid: String = "id-1",
        name: String? = "Station",
        url: String? = "https://ice.example/stream",
        urlResolved: String? = null,
        hls: Int? = 0,
        lastCheckOk: Int? = 1,
        codec: String? = "MP3",
        bitrate: Int? = 128
    ) = RadioStationDto(
        stationUuid = uuid,
        name = name,
        url = url,
        urlResolved = urlResolved,
        country = "Germany",
        tags = "jazz, smooth jazz,,",
        codec = codec,
        bitrate = bitrate,
        hls = hls,
        lastCheckOk = lastCheckOk
    )

    @Test
    fun `the resolved address is preferred, since url is often the playlist it came from`() {
        val station = RadioStationFilter.toPlayable(
            dto(url = "https://radio.example/listen.pls", urlResolved = "https://ice.example/live")
        )
        assertEquals("https://ice.example/live", station?.streamUrl)
        assertEquals(listOf("jazz", "smooth jazz"), station?.tags)
    }

    @Test
    fun `an http resolved address falls back to an https url`() {
        val station = RadioStationFilter.toPlayable(
            dto(url = "https://ice.example/secure", urlResolved = "http://ice.example/plain")
        )
        assertEquals("https://ice.example/secure", station?.streamUrl)
    }

    @Test
    fun `stations that can't ring are dropped`() {
        assertNull("http only", RadioStationFilter.toPlayable(dto(url = "http://ice.example/plain")))
        assertNull("failed its last check", RadioStationFilter.toPlayable(dto(lastCheckOk = 0)))
        assertNull("no name", RadioStationFilter.toPlayable(dto(name = "  ")))
        assertNull(
            "HLS with no .m3u8 for Media3 to recognise",
            RadioStationFilter.toPlayable(dto(url = "https://hls.example/live", hls = 1))
        )
        assertNotNull(
            "HLS that says so",
            RadioStationFilter.toPlayable(dto(url = "https://hls.example/live/index.m3u8?token=1", hls = 1))
        )
    }

    @Test
    fun `an unknown codec is left blank instead of shown`() {
        assertEquals("", RadioStationFilter.toPlayable(dto(codec = "UNKNOWN"))?.codec)
        assertEquals(0, RadioStationFilter.toPlayable(dto(bitrate = -5))?.bitrate)
    }

    @Test
    fun `name matches come first and repeats of one stream are dropped`() {
        val byName = listOf(
            dto(uuid = "a", name = "Jazz FM", url = "https://jazz.example/live"),
            dto(uuid = "b", name = "Jazz FM HQ", url = "https://JAZZ.example/live")
        )
        val byTag = listOf(
            dto(uuid = "a", name = "Jazz FM", url = "https://jazz.example/live"),
            dto(uuid = "c", name = "Smooth", url = "https://smooth.example/live")
        )
        assertEquals(listOf("a", "c"), RadioStationFilter.merge(byName, byTag).map { it.id })
    }

    @Test
    fun `merge stops at the result cap`() {
        val many = (1..80).map { dto(uuid = "id-$it", url = "https://ice.example/$it") }
        assertEquals(RadioStationFilter.MAX_RESULTS, RadioStationFilter.merge(many, emptyList()).size)
    }

    @Test
    fun `a real directory response parses, nulls included`() {
        // Trimmed from a de1.api.radio-browser.info/json/stations/search reply.
        val json = """
            [
              {"changeuuid":"x","stationuuid":"96062a7b-0601-11e8-ae97-52543be04c81",
               "name":"BBC Radio 1","url":"http://stream.live.vc.bbcmedia.co.uk/bbc_radio_one",
               "url_resolved":"https://as-hls-ww-live.akamaized.net/pool_01505109/live/ww/bbc_radio_one/bbc_radio_one.isml/bbc_radio_one-audio%3d96000.norewind.m3u8",
               "homepage":"https://www.bbc.co.uk/radio1","favicon":"","tags":"bbc,pop",
               "country":"The United Kingdom Of Great Britain And Northern Ireland","countrycode":"GB",
               "state":null,"language":"english","votes":1234,"codec":"AAC","bitrate":96,
               "hls":1,"lastcheckok":1,"clickcount":3000,"geo_lat":null,"geo_long":null},
              {"stationuuid":"no-name","name":null,"url":null,"url_resolved":null,"codec":null,
               "bitrate":null,"hls":null,"lastcheckok":null}
            ]
        """.trimIndent()
        val type = Types.newParameterizedType(List::class.java, RadioStationDto::class.java)
        val parsed = Moshi.Builder().build().adapter<List<RadioStationDto>>(type).fromJson(json)!!
        assertEquals(2, parsed.size)

        val stations = RadioStationFilter.merge(parsed, emptyList())
        assertEquals(1, stations.size)
        val bbc = stations.single()
        assertEquals("BBC Radio 1", bbc.name)
        assertEquals("AAC", bbc.codec)
        assertEquals(96, bbc.bitrate)
        assertEquals(true, bbc.streamUrl.endsWith(".m3u8"))
    }
}
