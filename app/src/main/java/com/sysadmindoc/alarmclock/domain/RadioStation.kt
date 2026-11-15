package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.remote.RadioStationDto

data class RadioStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val country: String,
    val codec: String,
    val bitrate: Int,
    val tags: List<String>
)

object RadioStationFilter {

    const val MAX_RESULTS = 50

    /**
     * A station the alarm can actually play: an https stream (the resolved
     * address first, since `url` is often the playlist it came from), a name
     * to show, and a last directory check that didn't fail.
     */
    fun toPlayable(dto: RadioStationDto): RadioStation? {
        if (dto.lastCheckOk == 0) return null
        val name = dto.name?.trim().orEmpty()
        if (name.isEmpty()) return null
        val stream = listOfNotNull(dto.urlResolved, dto.url)
            .map { it.trim() }
            .firstOrNull { it.startsWith("https://", ignoreCase = true) }
            ?: return null
        // Media3 picks its HLS player from the .m3u8 extension, and the alarm
        // stores nothing but the address, so an HLS stream without one can
        // never ring. Leave it out rather than offer a station that fails.
        if (dto.hls == 1 && !isHlsAddress(stream)) return null
        return RadioStation(
            id = dto.stationUuid?.trim().orEmpty(),
            name = name,
            streamUrl = stream,
            country = dto.country?.trim().orEmpty(),
            codec = dto.codec?.trim()?.takeUnless { it.equals("UNKNOWN", ignoreCase = true) }.orEmpty(),
            bitrate = (dto.bitrate ?: 0).coerceAtLeast(0),
            tags = dto.tags.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        )
    }

    fun isHlsAddress(url: String): Boolean =
        url.trim().substringBefore('#').substringBefore('?').lowercase().endsWith(".m3u8")

    /**
     * Name matches first, then genre-tag matches, each in the directory's
     * popularity order. One station is often listed several times under the
     * same stream, so repeats of a stream address are dropped.
     */
    fun merge(byName: List<RadioStationDto>, byTag: List<RadioStationDto>): List<RadioStation> {
        val seenStreams = HashSet<String>()
        val seenIds = HashSet<String>()
        return (byName + byTag).asSequence()
            .mapNotNull(::toPlayable)
            .filter { station ->
                val freshId = station.id.isEmpty() || seenIds.add(station.id)
                freshId && seenStreams.add(station.streamUrl.lowercase())
            }
            .take(MAX_RESULTS)
            .toList()
    }
}
