package com.sysadmindoc.alarmclock.ui.dashboard

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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Umbrella
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.data.remote.GeocodingResult
import com.sysadmindoc.alarmclock.data.repository.CalendarEvent
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.ui.components.AppEmptyState
import com.sysadmindoc.alarmclock.ui.components.AppLoadingCard
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
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

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .verticalScroll(rememberScrollState())
    ) {
        DashboardHeader(state)

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!state.showWeather && !state.showCalendar) {
                AppSurfaceCard {
                    AppEmptyState(
                        icon = Icons.Default.Schedule,
                        title = "Your dashboard is intentionally quiet",
                        description = "Weather and calendar cards are turned off. Re-enable them anytime from Settings."
                    )
                }
            }

            if (state.showWeather) {
                WeatherSection(
                    state = state,
                    onChangeLocation = viewModel::showLocationPicker
                )
            }

            if (state.showCalendar) {
                CalendarSection(state)
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
private fun DashboardHeader(state: DashboardUiState) {
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 0..4 -> "Rest well."
            in 5..11 -> "Good morning."
            in 12..17 -> "Good afternoon."
            else -> "Good evening."
        }
    }

    AlarmClockHeroHeader(
        title = "My Day",
        subtitle = "$greeting ${state.todayDate}",
        overline = "Daily overview",
        badge = {
            if (state.showWeather && state.locationName.isNotBlank()) {
                AppStatusChip(
                    label = state.locationName,
                    icon = Icons.Default.LocationOn
                )
            }
            if (state.showCalendar) {
                AppStatusChip(
                    label = when {
                        state.calendarPermissionNeeded -> "Calendar needs permission"
                        state.calendarEvents.isEmpty() -> "Nothing booked"
                        else -> "${state.calendarEvents.size} events today"
                    },
                    icon = Icons.Default.CalendarMonth,
                    color = if (state.calendarPermissionNeeded) SnoozeYellow else DismissGreen
                )
            }
        }
    )
}

