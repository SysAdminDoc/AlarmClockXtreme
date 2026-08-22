package com.sysadmindoc.alarmclock.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceMedium
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import androidx.compose.ui.res.stringResource
import com.sysadmindoc.alarmclock.R

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

    // v1.8.1: track WebView load so we can mask the initial 360 dp dark slab
    // with a skeleton until the first frame paints. Without this the card
    // reads as broken for ~1-3s on cold connections.
    var loaded by remember(embedUrl) { mutableStateOf(false) }
    val webViewAlpha by animateFloatAsState(
        targetValue = if (loaded) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = stringResource(R.string.radar_radar_fade),
    )
    val skeletonAlpha by animateFloatAsState(
        targetValue = if (loaded) 0f else 1f,
        animationSpec = tween(durationMillis = 240),
        label = stringResource(R.string.radar_radar_skeleton_fade),
    )

    AppSurfaceCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppIconSize.md)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_connection_radar),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.radar_animated_precipitation, locationLabel),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                AppStatusChip(
                    label = stringResource(R.string.radar_open_windy),
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    modifier = Modifier.clickable(
                        role = Role.Button,
                        onClick = { uriHandler.openUri(externalUrl) },
                    ),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceMedium)
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(webViewAlpha),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(AndroidColor.parseColor("#0F1721"))
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                builtInZoomControls = false
                                displayZoomControls = false
                                // v1.9.1: harden mixed-content policy. The Windy
                                // embed is HTTPS-only; ALWAYS_ALLOW would let a
                                // hostile redirect downgrade tiles to HTTP. The
                                // COMPATIBILITY mode still permits images/fonts
                                // over HTTP if Windy ever needed them, while
                                // blocking active mixed content (scripts).
                                mixedContentMode =
                                    WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                // Defence-in-depth: explicitly disable the file
                                // and JS-bridge surfaces — we only ever load a
                                // remote HTTPS URL, no app-injected JS, no local
                                // file:// content.
                                allowFileAccess = false
                                allowContentAccess = false
                                cacheMode = WebSettings.LOAD_DEFAULT
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?, url: String?, favicon: Bitmap?
                                ) {
                                    super.onPageStarted(view, url, favicon)
                                    loaded = false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    // Tiny grace period — Windy paints the
                                    // base map before the radar tiles. The
                                    // fade animation absorbs this.
                                    loaded = true
                                }
                            }
                            loadUrl(embedUrl)
                        }
                    },
                    update = { webView ->
                        if (webView.url != embedUrl) {
                            loaded = false
                            webView.loadUrl(embedUrl)
                        }
                    },
                    // v1.9.1: AndroidView leaks the underlying WebView when the
                    // composition exits unless we explicitly tear it down. Without
                    // this, navigating away from Today and back again accumulates
                    // WebView instances, each holding a JS engine + GL context
                    // (~5–15 MB on real devices).
                    onRelease = { webView ->
                        runCatching {
                            webView.stopLoading()
                            webView.loadUrl("about:blank")
                            webView.removeAllViews()
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            webView.destroy()
                        }
                    },
                )

                if (skeletonAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(skeletonAlpha),
                    ) {
                        RadarSkeleton()
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppSkeletonBlock(modifier = Modifier.fillMaxWidth(0.34f))
        AppSkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            cornerRadius = 12.dp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppSkeletonBlock(modifier = Modifier.weight(1f))
            AppSkeletonBlock(modifier = Modifier.weight(1f))
            AppSkeletonBlock(modifier = Modifier.weight(0.6f))
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
