package com.sysadmindoc.alarmclock.ui.dashboard

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.data.remote.GeocodingResult
import com.sysadmindoc.alarmclock.data.repository.CalendarEvent
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.ui.components.AppEmptyState
import com.sysadmindoc.alarmclock.ui.components.AppInlineNotice
import com.sysadmindoc.alarmclock.ui.components.AppLoadingCard
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.WeatherSkyBackground
import com.sysadmindoc.alarmclock.ui.components.WindyRadarCard
import com.sysadmindoc.alarmclock.ui.components.AppInputShape
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.ui.theme.AccentBlue
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.BlueLight
import com.sysadmindoc.alarmclock.ui.theme.ClockTimeDisplay
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import com.sysadmindoc.alarmclock.R
import androidx.compose.ui.res.stringResource

@Composable
fun DashboardScreen(
    onOpenAlarms: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.loadData()
    }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    // v1.9.0: dynamic time-of-day + weather sky behind the entire Today
    // tab. The sky reads as the background; cards bring their own surfaces
    // so content remains legible against any keyframe.
    WeatherSkyBackground(
        sunrise = state.sunriseLocal,
        sunset = state.sunsetLocal,
        weatherCode = state.currentWeatherCode,
        tornadoActive = state.tornadoAlertActive,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            DashboardHeader(state)

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!state.showWeather && !state.showCalendar) {
                    AppSurfaceCard {
                        AppEmptyState(
                            icon = Icons.Default.Schedule,
                            title = stringResource(R.string.dashboard_today_quiet),
                            description = stringResource(R.string.dashboard_weather_calendar_cards_are_off)
                        )
                    }
                }

                if (state.showWeather) {
                    WeatherSection(
                        state = state,
                        onChangeLocation = viewModel::showLocationPicker,
                        onRetryWeather = viewModel::loadWeather
                    )
                }

                // v1.8.0: Windy radar embed below the static weather card. Hidden
                // until we actually have a coordinate to center on (otherwise the
                // iframe shows the default Windy "world" view, which is jarring
                // when the rest of the screen is local-conditions data).
                if (state.showWeather && state.showRadar && state.hasLocation &&
                    state.latitude != null && state.longitude != null) {
                    WindyRadarCard(
                        latitude = state.latitude,
                        longitude = state.longitude,
                        locationLabel = state.locationName.ifBlank { "your area" }
                    )
                }

                if (state.showCalendar) {
                    CalendarSection(state) {
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    }
                }

                if (state.nextAlarmTime.isNotBlank()) {
                    NextAlarmSection(
                        state = state,
                        onOpenAlarms = onOpenAlarms
                    )
                }
            }
        }
    }

    if (state.showLocationPicker) {
        LocationPickerDialog(
            results = state.locationSearchResults,
            isSearching = state.locationSearching,
            onSearch = viewModel::searchLocation,
            onSelect = viewModel::selectLocation,
            onUseDevice = viewModel::useDeviceLocation,
            onDismiss = viewModel::hideLocationPicker
        )
    }
}