@Composable
private fun WeatherSection(
    state: DashboardUiState,
    onChangeLocation: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppSectionTitle(
            title = "Weather",
            description = "Current conditions and a short forecast for the rest of your day."
        )

        when {
            state.weatherLoading -> {
                AppLoadingCard()
            }

            state.weatherError != null -> {
                AppSurfaceCard {
                    AppEmptyState(
                        icon = Icons.Default.CloudOff,
                        title = "Weather isn’t ready yet",
                        description = state.weatherError,
                        footer = {
                            OutlinedButton(
                                onClick = onChangeLocation,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Choose location")
                            }
                        }
                    )
                }
            }

            else -> {
                AppSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppStatusChip(
                                label = state.locationName.ifBlank { "Weather" },
                                icon = Icons.Default.LocationOn
                            )
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = weatherIconFor(state.weatherIcon),
                                    contentDescription = state.weatherDescription,
                                    tint = weatherColorFor(state.weatherIcon),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.size(14.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text(
                                            text = state.temperature,
                                            style = ClockTimeDisplay,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "\u00B0${state.tempUnit}",
                                            fontSize = 18.sp,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                    Text(
                                        text = state.weatherDescription,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        IconButton(onClick = onChangeLocation) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Change weather location",
                                tint = TextMuted
                            )
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
                                label = "High",
                                value = "${state.highTemp}\u00B0",
                                icon = Icons.Default.ArrowUpward,
                                accent = AccentRed,
                                modifier = Modifier.weight(1f)
                            )
                            WeatherMetric(
                                label = "Low",
                                value = "${state.lowTemp}\u00B0",
                                icon = Icons.Default.ArrowDownward,
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            WeatherMetric(
                                label = "Feels like",
                                value = state.feelsLike.removePrefix("Feels like "),
                                icon = Icons.Default.Thermostat,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WeatherMetric(
                                label = "Humidity",
                                value = state.humidity,
                                icon = Icons.Default.WaterDrop,
                                modifier = Modifier.weight(1f)
                            )
                            WeatherMetric(
                                label = "Wind",
                                value = state.windSpeed,
                                icon = Icons.Default.Air,
                                modifier = Modifier.weight(1f)
                            )
                            WeatherMetric(
                                label = "Rain",
                                value = if (state.precipChance.isBlank()) "0%" else state.precipChance,
                                icon = Icons.Default.Umbrella,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (state.forecast.isNotEmpty()) {
                    AppSurfaceCard(contentPadding = PaddingValues(16.dp)) {
                        AppSectionTitle(
                            title = "Next 3 days",
                            description = "A quick glance at what is coming up."
                        )

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(state.forecast.take(3)) { day ->
                                ForecastCard(
                                    day = day,
                                    modifier = Modifier.width(168.dp)
                                )
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
    Column(
        modifier = modifier
            .background(
                color = SurfaceCard.copy(alpha = 0.7f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
            Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = value.ifBlank { "--" },
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ForecastCard(
    day: ForecastDay,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard.copy(alpha = 0.82f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = day.dayName,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = day.date,
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (day.precipChance.isNotBlank()) {
                    AppStatusChip(
                        label = "${day.precipChance} rain",
                        icon = Icons.Default.Umbrella,
                        color = BlueLight
                    )
                }
            }

            Text(
                text = "${day.high}\u00B0 / ${day.low}\u00B0",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = day.description,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CalendarSection(state: DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppSectionTitle(
            title = "Today’s schedule",
            description = "See upcoming commitments before the day gets moving."
        )

        AppSurfaceCard {
            when {
                state.calendarPermissionNeeded -> {
                    AppEmptyState(
                        icon = Icons.Default.CalendarMonth,
                        title = "Calendar access helps this page feel alive",
                        description = "Grant calendar permission to show today’s events and surface your first meeting automatically.",
                        accent = SnoozeYellow
                    )
                }

                state.calendarEvents.isEmpty() -> {
                    AppEmptyState(
                        icon = Icons.Default.EventAvailable,
                        title = "Nothing booked today",
                        description = "Enjoy the breathing room. Your events will appear here whenever your schedule fills up.",
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
                    shape = RoundedCornerShape(999.dp)
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
                Text("Close", color = TextSecondary)
            }
        },
        title = {
            Text(
                text = "Choose weather location",
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
                        label = if (results.isEmpty()) "Search a city" else "${results.size.coerceAtMost(6)} matches",
                        icon = Icons.Default.LocationOn,
                        color = MaterialTheme.colorScheme.primary
                    )
                    AppStatusChip(
                        label = "Optional",
                        icon = Icons.Default.Cloud,
                        color = TextMuted
                    )
                }

                Text(
                    text = "Use a city, ZIP code, or device location so the dashboard can stay accurate without extra setup later.",
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
                    placeholder = { Text("City, region, or ZIP code") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                    colors = appOutlinedTextFieldColors(),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedButton(
                    onClick = onUseDevice,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Use current device location", color = MaterialTheme.colorScheme.primary)
                }

                when {
                    isSearching -> {
                        AppLoadingCard(height = 180.dp)
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
                                    shape = RoundedCornerShape(18.dp),
                                    color = SurfaceCard.copy(alpha = 0.82f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
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
                                                text = result.name ?: "Unknown location",
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
                                            text = "Use",
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
                            title = "Search for a city",
                            description = "Type at least two characters to find a location, or use your current device location."
                        )
                    }

                    query.length >= 2 -> {
                        AppEmptyState(
                            icon = Icons.Default.LocationOn,
                            title = "No matching places",
                            description = "Try a broader city name, postal code, or nearby region."
                        )
                    }
                }
            }
        },
        containerColor = SurfaceDark.copy(alpha = 0.98f),
        shape = RoundedCornerShape(22.dp)
    )
}
