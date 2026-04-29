package com.sysadmindoc.alarmclock.data.news

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Minimal RSS 2.0 / Atom parser using Android's built-in XmlPullParser. We
 * don't pull Rome or rss-parser — they add ~600 KB and JAXB pain — because
 * every feed we plan to support (Google News, BBC, NPR, Reuters) is plain
 * RSS 2.0 with a tiny stable schema:
 *
 *   <channel>
 *     <title> ... </title>
 *     <item>
 *       <title>...</title>
 *       <link>...</link>
 *       <description>...</description>
 *       <pubDate>RFC-822</pubDate>
 *       <guid>optional, used for stable id</guid>
 *       <source url="...">Source name</source>  (Google News only)
 *     </item>
 *   </channel>
 *
 * Atom is structurally similar (<entry> instead of <item>, <updated> instead
 * of <pubDate>, ISO-8601 dates) so we accept both — see [parse]'s element
 * dispatch.
 *
 * Defensive choices:
 *  - Unknown tags are skipped via [skip] rather than aborting; broken or
 *    extended feeds (custom namespaces, vendor extensions) won't kill parsing.
 *  - Date parsing tries RFC-822 first (RSS 2.0), then ISO-8601 with offset
 *    (Atom). Failures are silently null — sorting falls back to "as listed".
 *  - Title/link/description are passed through `Html.fromHtml` at render
 *    time, not here, so this layer stays string-clean.
 */
object RssParser {

    private val rfc822Patterns = listOf(
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "dd MMM yyyy HH:mm:ss zzz",
        "dd MMM yyyy HH:mm:ss Z",
    )

    private val iso8601Patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
    )

    @Throws(Exception::class)
    fun parse(input: InputStream, defaultSource: String): List<NewsItem> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        val items = mutableListOf<NewsItem>()
        var channelTitle: String? = null

        // Descend into the document instead of relying on the top-level
        // dispatch — RSS wraps items in <channel> and Atom wraps entries in
        // <feed>, both of which would be `skip()`-ed by an "else" branch and
        // eat every item with them. Tag names are matched case-insensitively.
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name?.lowercase(Locale.ROOT)) {
                // Containers — keep walking, don't skip.
                "rss", "channel", "feed" -> Unit

                // Channel/feed metadata. Only capture the first title we see
                // at the channel level (item titles fire later via readItem).
                "title" -> if (channelTitle == null) {
                    channelTitle = readText(parser)
                }

                "item", "entry" -> readItem(parser)
                    ?.let { items.add(it.withFallbackSource(channelTitle ?: defaultSource)) }

                else -> skip(parser)
            }
        }
        return items
    }

    private fun readItem(parser: XmlPullParser): NewsItem? {
        val itemName = parser.name
        var title = ""
        var link = ""
        var description = ""
        var pubDate: String? = null
        var guid: String? = null
        var source: String? = null

        while (parser.next() != XmlPullParser.END_TAG ||
            parser.name?.equals(itemName, ignoreCase = true) == false
        ) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name?.lowercase(Locale.ROOT)) {
                "title" -> title = readText(parser)
                "link" -> {
                    // RSS: <link>https://...</link>
                    // Atom: <link href="https://..." />
                    val href = parser.getAttributeValue(null, "href")
                    link = if (!href.isNullOrBlank()) {
                        skip(parser)
                        href
                    } else readText(parser)
                }

                "description", "summary", "content" -> description = readText(parser)
                "pubdate", "published", "updated" -> pubDate = readText(parser)
                "guid", "id" -> guid = readText(parser)
                "source" -> {
                    val text = readText(parser)
                    if (text.isNotBlank()) source = text
                }

                else -> skip(parser)
            }
        }

        if (title.isBlank() && link.isBlank()) return null
        val id = (guid ?: link).ifBlank { title }.hashCode().toString()
        return NewsItem(
            id = id,
            title = title.trim(),
            link = link.trim(),
            description = description.trim(),
            source = source?.trim().orEmpty(),
            publishedAtMillis = pubDate?.let(::parseDate),
        )
    }

    private fun NewsItem.withFallbackSource(fallback: String): NewsItem =
        if (source.isBlank()) copy(source = fallback) else this

    private fun parseDate(raw: String): Long? {
        val cleaned = raw.trim().ifBlank { return null }
        val patterns = rfc822Patterns + iso8601Patterns
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(cleaned)?.time
            } catch (_: Throwable) {
                // try next pattern
            }
        }
        return null
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text ?: ""
            parser.nextTag()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) return
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}