@Composable
private fun NextAlarmSection(
    state: DashboardUiState,
    onOpenAlarms: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppSectionTitle(title = stringResource(R.string.bedtime_jetlag_helper_next_alarm))
        AppSurfaceCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenAlarms),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = state.nextAlarmTime,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = listOf(
                            state.nextAlarmLabel,
                            state.nextAlarmSchedule
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
                Text(
                    text = stringResource(R.string.dashboard_view),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(state: DashboardUiState) {
    // v1.7.5: Hero used to also show "Current Location" + "Nothing booked"
    // chips, but the same info is right below in the weather card and the
    // calendar card respectively. The chips were just doubling-up.
    // Calendar-permission-needed chip is kept because it's an actionable
    // warning, not a duplicate.
    // v1.8.0: Hero retitled "Weather" since the screen is now a weather hub
    // (centered conditions, hourly, 3-day, sunrise/sunset, UV, Windy radar).
    // Calendar still lives below the fold but is no longer the headline.
    // v1.9.0: hero is transparent so the dynamic sky behind the screen
    // shows through. Active tornado warnings surface as a hero chip so the
    // signal is visible immediately, before the user scrolls.
    val hasTornadoChip = state.tornadoAlertActive
    AlarmClockHeroHeader(
        transparent = true,
        title = stringResource(R.string.dashboard_today),
        subtitle = state.todayDate,
        badge = if (hasTornadoChip) {
            {
                AppStatusChip(
                    label = stringResource(R.string.dashboard_tornado_warning),
                    icon = Icons.Default.Warning,
                    color = AccentRed,
                )
            }
        } else null,
    )
}

@Composable
private fun WeatherSection(
    state: DashboardUiState,
    onChangeLocation: () -> Unit,
    onRetryWeather: () -> Unit
) {
    // v1.7.4: dropped the "Weather / Current conditions and a short forecast"
    // section title — the icon + temp + description below already self-narrate
    // and the title was eating ~50dp of vertical space.
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            state.weatherLoading -> {
                AppLoadingCard(label = stringResource(R.string.dashboard_loading_current_weather))
            }

            state.weatherError != null -> {
                // Any fetch failure used to render the "set your location"
                // prompt, so an offline user with a city already configured was
                // told to do something they had already done, and the actual
                // error was never shown.
                val hasLocation = state.hasLocation
                AppSurfaceCard(contentPadding = PaddingValues(16.dp)) {
                    AppSectionTitle(title = stringResource(R.string.dashboard_weather))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (hasLocation) {
                                    state.weatherError
                                } else {
                                    "Set your location"
                                },
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (hasLocation) {
                                    "Check your connection and try again."
                                } else {
                                    "Get local conditions and forecasts."
                                },
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (hasLocation) {
                            TextButton(onClick = onRetryWeather) {
                                Text(stringResource(R.string.dashboard_retry))
                            }
                        } else {
                            TextButton(onClick = onChangeLocation) {
                                Text(stringResource(R.string.alarm_edit_choose))
                            }
                        }
                    }
                }
            }

            else -> {
                if (state.weatherStale && state.weatherLastUpdatedMillis != null) {
                    AppInlineNotice(
                        title = stringResource(R.string.dashboard_showing_saved_forecast),
                        message = buildWeatherStaleMessage(state),
                        icon = Icons.Default.CloudOff,
                        color = SnoozeYellow
                    )
                    OutlinedButton(
                        onClick = onRetryWeather,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.dashboard_retry_weather))
                    }
                }

                // v1.7.4: Centered hero — location chip, big icon, big temp,
                // condition description, all stacked and centered. ZeusWatch's
                // CurrentConditionsHeader inspired the layout.
                AppSurfaceCard {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onChangeLocation,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.dashboard_change_weather_location),
                                tint = TextMuted
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = state.locationName.ifBlank { "Weather" },
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Icon(
                                imageVector = weatherIconFor(state.weatherIcon),
                                contentDescription = state.weatherDescription,
                                tint = weatherColorFor(state.weatherIcon),
                                modifier = Modifier.size(64.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = state.temperature,
                                    style = ClockTimeDisplay,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "\u00B0${state.tempUnit}",
                                    fontSize = 22.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 10.dp, start = 2.dp)
                                )
                            }
                            Text(
                                text = state.weatherDescription,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            if (state.feelsLike.isNotBlank()) {
                                Text(
                                    text = state.feelsLike,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = TextMuted.copy(alpha = 0.18f))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WeatherMetric(
                                label = stringResource(R.string.dashboard_high),
                                value = "${state.highTemp}\u00B0",
                                icon = Icons.Default.ArrowUpward,
                                accent = AccentRed,
                                modifier = Modifier.weight(1f)
                            )
                            WeatherMetric(
                                label = stringResource(R.string.dashboard_low),
                                value = "${state.lowTemp}\u00B0",
                                icon = Icons.Default.ArrowDownward,
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            WeatherMetric(
                                label = stringResource(R.string.dashboard_rain),
                                value = if (state.precipChance.isBlank()) "0%" else state.precipChance,
                                icon = Icons.Default.Umbrella,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WeatherMetric(
                                label = stringResource(R.string.dashboard_humidity),
                                value = state.humidity,
                                icon = Icons.Default.WaterDrop,
                                modifier = Modifier.weight(1f)
                            )
                            WeatherMetric(
                                label = stringResource(R.string.dashboard_wind),
                                value = state.windSpeed,
                                icon = Icons.Default.Air,
                                modifier = Modifier.weight(1f)
                            )
                            WeatherMetric(
                                label = stringResource(R.string.dashboard_uv),
                                value = state.uvIndex.ifBlank { "—" },
                                icon = Icons.Default.WbSunny,
                                accent = SnoozeYellow,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // v1.7.4: Sunrise / sunset row — ported from ZeusWatch's
                    // GoldenHour card but slimmed to a horizontal pair. Most
                    // useful field in an alarm clock context: "is the sun up
                    // by my alarm time?"
                    if (state.sunrise.isNotBlank() || state.sunset.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WeatherMetric(
                                label = stringResource(R.string.alarm_edit_solar_sunrise),
                                value = state.sunrise.ifBlank { "—" },
                                icon = Icons.Default.WbSunny,
                                accent = SnoozeYellow,
                                modifier = Modifier.weight(1f)
                            )
                            WeatherMetric(
                                label = stringResource(R.string.alarm_edit_solar_sunset),
                                value = state.sunset.ifBlank { "—" },
                                icon = Icons.Default.NightsStay,
                                accent = AccentBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                state.airQuality?.let { airQuality ->
                    AirQualityCard(airQuality)
                }

                // v1.7.4: hourly strip — next ~8 hours so users can see what
                // it'll look like when their morning alarm rings. Kept small
                // and horizontal-scrolling here (it's a short strip, not the
                // 24-hour ZeusWatch one). The 3-day below is the bigger
                // change: vertical now.
                if (state.hourly.isNotEmpty()) {
                    AppSurfaceCard(contentPadding = PaddingValues(14.dp)) {
                        Text(stringResource(R.string.dashboard_next_few_hours),
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(state.hourly) { hour -> HourlyCell(hour) }
                        }
                    }
                }

                if (state.forecast.isNotEmpty()) {
                    // v1.7.4: vertical 3-day list, replacing the horizontal
                    // LazyRow. One day per line is easier to scan and stops
                    // truncating long descriptions on narrow phones.
                    AppSurfaceCard(contentPadding = PaddingValues(14.dp)) {
                        Text(stringResource(R.string.dashboard_next_3_days),
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            state.forecast.take(3).forEachIndexed { index, day ->
                                ForecastRow(day)
                                if (index < state.forecast.take(3).lastIndex) {
                                    HorizontalDivider(color = TextMuted.copy(alpha = 0.16f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherMetric(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color = TextMuted,
    modifier: Modifier = Modifier
) {
    com.sysadmindoc.alarmclock.ui.components.AppMetricTile(
        label = label,
        value = value,
        icon = icon,
        accent = accent,
        modifier = modifier
    )
}

private fun buildWeatherStaleMessage(state: DashboardUiState): String {
    val updated = state.weatherLastUpdatedMillis?.let(::formatRelativeAge) ?: "earlier"
    val reason = state.weatherStaleMessage ?: "Showing the last saved forecast."
    return "$reason Last updated $updated."
}

private fun formatRelativeAge(epochMs: Long): String {
    val deltaMs = (System.currentTimeMillis() - epochMs).coerceAtLeast(0)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(deltaMs)
    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> "${TimeUnit.SECONDS.toMinutes(seconds)}m ago"
        seconds < 86_400 -> "${TimeUnit.SECONDS.toHours(seconds)}h ago"
        else -> "${TimeUnit.SECONDS.toDays(seconds)}d ago"
    }
}

@Composable
private fun AirQualityCard(summary: AirQualitySummary) {
    val accent = airQualityColorFor(summary.level)
    AppSurfaceCard(contentPadding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.14f)
            ) {
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Air,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(stringResource(R.string.dashboard_air_quality),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    summary.detail,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    summary.aqi,
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    summary.band,
                    color = accent,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End
                )
            }
        }

        if (summary.pollutantMetrics.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                summary.pollutantMetrics.take(3).forEach { metric ->
                    WeatherMetric(
                        label = metric.label,
                        value = metric.value,
                        icon = Icons.Default.WaterDrop,
                        accent = TextMuted,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        HorizontalDivider(color = TextMuted.copy(alpha = 0.16f))

        Text(stringResource(R.string.dashboard_pollen),
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        summary.pollenRows.forEachIndexed { index, row ->
            PollenRow(row)
            if (index < summary.pollenRows.lastIndex) {
                HorizontalDivider(color = TextMuted.copy(alpha = 0.10f))
            }
        }

        if (!summary.hasPollenData) {
            AppInlineNotice(
                title = stringResource(R.string.dashboard_pollen_unavailable),
                message = stringResource(R.string.dashboard_open_meteo_does_not_report_pollen),
                icon = Icons.Default.WaterDrop,
                color = TextMuted
            )
        }

        Text(stringResource(R.string.dashboard_source_open_meteo_cams),
            color = TextMuted,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun PollenRow(row: PollenMetric) {
    val accent = pollenColorFor(row.level)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(accent, RoundedCornerShape(4.dp))
        )
        Text(
            text = row.label,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = row.value,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            )
            Text(
                text = row.band,
                color = accent,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.End
            )
        }
    }
}

private fun airQualityColorFor(level: AirQualityLevel): Color = when (level) {
    AirQualityLevel.GOOD -> DismissGreen
    AirQualityLevel.MODERATE -> SnoozeYellow
    AirQualityLevel.SENSITIVE -> SnoozeYellow
    AirQualityLevel.UNHEALTHY -> AccentRed
    AirQualityLevel.VERY_UNHEALTHY -> AccentRed
    AirQualityLevel.HAZARDOUS -> AccentRed
    AirQualityLevel.UNKNOWN -> TextMuted
}

private fun pollenColorFor(level: PollenLevel): Color = when (level) {
    PollenLevel.NONE -> TextMuted
    PollenLevel.LOW -> DismissGreen
    PollenLevel.MODERATE -> SnoozeYellow
    PollenLevel.HIGH -> AccentRed
    PollenLevel.VERY_HIGH -> AccentRed
    PollenLevel.UNAVAILABLE -> TextMuted
}

/**
 * v1.7.4: Vertical 3-day forecast row. Replaces the old horizontal
 * ForecastCard so users can scan three days at a glance without swiping.
 * Layout: day name | icon | description | rain chip | H/L
 */
@Composable
private fun ForecastRow(day: ForecastDay) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = day.dayName,
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(86.dp)
        )
        Icon(
            imageVector = weatherIconFor(day.icon),
            contentDescription = day.description,
            tint = weatherColorFor(day.icon),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = day.description,
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (day.precipChance.isNotBlank() && day.precipChance != "0%") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Umbrella,
                    contentDescription = null,
                    tint = BlueLight,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = day.precipChance,
                    color = BlueLight,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Text(
            text = "${day.high}° / ${day.low}°",
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * v1.7.4: Single hourly cell — time, icon, temp, optional rain%.
 * Lifted from ZeusWatch's HourlyForecastStrip but slimmer.
 */
@Composable
private fun HourlyCell(hour: HourlyForecast) {
    Column(
        modifier = Modifier
            .background(
                color = com.sysadmindoc.alarmclock.ui.theme.SurfaceLight,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = hour.timeLabel,
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium
        )
        Icon(
            imageVector = weatherIconFor(hour.icon),
            contentDescription = null,
            tint = weatherColorFor(hour.icon),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = "${hour.temperature}°",
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = hour.precipChance.ifBlank { " " },
            color = if (hour.precipChance.isNotBlank()) BlueLight else TextMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CalendarSection(
    state: DashboardUiState,
    onRequestCalendarPermission: () -> Unit
) {
    // v1.7.5: Title moved INTO the card so the calendar section matches the
    // "Next few hours" / "Next 3 days" weather sub-cards. Previously the
    // section title floated outside the card, looking like a stray label.
    AppSurfaceCard {
        Text(stringResource(R.string.dashboard_schedule),
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        when {
            state.calendarPermissionNeeded -> {
                // The row used to say "allow calendar access" and do nothing
                // when tapped, with no other place in the app to grant it.
                CompactDashboardRow(
                    icon = Icons.Default.CalendarMonth,
                    title = stringResource(R.string.dashboard_calendar_access),
                    description = stringResource(R.string.dashboard_tap_allow_calendar_access_see),
                    accent = SnoozeYellow,
                    onClick = onRequestCalendarPermission
                )
            }

            state.calendarEvents.isEmpty() -> {
                CompactDashboardRow(
                    icon = Icons.Default.EventAvailable,
                    title = stringResource(R.string.dashboard_nothing_scheduled_today),
                    description = stringResource(R.string.dashboard_day_clear),
                    accent = DismissGreen
                )
            }

            else -> {
                state.calendarEvents.forEachIndexed { index, event ->
                    EventRow(event)
                    if (index != state.calendarEvents.lastIndex) {
                        HorizontalDivider(color = TextMuted.copy(alpha = 0.16f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDashboardRow(
    icon: ImageVector,
    title: String,
    description: String,
    accent: Color,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 52.dp)
                .background(
                    color = if (event.calendarColor != 0) Color(event.calendarColor) else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                )
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppStatusChip(
                label = event.timeRange,
                icon = Icons.Default.Schedule,
                color = if (event.calendarColor != 0) Color(event.calendarColor) else MaterialTheme.colorScheme.primary
            )
            Text(
                text = event.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall
            )
            if (event.location.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = event.location,
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun weatherIconFor(icon: String): ImageVector = when (icon) {
    "clear" -> Icons.Default.WbSunny
    "partly_cloudy" -> Icons.Default.WbCloudy
    "cloudy" -> Icons.Default.Cloud
    "fog" -> Icons.Default.Cloud
    "drizzle", "rain", "showers" -> Icons.Default.WaterDrop
    "snow" -> Icons.Default.WaterDrop
    "thunderstorm" -> Icons.Default.Bolt
    else -> Icons.Default.Cloud
}

private fun weatherColorFor(icon: String): Color = when (icon) {
    "clear" -> SnoozeYellow
    "partly_cloudy" -> BlueLight
    "thunderstorm" -> AccentRed
    else -> TextSecondary
}

@Composable
private fun LocationPickerDialog(
    results: List<GeocodingResult>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onSelect: (GeocodingResult) -> Unit,
    onUseDevice: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_close), color = TextSecondary)
            }
        },
        title = {
            Text(
                text = stringResource(R.string.dashboard_choose_weather_location),
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppStatusChip(
                        label = if (results.isEmpty()) stringResource(R.string.dashboard_search_a_city) else stringResource(R.string.dashboard_matches, results.size.coerceAtMost(6)),
                        icon = Icons.Default.LocationOn,
                        color = MaterialTheme.colorScheme.primary
                    )
                    AppStatusChip(
                        label = stringResource(R.string.dashboard_optional),
                        icon = Icons.Default.Cloud,
                        color = TextMuted
                    )
                }

                Text(
                    text = stringResource(R.string.dashboard_use_city_zip_code_device),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = { newQuery ->
                        query = newQuery
                        if (newQuery.length >= 2) {
                            onSearch(newQuery)
                        }
                    },
                    placeholder = { Text(stringResource(R.string.dashboard_city_region_zip_code)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = AppInputShape,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedButton(
                    onClick = onUseDevice,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.dashboard_use_current_device_location), color = MaterialTheme.colorScheme.primary)
                }

                when {
                    isSearching -> {
                        AppLoadingCard(
                            height = 180.dp,
                            label = stringResource(R.string.dashboard_searching_locations)
                        )
                    }

                    results.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(results.take(6)) { result ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelect(result) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = com.sysadmindoc.alarmclock.ui.theme.SurfaceLight,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        com.sysadmindoc.alarmclock.ui.theme.BorderSubtle
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(38.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = result.name ?: stringResource(R.string.dashboard_unknown_location),
                                                color = TextPrimary,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = buildString {
                                                    if (!result.state.isNullOrBlank()) {
                                                        append(result.state)
                                                        append(", ")
                                                    }
                                                    append(result.country ?: "")
                                                },
                                                color = TextSecondary,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Text(
                                            text = stringResource(R.string.dashboard_use),
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }

                    query.isBlank() -> {
                        AppEmptyState(
                            icon = Icons.Default.Search,
                            title = stringResource(R.string.dashboard_search_city),
                            description = stringResource(R.string.dashboard_type_at_least_two_characters)
                        )
                    }

                    query.length >= 2 -> {
                        AppEmptyState(
                            icon = Icons.Default.LocationOn,
                            title = stringResource(R.string.dashboard_no_matching_places),
                            description = stringResource(R.string.dashboard_try_broader_city_name_postal)
                        )
                    }
                }
            }
        },
        containerColor = SurfaceDark.copy(alpha = 0.98f),
        shape = RoundedCornerShape(12.dp)
    )
}
