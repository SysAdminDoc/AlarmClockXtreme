package com.sysadmindoc.alarmclock.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary

/**
 * Embedded Windy radar map. Wraps a WebView pointed at Windy's public embed
 * endpoint (https://embed.windy.com/embed2.html). The endpoint serves no
 * X-Frame-Options or CSP frame-ancestors header, so it loads cleanly in a
 * WebView with JavaScript + DOM storage enabled.
 *
 * Design notes (v1.8.0):
 *  - Fixed 360 dp height. Windy renders WebGL into a canvas; inside an
 *    unbounded scrollable parent the canvas measures 0×0 and stays blank.
 *  - Hardware acceleration is left at the default (LAYER_TYPE_HARDWARE).
 *    Forcing software-layer was tried in earlier prototypes and tore the
 *    radar animation across frames.
 *  - WebViewClient is overridden so internal Windy navigation (panning
 *    layers, opening the legend) stays inside the WebView. The "open in
 *    Windy" button below the map is the only escape hatch to a real browser.
 *  - The embed URL uses overlay=radar + product=radar + radarRange=-1 so
 *    the animated rain layer plays on load, not the static "now" snapshot.
 *
 * Reference impls verified against:
 *   github.com/nguyenvanbaoub2005/weather-app — full param set
 *   github.com/ed-capstone-design/android-front — minimal embed
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WindyRadarCard(
    latitude: Double?,
    longitude: Double?,
    locationLabel: String,
    modifier: Modifier = Modifier,
    zoom: Int = 7
) {
    val uriHandler = LocalUriHandler.current
    val embedUrl = remember(latitude, longitude, zoom) {
        buildWindyEmbedUrl(latitude, longitude, zoom)
    }
    val externalUrl = remember(latitude, longitude, zoom) {
        buildWindyExternalUrl(latitude, longitude, zoom)
    }

    AppSurfaceCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Live radar",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Animated precipitation around $locationLabel — powered by Windy.com",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(AndroidColor.parseColor("#0B0F1A"))
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                builtInZoomControls = false
                                displayZoomControls = false
                                mixedContentMode =
                                    WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                cacheMode = WebSettings.LOAD_DEFAULT
                            }
                            webViewClient = WebViewClient()
                            loadUrl(embedUrl)
                        }
                    },
                    update = { webView ->
                        if (webView.url != embedUrl) webView.loadUrl(embedUrl)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.size(2.dp))

            TextButton(
                onClick = { uriHandler.openUri(externalUrl) }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    "Open full map in Windy",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

private fun buildWindyEmbedUrl(
    latitude: Double?,
    longitude: Double?,
    zoom: Int
): String {
    // Fall back to a wide-frame US view if no location is available — better
    // than a broken iframe. Windy is global so any sane lat/lon works.
    val lat = latitude ?: 39.5
    val lon = longitude ?: -98.35
    return "https://embed.windy.com/embed2.html" +
        "?lat=$lat" +
        "&lon=$lon" +
        "&detailLat=$lat" +
        "&detailLon=$lon" +
        "&zoom=$zoom" +
        "&overlay=radar" +
        "&product=radar" +
        "&level=surface" +
        "&menu=" +
        "&message=" +
        "&marker=true" +
        "&type=map" +
        "&location=coordinates" +
        "&metricWind=default" +
        "&metricTemp=default" +
        "&radarRange=-1"
}

private fun buildWindyExternalUrl(
    latitude: Double?,
    longitude: Double?,
    zoom: Int
): String {
    val lat = latitude ?: 39.5
    val lon = longitude ?: -98.35
    return "https://www.windy.com/?radar,$lat,$lon,$zoom"
}
